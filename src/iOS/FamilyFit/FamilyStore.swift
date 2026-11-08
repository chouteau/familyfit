import Foundation
import Observation
import SwiftData
import Network

@MainActor @Observable final class FamilyStore {
    var snapshot: FamilySnapshot?
    var busy = false
    var message: String?
    var error: String?
    var needsActivation = false
    var pendingCount = 0
    var rejectedResults: [PendingWorkout] = []
    var invitationCode = ""
    var invitation: InvitationPreview?
    var browserCode: String?
    var workout: WorkoutRunner?
    var activeAvailable = false
    var isActive = true
    private(set) var authorization: DeviceAuthorization?
    @ObservationIgnored private let context: ModelContext
    @ObservationIgnored private let monitor = NWPathMonitor()
    @ObservationIgnored private let mediaDirectory: URL
    @ObservationIgnored let container: ModelContainer
    let serverURL: URL

    init() throws {
            let schema = Schema([CachedSnapshot.self, PendingWorkout.self, ActiveWorkout.self])
            container = try ModelContainer(for: schema, configurations: [ModelConfiguration(schema: schema)])
            context = ModelContext(container); context.autosaveEnabled = false
            authorization = try DeviceKeychain.load()
            let configured = Bundle.main.object(forInfoDictionaryKey: "FamilyFitServerURL") as? String ?? "https://fit.chouteau.info"
            guard let url = URL(string: configured), url.scheme == "https", url.host != nil, url.user == nil, url.query == nil else { throw FamilyError.message("L’adresse du serveur FamilyFit n’est pas valide.") }
            serverURL = url
            if let saved = authorization, saved.server != url.absoluteString { authorization = nil; needsActivation = true }
            mediaDirectory = try FileManager.default.url(for: .applicationSupportDirectory, in: .userDomainMask, appropriateFor: nil, create: true).appending(path: "FamilyFitMedia", directoryHint: .isDirectory)
            try FileManager.default.createDirectory(at: mediaDirectory, withIntermediateDirectories: true)
            loadCache()
            monitor.pathUpdateHandler = { [weak self] path in
                guard path.status == .satisfied else { return }
                Task { @MainActor [weak self] in guard let self, self.isActive else { return }; await self.sync() }
            }
            monitor.start(queue: DispatchQueue(label: "FamilyFit.Network"))
    }
    var isPaired: Bool { authorization != nil && !needsActivation }
    private func loadCache() {
        snapshot = nil
        do {
            let snapshots = try context.fetch(FetchDescriptor<CachedSnapshot>())
            if let saved = snapshots.first(where: { $0.memberId == authorization?.memberId }) { snapshot = try JSONDecoder().decode(FamilySnapshot.self, from: saved.payload) }
            pendingCount = try context.fetchCount(FetchDescriptor<PendingWorkout>())
            rejectedResults = try context.fetch(FetchDescriptor<PendingWorkout>()).filter { $0.memberId == authorization?.memberId && $0.lastError?.hasPrefix("Rejeté : ") == true }
            activeAvailable = !(try context.fetch(FetchDescriptor<ActiveWorkout>())).isEmpty
        } catch { self.error = "Les données locales sont conservées, mais leur lecture a échoué. Réessaie après avoir relancé l’application." }
    }
    private func request(_ path: String, body: Data? = nil, authorized: Bool = true) async throws -> Data {
        var request = URLRequest(url: serverURL.appending(path: path), timeoutInterval: 45)
        if let body { request.httpMethod = "POST"; request.httpBody = body; request.setValue("application/json", forHTTPHeaderField: "Content-Type") }
        if authorized { guard let authorization else { throw FamilyError.http(401, "Active ton iPhone avec une invitation personnelle.") }; request.setValue("Bearer \(authorization.token)", forHTTPHeaderField: "Authorization") }
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let response = response as? HTTPURLResponse else { throw FamilyError.message("Le site n’a pas répondu.") }
        guard (200..<300).contains(response.statusCode) else {
            let problem = try? JSONDecoder().decode(Problem.self, from: data)
            throw FamilyError.http(response.statusCode, problem?.detail ?? (response.statusCode == 401 ? "Ce téléphone doit être réactivé avec une nouvelle invitation." : "Le site est momentanément indisponible."))
        }
        return data
    }
    func previewInvitation() async {
        guard !busy else { return }; busy = true; defer { busy = false }
        do { let code = cleanCode(invitationCode); let data = try await request("api/v1/auth/invitations/preview", body: JSONEncoder().encode(["code": code]), authorized: false); invitation = try JSONDecoder().decode(InvitationPreview.self, from: data); error = nil }
        catch { self.error = error.localizedDescription }
    }
    func activate() async {
        guard let invitation, !busy else { return }; busy = true
        do {
            let active = try context.fetch(FetchDescriptor<ActiveWorkout>()).first
            guard active == nil || active!.memberId == invitation.memberId else { throw FamilyError.message("Une séance d’un autre membre est encore ouverte. Termine-la avant de changer de profil.") }
            let data = try await request("api/v1/auth/enroll", body: JSONEncoder().encode(["code": cleanCode(invitationCode), "deviceName": "iPhone FamilyFit"]), authorized: false)
            let grant = try JSONDecoder().decode(DeviceGrant.self, from: data)
            guard grant.member.id == invitation.memberId else { throw FamilyError.message("Le profil de l’invitation a changé. Demande une nouvelle invitation.") }
            let credentials = DeviceAuthorization(token: grant.token, deviceId: grant.deviceId, memberId: grant.member.id, server: serverURL.absoluteString)
            try DeviceKeychain.save(credentials); authorization = credentials; needsActivation = false
            self.invitation = nil; invitationCode = ""; error = nil; message = "Bienvenue \(grant.member.nickname) !"; loadCache()
        } catch { self.error = error.localizedDescription }
        busy = false
        if isPaired { await sync() }
    }
    func approveBrowser(code: String) async {
        guard !code.isEmpty, !busy else { return }; busy = true; defer { busy = false }
        do { _ = try await request("api/v1/auth/browser/approve", body: JSONEncoder().encode(["code": cleanCode(code)])); browserCode = nil; message = "Connexion au site confirmée."; error = nil }
        catch { self.error = error.localizedDescription }
    }
    func handleQR(_ text: String) async {
        do {
            guard let components = URLComponents(string: text), components.scheme == "familyfit", let purpose = components.host else { throw FamilyError.message("Ce QR code n’est pas un code FamilyFit.") }
            let values = Dictionary(components.queryItems?.compactMap { item in item.value.map { (item.name, $0) } } ?? [], uniquingKeysWith: { first, _ in first })
            if let server = values["server"], server.trimmingCharacters(in: CharacterSet(charactersIn: "/")) != serverURL.absoluteString.trimmingCharacters(in: CharacterSet(charactersIn: "/")) { throw FamilyError.message("Ce QR code appartient à un autre serveur FamilyFit.") }
            switch purpose {
            case "enroll": invitationCode = values["code"] ?? ""; await previewInvitation()
            case "login": guard isPaired else { throw FamilyError.message("Active d’abord ton iPhone avec ton invitation personnelle.") }; browserCode = values["code"]
            case "workout":
                if let day = snapshot?.days.first(where: { $0.id == values["day"] }) { try startWorkout(day) }
                else { throw FamilyError.message("Synchronise ton mobile pour retrouver cette séance de ton programme.") }
            default: throw FamilyError.message("Ce QR code n’est pas reconnu.")
            }
        } catch { self.error = error.localizedDescription }
    }
    func sync(retryRejected: Bool = false) async {
        guard let authorization, !busy else { return }; busy = true; message = "Synchronisation…"; defer { busy = false }
        do {
            let pending = try context.fetch(FetchDescriptor<PendingWorkout>(sortBy: [SortDescriptor(\.createdAt)]))
            for item in pending where item.memberId == authorization.memberId {
                if !retryRejected && item.lastError?.hasPrefix("Rejeté : ") == true { continue }
                do { _ = try await request("api/v1/workouts", body: item.payload); context.delete(item); try context.save() }
                catch {
                    if case let FamilyError.http(status, _) = error, [400, 403, 404, 409, 422].contains(status) {
                        item.lastError = "Rejeté : \(error.localizedDescription)"; try context.save()
                    } else { item.lastError = error.localizedDescription; try? context.save(); throw error }
                }
            }
            let data = try await request("api/v1/sync")
            let received = try JSONDecoder().decode(FamilySnapshot.self, from: data)
            guard received.member.id == authorization.memberId else { throw FamilyError.message("Le profil reçu ne correspond pas à cet iPhone.") }
            let caches = try context.fetch(FetchDescriptor<CachedSnapshot>())
            if let existing = caches.first(where: { $0.memberId == authorization.memberId }) { existing.payload = data; existing.receivedAt = .now }
            else { context.insert(CachedSnapshot(memberId: authorization.memberId, payload: data)) }
            try context.save(); snapshot = received; needsActivation = false
            message = "Téléchargement des exercices…"
            let urls = Set(received.days.flatMap(\.exercises).flatMap { [$0.imageUrl, $0.videoUrl].compactMap { $0 } })
            var missing = 0
            for url in urls { do { try await downloadMedia(url) } catch { missing += 1 } }
            message = missing == 0 ? "Tout est synchronisé." : "Séances synchronisées. Certains médias restent à télécharger."
            error = nil; loadCache()
        } catch {
            if case FamilyError.http(401, _) = error { needsActivation = true }
            self.error = error.localizedDescription; message = "Les séances et résultats restent conservés sur cet iPhone."; loadCache()
        }
    }
    func mediaURL(_ path: String?) -> URL? {
        guard let path, path.hasPrefix("/media/"), !path.contains("..") else { return nil }
        let file = mediaDirectory.appending(path: URL(fileURLWithPath: path).lastPathComponent)
        return FileManager.default.fileExists(atPath: file.path) ? file : nil
    }
    private func downloadMedia(_ path: String) async throws {
        guard path.hasPrefix("/media/"), !path.contains(".."), let authorization else { throw FamilyError.message("Média invalide.") }
        if mediaURL(path) != nil { return }
        var request = URLRequest(url: serverURL.appending(path: String(path.dropFirst())), timeoutInterval: 60)
        request.setValue("Bearer \(authorization.token)", forHTTPHeaderField: "Authorization")
        let (temp, response) = try await URLSession.shared.download(for: request)
        defer { try? FileManager.default.removeItem(at: temp) }
        guard let response = response as? HTTPURLResponse, response.statusCode == 200 else { throw FamilyError.message("Média indisponible.") }
        let file = mediaDirectory.appending(path: URL(fileURLWithPath: path).lastPathComponent)
        try FileManager.default.moveItem(at: temp, to: file)
    }
    func startWorkout(_ day: SessionDay) throws {
        guard !day.isRest, let authorization else { return }
        guard snapshot?.member.id == authorization.memberId, snapshot?.days.contains(where: { $0.id == day.id }) == true else { throw FamilyError.message("Synchronise ton profil avant de démarrer cette séance.") }
        if let existing = try context.fetch(FetchDescriptor<ActiveWorkout>()).first {
            let activeDay = try JSONDecoder().decode(SessionDay.self, from: existing.dayPayload)
            guard existing.memberId == authorization.memberId, activeDay.id == day.id else { throw FamilyError.message("Une séance est déjà ouverte. Reprends-la ou termine-la d’abord.") }
            try open(existing); return
        }
        let active = try ActiveWorkout(memberId: authorization.memberId, day: day)
        context.insert(active); try context.save(); activeAvailable = true; try open(active)
    }
    func resumeWorkout() { do { if let active = try context.fetch(FetchDescriptor<ActiveWorkout>()).first { guard active.memberId == authorization?.memberId else { throw FamilyError.message("Cette séance appartient à un autre profil.") }; try open(active) } } catch { self.error = error.localizedDescription } }
    private func open(_ active: ActiveWorkout) throws {
        let day = try JSONDecoder().decode(SessionDay.self, from: active.dayPayload)
        var checkpoint = try JSONDecoder().decode(WorkoutCheckpoint.self, from: active.checkpointPayload)
        if checkpoint.phase == "running" || checkpoint.phase == "resting" { checkpoint.pausedPhase = checkpoint.phase; checkpoint.phase = "paused" }
        workout = WorkoutRunner(day: day, checkpoint: checkpoint) { [weak self, weak active] saved in
            guard let self, let active else { return }
            do { active.checkpointPayload = try JSONEncoder().encode(saved); try self.context.save() }
            catch { self.error = "L’enregistrement local a échoué. Libère de l’espace avant de quitter la séance." }
        }
    }
    func finishWorkout() async {
        guard let runner = workout else { return }
        runner.pause()
        do {
            guard let active = try context.fetch(FetchDescriptor<ActiveWorkout>()).first, active.memberId == authorization?.memberId else { throw FamilyError.message("La séance ouverte ne correspond pas au profil actuel.") }
            let steps = runner.day.steps
            let performances = runner.day.exercises.map { exercise in
                let done = runner.checkpoint.completed.filter { steps.indices.contains($0) && steps[$0].exercise.id == exercise.id }.count
                return ExercisePerformance(exerciseId: exercise.id, completedSets: done, repetitions: exercise.targetType == "repetitions" ? exercise.target : nil, durationSeconds: exercise.targetType == "time" ? exercise.target * done : nil, weightKg: nil, skipped: runner.checkpoint.skipped.contains { steps.indices.contains($0) && steps[$0].exercise.id == exercise.id })
            }
            let submission = WorkoutSubmission(id: active.id, programDayId: runner.day.id, startedAtUtc: active.startedAtUtc, finishedAtUtc: FamilyDates.instant(.now), exercises: performances)
            let queued = try context.fetch(FetchDescriptor<PendingWorkout>()).contains { $0.id == active.id }
            if !queued { context.insert(PendingWorkout(id: active.id, memberId: active.memberId, payload: try JSONEncoder().encode(submission))) }
            context.delete(active); try context.save()
            runner.stop(); workout = nil; activeAvailable = false; message = "Séance enregistrée sur cet iPhone."; loadCache(); await sync()
        } catch { context.rollback(); self.error = error.localizedDescription }
    }
    func closeWorkout() { guard let current = workout else { return }; current.pause(); current.stop(); workout = nil; activeAvailable = ((try? context.fetchCount(FetchDescriptor<ActiveWorkout>())) ?? 0) > 0 }
    func sceneChanged(active: Bool) { isActive = active; if !active { workout?.pause(); workout?.disableVoice() } else { Task { await sync() } } }
    func cleanCode(_ code: String) -> String { code.uppercased().filter { !$0.isWhitespace && $0 != "-" } }
    private struct Problem: Decodable { let detail: String? }
}
