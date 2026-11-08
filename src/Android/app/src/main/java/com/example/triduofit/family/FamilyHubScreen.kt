package com.example.triduofit.family

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.triduofit.model.WorkoutSession
import com.example.triduofit.ui.components.MascotMood
import com.example.triduofit.ui.components.MascotView
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun FamilyHubScreen(deepLink: String?, onStart: (WorkoutSession) -> Unit, onLocalCoach: () -> Unit, onLinkHandled: () -> Unit = {}) {
    val context = LocalContext.current
    val repository = remember { FamilyRepository.get(context) }
    val state by repository.state.collectAsState()
    val pending by repository.pendingCount.collectAsState(initial = 0)
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<InvitationPreview?>(null) }
    var browserCode by remember { mutableStateOf<String?>(null) }
    var starting by remember { mutableStateOf(false) }
    var activeAvailable by remember { mutableStateOf(false) }
    val paired = repository.credentials.token != null && !state.needsActivation

    fun start(day: FamilyDay) { scope.launch { starting = true; try { onStart(repository.begin(day)) } catch (e: Exception) { error = e.message } finally { starting = false } } }
    fun inspect(value: String) {
        try {
            val link = repository.parseLink(value)
            when (link.action) {
                "enroll" -> { code = link.code.orEmpty(); preview = null }
                "login" -> { require(paired) { "Active d’abord ton téléphone avec ton invitation personnelle." }; browserCode = link.code }
                "workout" -> { val day = state.snapshot?.days?.firstOrNull { it.id.equals(link.day, true) }; require(day != null) { "Synchronise le mobile pour retrouver cette séance de ton programme." }; start(day) }
            }
        } catch (e: Exception) { error = e.message ?: "QR code non reconnu." }
    }
    val scanner = rememberLauncherForActivityResult(ScanContract()) { result -> result.contents?.let(::inspect) }
    LaunchedEffect(Unit) { repository.load(); activeAvailable = repository.active != null; repository.sync() }
    LaunchedEffect(deepLink) { deepLink?.let { inspect(it); onLinkHandled() } }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) scope.launch { repository.sync(); activeAvailable = repository.active != null } }
        lifecycle.addObserver(observer)
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() { override fun onAvailable(network: Network) { if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) scope.launch { repository.sync() } } }
        manager.registerDefaultNetworkCallback(callback)
        onDispose { lifecycle.removeObserver(observer); manager.unregisterNetworkCallback(callback) }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text("FamilyFit", style = MaterialTheme.typography.headlineLarge); Text(state.snapshot?.member?.let { "Salut ${it.nickname} !" } ?: "Bienvenue dans la famille !") }
            MascotView(size = 80.dp, mood = MascotMood.CHEERING)
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (state.rejected.isNotEmpty()) Card {
            Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Résultats à vérifier", style = MaterialTheme.typography.titleMedium)
                Text("Ces résultats restent conservés sur ce téléphone. Les autres séances continuent de se synchroniser.")
                state.rejected.forEach { Text("${it.id.take(8)} · ${it.message}", style = MaterialTheme.typography.bodySmall) }
                OutlinedButton(onClick = { scope.launch { repository.sync(retryRejected = true) } }, enabled = !state.busy) { Text("Réessayer ces envois") }
            }
        }
        if (!paired) {
            Text(if (state.needsActivation) "Réactiver mon téléphone" else "Mon invitation personnelle", style = MaterialTheme.typography.titleLarge)
            Text("Scanne le QR code donné par l’administrateur, ou saisis le code. Aucun mot de passe à retenir.")
            OutlinedTextField(code, { code = it.uppercase().replace(" ", "").replace("-", "").take(10); preview = null }, label = { Text("Code d’invitation") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(onClick = { scanner.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE).setPrompt("Scanner un QR code FamilyFit").setBeepEnabled(false).setOrientationLocked(false)) }, modifier = Modifier.fillMaxWidth()) { Text("Scanner mon invitation") }
            OutlinedButton(onClick = { scope.launch { try { preview = repository.preview(code); error = null } catch (e: Exception) { error = e.message } } }, enabled = code.length == 10 && !starting, modifier = Modifier.fillMaxWidth()) { Text("Vérifier l’invitation") }
            preview?.let { invitation ->
                Text("Ce téléphone sera associé à ${invitation.nickname}.", style = MaterialTheme.typography.titleMedium)
                Button(onClick = { scope.launch { starting = true; try { repository.enroll(code); preview = null; error = null } catch (e: Exception) { error = e.message } finally { starting = false } } }, enabled = !starting, modifier = Modifier.fillMaxWidth()) { Text("Confirmer et activer mon mobile") }
            }
        } else {
            if (state.snapshot == null) OutlinedButton(onClick = { scope.launch { repository.sync() } }, enabled = !state.busy) { Text("Réessayer la synchronisation") }
            state.snapshot?.let { snapshot ->
                val today = LocalDate.now(ZoneId.of("Europe/Paris"))
                val finished = snapshot.results.count { it.status == "completed" }
                Text("$finished séance(s) terminée(s) · $pending en attente d’envoi", style = MaterialTheme.typography.titleMedium)
                if (activeAvailable) Card {
                    Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) { Text("Ta séance est conservée", style = MaterialTheme.typography.titleMedium); Text("Tu peux reprendre là où tu t’es arrêté."); Button(onClick = { repository.activeSession()?.let(onStart) }) { Text("Reprendre la séance") } }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { scope.launch { repository.sync() } }, enabled = !state.busy, modifier = Modifier.weight(1f)) { Text("Synchroniser") }
                    Button(onClick = { scanner.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE).setPrompt("Scanner le QR code du site").setBeepEnabled(false)) }, modifier = Modifier.weight(1f)) { Text("Ouvrir le site") }
                }
                OutlinedTextField(code, { code = it.uppercase().replace(" ", "").replace("-", "").take(10) }, label = { Text("Code affiché sur le site") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { browserCode = code }, enabled = code.length == 10) { Text("Confirmer une connexion au site") }
                Text("Mon planning", style = MaterialTheme.typography.titleLarge)
                val days = snapshot.days.filter { !LocalDate.parse(it.date).isBefore(today.minusDays(7)) }.sortedBy { it.date }
                if (days.isEmpty()) Text("Ton programme apparaîtra ici après sa validation et la prochaine synchronisation.")
                days.forEach { day ->
                    val result = snapshot.results.filter { it.programDayId == day.id }.let { items -> items.firstOrNull { it.status == "completed" } ?: items.firstOrNull { it.status == "partial" } ?: items.firstOrNull() }
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(LocalDate.parse(day.date).format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)), style = MaterialTheme.typography.labelLarge)
                            Text(day.title, style = MaterialTheme.typography.titleMedium)
                            Text(if (day.isRest) "Repos · on recharge les batteries" else "${day.durationMinutes} min · ${day.locationName}")
                            result?.let { Text(when (it.status) { "completed" -> "Séance terminée"; "partial" -> "Séance partielle"; else -> "Séance abandonnée" }) }
                            if (!day.isRest) Button(onClick = { start(day) }, enabled = !starting) { Text(if (result == null) "Faire ma séance" else "Refaire la séance") }
                        }
                    }
                }
            }
        }
        TextButton(onClick = onLocalCoach) { Text("Retrouver mon coach local et ses réglages") }
        Text("Les séances téléchargées et les résultats restent sur ce téléphone sans réseau.", style = MaterialTheme.typography.bodySmall)
    }
    browserCode?.let { pendingCode ->
        AlertDialog(onDismissRequest = { browserCode = null }, title = { Text("Ouvrir une session web ?") }, text = { Text("Autoriser ce navigateur sur ${repository.credentials.server} avec le profil ${state.snapshot?.member?.nickname.orEmpty()} ?\n\nCode : $pendingCode") }, confirmButton = { TextButton(onClick = { scope.launch { try { repository.approve(pendingCode); browserCode = null; code = ""; error = null } catch (e: Exception) { error = e.message; browserCode = null } } }) { Text("Autoriser la connexion") } }, dismissButton = { TextButton(onClick = { browserCode = null }) { Text("Annuler") } })
    }
}
