import SwiftUI
import SwiftData

@main @MainActor struct FamilyFitApp: App {
    @State private var store: FamilyStore?
    @Environment(\.scenePhase) private var scenePhase
    init() { _store = State(initialValue: try? FamilyStore()) }
    var body: some Scene {
        WindowGroup {
            if let store {
                RootView().environment(store).modelContainer(store.container).tint(.fitGreen).preferredColorScheme(.light)
                .onOpenURL { url in Task { await store.handleQR(url.absoluteString) } }
                .onChange(of: scenePhase) { _, phase in store.sceneChanged(active: phase == .active) }
                .task { await store.sync() }
            } else {
                ContentUnavailableView("Le stockage est indisponible", systemImage: "externaldrive.badge.exclamationmark", description: Text("Tes données existantes sont conservées. Déverrouille l’iPhone, vérifie l’espace disponible puis relance FamilyFit."))
            }
        }
    }
}
