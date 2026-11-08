package com.example.triduofit.family

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.triduofit.model.WorkoutSession
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

data class SyncRejection(val id: String, val message: String)
data class FamilyUiState(val snapshot: FamilySnapshot? = null, val busy: Boolean = false, val message: String? = null, val needsActivation: Boolean = false, val storageFailure: Boolean = false, val rejected: List<SyncRejection> = emptyList())
class FamilyApiException(val status: Int, message: String) : Exception(message)

class FamilyRepository private constructor(private val context: Context) {
    val credentials = DeviceCredentials(context)
    private val db = FamilyDatabase.get(context)
    private val dao = db.dao()
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(45, TimeUnit.SECONDS).build()
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mutex = Mutex()
    private val syncMutex = Mutex()
    private val _state = MutableStateFlow(FamilyUiState())
    val state = _state.asStateFlow()
    val pendingCount = dao.pendingCount().catch { _state.value = _state.value.copy(message = "Le stockage local est indisponible. Les données existantes sont conservées.", storageFailure = true); emit(0) }
    @Volatile var active: ActiveWorkoutEntity? = null; private set
    private val mediaDirectory = File(context.filesDir, "familyfit_media").apply { mkdirs() }

    suspend fun load() = withContext(Dispatchers.IO) {
        try {
            credentials.memberId?.let { id -> dao.snapshot(id)?.let { _state.value = _state.value.copy(snapshot = runCatching { json.decodeFromString<FamilySnapshot>(it.payload) }.getOrNull()) } }
            active = dao.active()
        } catch (e: Exception) { if (e is CancellationException) throw e; _state.value = _state.value.copy(message = "Le stockage local est indisponible. Les données existantes sont conservées ; vérifie l’espace disponible puis relance l’application.") }
    }
    private fun request(path: String, payload: String? = null, authorized: Boolean = true): String {
        val builder = Request.Builder().url(credentials.server + path)
        if (authorized) builder.header("Authorization", "Bearer ${credentials.token ?: throw FamilyApiException(401, "Active ton téléphone avec une invitation.")}")
        if (payload != null) builder.post(payload.toRequestBody("application/json".toMediaType()))
        client.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = runCatching { json.parseToJsonElement(text).jsonObject["detail"]?.jsonPrimitive?.content }.getOrNull()
                throw FamilyApiException(response.code, message ?: if (response.code == 401) "Ce téléphone doit être réactivé avec une nouvelle invitation." else "Le site est momentanément indisponible.")
            }
            return text
        }
    }
    suspend fun preview(code: String): InvitationPreview = withContext(Dispatchers.IO) { json.decodeFromString(request("/api/v1/auth/invitations/preview", json.encodeToString(mapOf("code" to code)), false)) }
    suspend fun enroll(code: String) = withContext(Dispatchers.IO) {
        val invitation = preview(code)
        val owner = active?.memberId
        require(owner == null || owner == "local" || owner.equals(invitation.memberId, ignoreCase = true)) { "Une séance d’un autre membre est encore ouverte. Termine-la avant de changer de profil." }
        val grant = json.decodeFromString<DeviceGrant>(request("/api/v1/auth/enroll", json.encodeToString(mapOf("code" to code, "deviceName" to android.os.Build.MODEL.take(80))), false))
        credentials.save(grant)
        _state.value = FamilyUiState(message = "Bienvenue ${grant.member.nickname} !")
        load(); sync()
    }
    suspend fun approve(code: String) = withContext(Dispatchers.IO) { request("/api/v1/auth/browser/approve", json.encodeToString(mapOf("code" to code))); _state.value = _state.value.copy(message = "Connexion au site confirmée.") }

    suspend fun sync(retryRejected: Boolean = false) = withContext(Dispatchers.IO) {
        if (credentials.token == null) return@withContext
        syncMutex.withLock {
            _state.value = _state.value.copy(busy = true, message = "Synchronisation…")
            try {
                val memberId = credentials.memberId!!
                for (item in dao.pending(memberId)) {
                    if (!retryRejected && item.lastError?.startsWith("Rejeté : ") == true) continue
                    try { request("/api/v1/workouts", item.payload); dao.acknowledge(item.id, memberId) }
                    catch (error: FamilyApiException) {
                        val permanent = error.status in listOf(400, 403, 404, 409, 422)
                        dao.fail(item.id, (if (permanent) "Rejeté : " else "") + (error.message ?: "Envoi différé"))
                        if (!permanent) throw error
                    }
                }
                val rejected = dao.pending(memberId).filter { it.lastError?.startsWith("Rejeté : ") == true }.map { SyncRejection(it.id, it.lastError!!) }
                val payload = request("/api/v1/sync")
                val snapshot = json.decodeFromString<FamilySnapshot>(payload)
                require(snapshot.member.id.equals(memberId, ignoreCase = true)) { "Le profil reçu ne correspond pas au téléphone." }
                dao.saveSnapshot(SnapshotEntity(memberId, payload, System.currentTimeMillis()))
                _state.value = _state.value.copy(snapshot = snapshot, busy = true, message = "Téléchargement des exercices…", rejected = rejected, needsActivation = false)
                var mediaFailures = 0
                snapshot.days.flatMap { it.exercises }.flatMap { listOfNotNull(it.imageUrl, it.videoUrl) }.distinct().forEach { url -> if (!downloadMedia(url)) mediaFailures++ }
                _state.value = _state.value.copy(snapshot = snapshot, busy = false, message = if (mediaFailures == 0) "Planning synchronisé." else "Séances synchronisées. Certains médias restent à télécharger.", rejected = rejected)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _state.value = _state.value.copy(busy = false, needsActivation = error is FamilyApiException && error.status == 401, message = error.message ?: "Sans réseau : les séances et résultats sont conservés sur ce téléphone.")
            }
        }
    }
    fun mediaFile(url: String?): File? = url?.let { if (!it.startsWith("/media/") || it.substringAfterLast('/').contains("..")) null else File(mediaDirectory, it.substringAfterLast('/')).takeIf(File::exists) }
    private fun downloadMedia(url: String): Boolean {
        if (!url.startsWith("/media/") || url.substringAfterLast('/').contains("..")) return false
        if (mediaFile(url) != null) return true
        val target = File(mediaDirectory, url.substringAfterLast('/')); val temp = File(mediaDirectory, "${target.name}.part")
        return runCatching {
            client.newCall(Request.Builder().url(credentials.server + url).header("Authorization", "Bearer ${credentials.token}").build()).execute().use { response ->
                check(response.isSuccessful); response.body!!.byteStream().use { input -> temp.outputStream().use { output -> input.copyTo(output) } }; check(temp.renameTo(target))
            }; true
        }.getOrElse { temp.delete(); false }
    }
    fun parseLink(value: String): FamilyLink {
        val uri = Uri.parse(value)
        require(uri.scheme == "familyfit" && uri.host in listOf("enroll", "login", "workout")) { "Ce QR code n’est pas un code FamilyFit." }
        val server = uri.getQueryParameter("server")?.trimEnd('/')
        require(server == null || server == credentials.server) { "Ce QR code appartient à un autre serveur FamilyFit." }
        return FamilyLink(uri.host!!, uri.getQueryParameter("code"), uri.getQueryParameter("day"))
    }
    suspend fun begin(day: FamilyDay): WorkoutSession = withContext(Dispatchers.IO) {
        mutex.withLock {
        require(_state.value.snapshot?.member?.id == credentials.memberId && _state.value.snapshot?.days?.any { it.id == day.id } == true) { "Synchronise ton profil avant de démarrer cette séance." }
        val current = dao.active()
        if (current != null) {
            require(current.memberId == credentials.memberId && current.dayJson?.let { json.decodeFromString<FamilyDay>(it).id } == day.id) { "Une séance est déjà ouverte. Reprends-la ou termine-la d’abord." }
            active = current; return@withLock json.decodeFromString<WorkoutSession>(current.sessionJson)
        }
        val session = day.nativeSession()
        val entity = ActiveWorkoutEntity(id = UUID.randomUUID().toString(), memberId = credentials.memberId!!, sessionJson = json.encodeToString(session), dayJson = json.encodeToString(day), startedAt = Instant.now().toString(), checkpointJson = json.encodeToString(WorkoutCheckpoint(secondsRemaining = session.exercises.firstOrNull()?.targetCount ?: 0)))
        dao.saveActive(entity); active = entity; session
        }
    }
    fun activeSession(): WorkoutSession? = active?.let { runCatching { json.decodeFromString<WorkoutSession>(it.sessionJson) }.getOrNull() }
    fun checkpoint(): WorkoutCheckpoint? = active?.let { runCatching { json.decodeFromString<WorkoutCheckpoint>(it.checkpointJson) }.getOrNull() }
    suspend fun saveCheckpoint(checkpoint: WorkoutCheckpoint) = withContext(Dispatchers.IO) {
        try { mutex.withLock { active?.let { updated -> val next = updated.copy(checkpointJson = json.encodeToString(checkpoint)); dao.saveActive(next); active = next; _state.value = _state.value.copy(storageFailure = false) } } }
        catch (error: Exception) { if (error is CancellationException) throw error; _state.value = _state.value.copy(message = "L’enregistrement local a échoué. Libère de l’espace puis réessaie.", storageFailure = true) }
    }
    suspend fun finish(checkpoint: WorkoutCheckpoint): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val run = active ?: return@withLock true
            val day = run.dayJson?.let { json.decodeFromString<FamilyDay>(it) } ?: return@withLock true
            val session = json.decodeFromString<WorkoutSession>(run.sessionJson)
            val performances = day.exercises.map { e ->
                val done = checkpoint.completedIndices.count { index -> session.exercises.getOrNull(index)?.id == e.id }
                ExercisePerformance(e.id, done, repetitions = if (e.targetType == "repetitions") e.target else null, durationSeconds = if (e.targetType == "time") e.target * done else null, skipped = checkpoint.skippedIndices.any { index -> session.exercises.getOrNull(index)?.id == e.id })
            }
            val submission = WorkoutSubmission(run.id, day.id, run.startedAt, Instant.now().toString(), performances)
            db.withTransaction { dao.queue(OutboxEntity(run.id, run.memberId, json.encodeToString(submission), System.currentTimeMillis())); dao.deleteActive(run.id) }
            active = null
            performances.all { p -> !p.skipped && p.completedSets == day.exercises.first { it.id == p.exerciseId }.sets }
        }
    }
    companion object {
        @Volatile private var instance: FamilyRepository? = null
        fun get(context: Context) = instance ?: synchronized(this) { instance ?: FamilyRepository(context.applicationContext).also { instance = it } }
    }
}
