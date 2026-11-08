import SwiftUI
import AVKit

extension Color {
    static let fitGreen = Color(red: 0.23, green: 0.58, blue: 0.04)
    static let fitInk = Color(red: 0.09, green: 0.13, blue: 0.24)
    static let fitIvory = Color(red: 0.98, green: 0.97, blue: 0.95)
    static let fitLemon = Color(red: 1, green: 0.96, blue: 0.78)
}
struct FitButtonStyle: ButtonStyle {
    @Environment(\.isEnabled) private var enabled
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    func makeBody(configuration: Configuration) -> some View {
        configuration.label.font(.system(.headline, design: .rounded)).frame(maxWidth: .infinity).padding(.vertical, 14).padding(.horizontal, 15)
            .foregroundStyle(.white).background(Color.fitGreen.opacity(enabled ? 1 : 0.45), in: RoundedRectangle(cornerRadius: 14))
            .scaleEffect(configuration.isPressed ? 0.98 : 1).animation(reduceMotion ? nil : .easeOut(duration: 0.12), value: configuration.isPressed)
    }
}
struct RootView: View {
    @Environment(FamilyStore.self) private var store
    @State private var showScanner = false
    var body: some View {
        @Bindable var store = store
        let approvalCode = store.browserCode ?? ""
        Group {
            if store.isPaired {
                TabView {
                    PlanningView().tabItem { Label("Mon planning", systemImage: "calendar") }
                    FamilyView().tabItem { Label("La famille", systemImage: "person.2.fill") }
                    ConnectionView(scan: { showScanner = true }).tabItem { Label("Connexion", systemImage: "qrcode.viewfinder") }
                }
            } else { EnrollmentView(scan: { showScanner = true }) }
        }
        .sheet(isPresented: $showScanner) {
            NavigationStack {
                QRScanner(onCode: { code in showScanner = false; Task { await store.handleQR(code) } }, onFailure: { message in showScanner = false; store.error = message })
                    .ignoresSafeArea(edges: .bottom).navigationTitle("Scanner un QR FamilyFit").navigationBarTitleDisplayMode(.inline)
                    .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Fermer") { showScanner = false } } }
            }
        }
        .confirmationDialog("Ouvrir une session web ?", isPresented: Binding(get: { store.browserCode != nil }, set: { if !$0 { store.browserCode = nil } }), titleVisibility: .visible) {
            Button("Autoriser la connexion") { Task { await store.approveBrowser(code: approvalCode) } }
            Button("Annuler", role: .cancel) { store.browserCode = nil }
        } message: { Text("Autoriser ce navigateur sur \(store.serverURL.host ?? "fit.chouteau.info") avec le profil \(store.snapshot?.member.nickname ?? "de ce téléphone") ?\n\nCode : \(store.browserCode ?? "")") }
        .alert("Un petit contretemps", isPresented: Binding(get: { store.error != nil }, set: { if !$0 { store.error = nil } })) { Button("D’accord") { store.error = nil } } message: { Text(store.error ?? "") }
        .fullScreenCover(isPresented: Binding(get: { store.workout != nil }, set: { if !$0 { store.closeWorkout() } })) { if let workout = store.workout { WorkoutView(runner: workout).environment(store).interactiveDismissDisabled() } }
    }
}
struct EnrollmentView: View {
    @Environment(FamilyStore.self) private var store
    let scan: () -> Void
    var body: some View {
        @Bindable var store = store
        NavigationStack {
            ScrollView {
                VStack(spacing: 23) {
                    Image("Mascot").resizable().scaledToFit().frame(height: 190).accessibilityHidden(true)
                    Text("Bienvenue dans la famille !").font(.system(.largeTitle, design: .rounded, weight: .black)).multilineTextAlignment(.center)
                    Text("Ton invitation personnelle associe cet iPhone à ton profil. Aucun mot de passe à retenir.").foregroundStyle(.secondary).multilineTextAlignment(.center)
                    Button(action: scan) { Label("Scanner mon invitation", systemImage: "qrcode.viewfinder") }.buttonStyle(FitButtonStyle())
                    TextField("Code d’invitation", text: $store.invitationCode).textInputAutocapitalization(.characters).autocorrectionDisabled().textContentType(.oneTimeCode).keyboardType(.asciiCapable).padding().background(.white, in: RoundedRectangle(cornerRadius: 13)).onChange(of: store.invitationCode) { _, _ in store.invitation = nil }
                    Button("Vérifier l’invitation") { Task { await store.previewInvitation() } }.buttonStyle(.bordered).disabled(store.cleanCode(store.invitationCode).count != 10 || store.busy)
                    if let invitation = store.invitation {
                        VStack(spacing: 14) { Text("Ce téléphone sera associé à").foregroundStyle(.secondary); Text(invitation.nickname).font(.system(.title, design: .rounded, weight: .bold)); Button("Confirmer et activer mon iPhone") { Task { await store.activate() } }.buttonStyle(FitButtonStyle()).disabled(store.busy) }.padding().background(Color.fitLemon, in: RoundedRectangle(cornerRadius: 16))
                    }
                    if store.busy { ProgressView("Connexion…") }
                    Text("Demande ton QR code ou ton code d’activation à l’administrateur familial.").font(.footnote).foregroundStyle(.secondary).multilineTextAlignment(.center)
                }.padding(25)
            }.background(Color.fitIvory).navigationTitle("FamilyFit")
        }
    }
}
struct PlanningView: View {
    @Environment(FamilyStore.self) private var store
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    HStack { VStack(alignment: .leading, spacing: 5) { Text("Salut \(store.snapshot?.member.nickname ?? "la famille") !").font(.system(.largeTitle, design: .rounded, weight: .black)); Text("Un peu de mouvement, beaucoup de bonne humeur.").foregroundStyle(.secondary) }; Spacer(); Image("Mascot").resizable().scaledToFit().frame(width: 90, height: 100).accessibilityHidden(true) }
                    if let message = store.message { Label(message, systemImage: "arrow.triangle.2.circlepath").font(.footnote).foregroundStyle(.secondary) }
                    if store.busy { ProgressView().frame(maxWidth: .infinity) }
                    HStack { Text("\(store.snapshot?.results.filter { $0.status == "completed" }.count ?? 0) séance(s) terminée(s)").font(.headline); Spacer(); Text("\(store.pendingCount) à envoyer").font(.caption).foregroundStyle(.secondary) }.padding().background(Color.fitLemon, in: RoundedRectangle(cornerRadius: 15))
                    if store.activeAvailable { VStack(alignment: .leading, spacing: 12) { Text("Ta séance est conservée.").font(.headline); Text("Reprends là où tu t’es arrêté."); Button("Reprendre ma séance") { store.resumeWorkout() }.buttonStyle(FitButtonStyle()) }.padding().background(.white, in: RoundedRectangle(cornerRadius: 16)) }
                    Button { Task { await store.sync() } } label: { Label("Synchroniser", systemImage: "arrow.triangle.2.circlepath") }.buttonStyle(.bordered).disabled(store.busy)
                    if !store.rejectedResults.isEmpty {
                        VStack(alignment: .leading, spacing: 10) {
                            Text("Résultats à vérifier").font(.headline)
                            Text("Ils restent conservés sur cet iPhone. Les autres séances continuent de se synchroniser.").font(.footnote)
                            ForEach(store.rejectedResults) { item in Text("\(item.id.prefix(8)) · \(item.lastError ?? "Envoi à vérifier")").font(.caption) }
                            Button("Réessayer ces envois") { Task { await store.sync(retryRejected: true) } }.disabled(store.busy)
                        }.padding().background(Color.fitLemon, in: RoundedRectangle(cornerRadius: 16))
                    }
                    if let snapshot = store.snapshot, !snapshot.days.isEmpty {
                        ForEach(snapshot.days.sorted { $0.date < $1.date }) { day in
                            VStack(alignment: .leading, spacing: 11) {
                                Text(FamilyDates.displayDay(day.date)).font(.subheadline.weight(.bold)).foregroundStyle(Color.fitGreen)
                                HStack { Text(day.title).font(.system(.title2, design: .rounded, weight: .bold)); Spacer(); StatusBadge(status: status(day, snapshot)) }
                                if day.isRest { Label("On recharge les batteries.", systemImage: "bed.double.fill").foregroundStyle(.secondary) }
                                else {
                                    Label("\(day.durationMinutes) min · \(day.locationName)", systemImage: "mappin.and.ellipse").font(.subheadline).foregroundStyle(.secondary)
                                    ForEach(day.exercises) { exercise in Text("\(exercise.name) · \(exercise.sets) × \(exercise.target) \(exercise.targetType == "time" ? "s" : "rép.")").font(.footnote) }
                                    Button("Faire ma séance") { do { try store.startWorkout(day) } catch { store.error = error.localizedDescription } }.buttonStyle(FitButtonStyle())
                                }
                            }.padding(19).frame(maxWidth: .infinity, alignment: .leading).background(.white, in: RoundedRectangle(cornerRadius: 16))
                        }
                    } else { ContentUnavailableView("Ton programme arrive bientôt", systemImage: "calendar", description: Text("Il apparaîtra après validation par l’administrateur et synchronisation.")) }
                }.padding(20)
            }.background(Color.fitIvory).navigationTitle("Mon planning").navigationBarTitleDisplayMode(.inline).refreshable { await store.sync() }
        }
    }
    private func status(_ day: SessionDay, _ snapshot: FamilySnapshot) -> String {
        if day.isRest { return "rest" }
        let results = snapshot.results.filter { $0.programDayId == day.id }
        if results.contains(where: { $0.status == "completed" }) { return "completed" }
        if results.contains(where: { $0.status == "partial" }) { return "partial" }
        if !results.isEmpty { return "abandoned" }
        return day.calendarDate < FamilyDates.paris.startOfDay(for: .now) ? "missed" : "upcoming"
    }
}
struct FamilyView: View {
    @Environment(FamilyStore.self) private var store
    var body: some View {
        NavigationStack { List(store.snapshot?.members ?? []) { member in HStack(alignment: .top, spacing: 15) { Text(String(member.nickname.prefix(1))).font(.system(.title, design: .rounded, weight: .black)).foregroundStyle(Color.fitGreen).frame(width: 48, height: 48).background(Color.fitGreen.opacity(0.12), in: Circle()); VStack(alignment: .leading, spacing: 6) { Text(member.nickname).font(.headline); Text("\(member.age) ans · \(member.weightKg.formatted(.number.precision(.fractionLength(0...1)))) kg").font(.subheadline).foregroundStyle(.secondary); Text(member.goal).font(.subheadline); Text(member.level).font(.caption).foregroundStyle(.secondary) } }.padding(.vertical, 8) }.navigationTitle("La famille").refreshable { await store.sync() } }
    }
}
struct ConnectionView: View {
    @Environment(FamilyStore.self) private var store
    @State private var code = ""
    let scan: () -> Void
    var body: some View {
        NavigationStack { ScrollView { VStack(spacing: 23) { Image("Mascot").resizable().scaledToFit().frame(height: 165).accessibilityHidden(true); Text("Ton iPhone ouvre le site.").font(.system(.title, design: .rounded, weight: .black)).multilineTextAlignment(.center); Text("Sur fit.chouteau.info, affiche le QR code puis scanne-le ici. Tu confirmeras la connexion avec ton profil.").foregroundStyle(.secondary).multilineTextAlignment(.center); Button(action: scan) { Label("Scanner le QR code du site", systemImage: "qrcode.viewfinder") }.buttonStyle(FitButtonStyle()); TextField("Code affiché sur le site", text: $code).textInputAutocapitalization(.characters).autocorrectionDisabled().keyboardType(.asciiCapable).textContentType(.oneTimeCode).padding().background(.white, in: RoundedRectangle(cornerRadius: 12)); Button("Confirmer une connexion") { store.browserCode = store.cleanCode(code) }.buttonStyle(.bordered).disabled(store.cleanCode(code).count != 10 || store.busy); Text("Connecté en tant que \(store.snapshot?.member.nickname ?? "membre de la famille")").font(.footnote).foregroundStyle(.secondary) }.padding(25) }.background(Color.fitIvory).navigationTitle("Connexion au site") }
    }
}
struct StatusBadge: View {
    let status: String
    private var label: String { switch status { case "completed": return "Terminée"; case "partial": return "Partielle"; case "abandoned": return "Abandonnée"; case "missed": return "Non réalisée"; case "rest": return "Repos"; default: return "À venir" } }
    private var color: Color { switch status { case "completed": return .fitGreen; case "partial": return .orange; case "abandoned", "missed": return .red; case "rest": return .secondary; default: return .blue } }
    var body: some View { Text(label).font(.caption.weight(.bold)).padding(.horizontal, 10).padding(.vertical, 6).foregroundStyle(color).background(color.opacity(0.12), in: Capsule()) }
}
struct WorkoutView: View {
    @Environment(FamilyStore.self) private var store
    @Bindable var runner: WorkoutRunner
    @State private var showExit = false
    @State private var confirmEnd = false
    @State private var saving = false
    var body: some View {
        NavigationStack {
            ScrollView { VStack(spacing: 21) {
                if let error = store.error { Text(error).font(.footnote).foregroundStyle(.red) }
                ProgressView(value: runner.progress).tint(.fitGreen)
                Text("\(runner.checkpoint.completed.count) série(s) réalisée(s) sur \(runner.steps.count)").font(.subheadline).foregroundStyle(.secondary)
                if let step = runner.step, !runner.isFinished {
                    Text(runner.checkpoint.phase == "resting" ? "Récupération" : step.exercise.name).font(.system(.largeTitle, design: .rounded, weight: .black)).multilineTextAlignment(.center)
                    Text("Série \(step.set) sur \(step.exercise.sets)").font(.headline).foregroundStyle(Color.fitGreen)
                    WorkoutMedia(exercise: step.exercise).id(step.id).frame(height: 220)
                    Text("\(runner.checkpoint.phase == "resting" ? runner.checkpoint.remaining : step.exercise.targetType == "time" ? runner.checkpoint.remaining : step.exercise.target)").font(.system(size: 72, weight: .black, design: .rounded)).monospacedDigit().contentTransition(.numericText()).accessibilityLabel("\(runner.checkpoint.remaining) \(step.exercise.targetType == "time" ? "secondes" : "répétitions")")
                    Text(runner.checkpoint.phase == "resting" || step.exercise.targetType == "time" ? "secondes" : "répétitions").font(.headline).foregroundStyle(.secondary)
                    Text(step.exercise.instructions).multilineTextAlignment(.center)
                    Text(step.exercise.postureTip).font(.subheadline).foregroundStyle(.secondary).multilineTextAlignment(.center)
                    if runner.checkpoint.phase == "running" && step.exercise.targetType == "time" { Button("Pause") { runner.pause() }.buttonStyle(FitButtonStyle()) }
                    else { Button(runner.checkpoint.phase == "paused" ? "Reprendre" : runner.checkpoint.phase == "running" ? "Série terminée" : runner.checkpoint.phase == "resting" ? "Passer la récupération" : "C’est parti !") { runner.go() }.buttonStyle(FitButtonStyle()) }
                    HStack { Button("Remettre à zéro") { runner.reset() }.buttonStyle(.bordered); Spacer(); Button("Passer l’exercice") { runner.skip() }.buttonStyle(.bordered) }
                    Button(runner.voiceEnabled ? "Désactiver les commandes vocales" : "Activer les commandes vocales") { Task { if runner.voiceEnabled { runner.disableVoice() } else { await runner.enableVoice() } } }.font(.subheadline)
                    if let error = runner.voiceError { Text(error).font(.footnote).foregroundStyle(.secondary) }
                } else {
                    Image("Mascot").resizable().scaledToFit().frame(height: 230).accessibilityHidden(true)
                    Text(runner.fullyCompleted ? "Bravo, séance terminée !" : "Ton effort compte.").font(.system(.largeTitle, design: .rounded, weight: .black)).multilineTextAlignment(.center)
                    Text("Confirme l’enregistrement pour mettre à jour le suivi familial dès le retour du réseau.").foregroundStyle(.secondary).multilineTextAlignment(.center)
                    Button("Enregistrer ma séance") { confirmEnd = true }.buttonStyle(FitButtonStyle())
                }
            }.padding(23) }.background(Color.fitIvory).navigationTitle(runner.day.title).navigationBarTitleDisplayMode(.inline)
                .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Fermer") { runner.pause(); showExit = true } } }
        }
        .onAppear { UIApplication.shared.isIdleTimerDisabled = true; if runner.checkpoint.phase == "ready" { runner.prepare() } }
        .onDisappear { UIApplication.shared.isIdleTimerDisabled = false }
        .confirmationDialog("Que faire de ta séance ?", isPresented: $showExit, titleVisibility: .visible) { Button("Conserver pour reprendre plus tard") { store.closeWorkout() }; Button("Terminer et enregistrer") { confirmEnd = true }; Button("Continuer", role: .cancel) { } }
        .alert("Enregistrer cette séance ?", isPresented: $confirmEnd) { Button("Confirmer et enregistrer") { guard !saving else { return }; saving = true; Task { await store.finishWorkout(); saving = false } }; Button("Continuer la séance", role: .cancel) { } } message: { Text(runner.fullyCompleted ? "Tous les exercices ont été réalisés. La séance comptera comme terminée." : "Les exercices réalisés et sautés sont conservés. La séance sera marquée comme partielle ou abandonnée.") }
    }
}
struct WorkoutMedia: View {
    @Environment(FamilyStore.self) private var store
    let exercise: SessionExercise
    @State private var player: AVPlayer?
    var body: some View {
        Group {
            if let player { VideoPlayer(player: player).clipShape(RoundedRectangle(cornerRadius: 16)) }
            else if let file = store.mediaURL(exercise.imageUrl), let image = UIImage(contentsOfFile: file.path) { Image(uiImage: image).resizable().scaledToFit().clipShape(RoundedRectangle(cornerRadius: 16)).accessibilityLabel(exercise.name) }
            else { VStack(spacing: 15) { Image(systemName: "figure.strengthtraining.functional").font(.system(size: 76)).foregroundStyle(Color.fitGreen); Text(exercise.muscle).font(.subheadline).foregroundStyle(.secondary) }.frame(maxWidth: .infinity).frame(height: 210).background(.white, in: RoundedRectangle(cornerRadius: 16)) }
        }
        .onAppear { if let file = store.mediaURL(exercise.videoUrl) { let video = AVPlayer(url: file); video.isMuted = true; player = video; video.play() } }
        .onDisappear { player?.pause(); player = nil }
        .onReceive(NotificationCenter.default.publisher(for: .AVPlayerItemDidPlayToEndTime)) { notification in if let item = notification.object as? AVPlayerItem, item === player?.currentItem { player?.seek(to: .zero); player?.play() } }
    }
}
