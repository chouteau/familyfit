import Foundation
import Observation

@MainActor @Observable final class WorkoutRunner {
    let day: SessionDay
    let steps: [WorkoutStep]
    var checkpoint: WorkoutCheckpoint { didSet { persist(checkpoint) } }
    var voiceEnabled = false
    var voiceError: String?
    @ObservationIgnored private let persist: (WorkoutCheckpoint) -> Void
    @ObservationIgnored private let voice = VoiceCoach()
    @ObservationIgnored private var timer: Task<Void, Never>?
    var step: WorkoutStep? { steps.indices.contains(checkpoint.index) ? steps[checkpoint.index] : nil }
    var isFinished: Bool { checkpoint.phase == "finished" }
    var fullyCompleted: Bool { checkpoint.completed.count == steps.count && checkpoint.skipped.isEmpty }
    var progress: Double { steps.isEmpty ? 0 : Double(checkpoint.completed.count + checkpoint.skipped.count) / Double(steps.count) }

    init(day: SessionDay, checkpoint: WorkoutCheckpoint, persist: @escaping (WorkoutCheckpoint) -> Void) {
        self.day = day; steps = day.steps; self.checkpoint = checkpoint; self.persist = persist
        voice.onCommand = { [weak self] command in
            guard let self else { return }
            switch command { case "go": self.go(); case "pause": self.pause(); case "next": self.skip(); case "reset": self.reset(); case "repeat": self.repeatCue(); default: break }
        }
    }
    func prepare() {
        guard let step else { checkpoint.phase = "finished"; return }
        checkpoint.phase = "ready"; checkpoint.remaining = step.exercise.target
        voice.speak("\(step.exercise.name), série \(step.set) sur \(step.exercise.sets). \(step.exercise.instructions) Dis GO quand tu es prêt.")
    }
    func go() {
        guard let step, !isFinished else { return }
        switch checkpoint.phase {
        case "ready":
            checkpoint.phase = "running"
            voice.speak("C’est parti ! \(step.exercise.target) \(step.exercise.targetType == "time" ? "secondes" : "répétitions"). \(step.exercise.postureTip)")
            if step.exercise.targetType == "time" { countdown(step.exercise.target) { [weak self] in self?.completeCurrent() } }
        case "paused":
            checkpoint.phase = checkpoint.pausedPhase
            voice.speak("On reprend !")
            if checkpoint.phase == "resting" { countdown(checkpoint.remaining) { [weak self] in self?.prepare() } }
            else if step.exercise.targetType == "time" { countdown(checkpoint.remaining) { [weak self] in self?.completeCurrent() } }
        case "running": if step.exercise.targetType == "repetitions" { completeCurrent() }
        case "resting": timer?.cancel(); prepare(); go()
        default: break
        }
    }
    private func countdown(_ seconds: Int, finished: @escaping () -> Void) {
        timer?.cancel()
        let deadline = ProcessInfo.processInfo.systemUptime + Double(seconds)
        timer = Task { [weak self] in
            while !Task.isCancelled {
                guard let self else { return }
                let remaining = max(0, Int(ceil(deadline - ProcessInfo.processInfo.systemUptime)))
                if remaining != self.checkpoint.remaining { self.checkpoint.remaining = remaining }
                if remaining == 0 { finished(); return }
                try? await Task.sleep(for: .milliseconds(200))
            }
        }
    }
    private func completeCurrent() {
        guard checkpoint.phase == "running", step != nil else { return }
        timer?.cancel()
        checkpoint.completed = Array(Set(checkpoint.completed + [checkpoint.index])).sorted()
        advance()
    }
    func skip() {
        guard step != nil, !isFinished else { return }
        timer?.cancel(); checkpoint.skipped = Array(Set(checkpoint.skipped + [checkpoint.index])).sorted(); advance()
    }
    private func advance() {
        let rest = step?.exercise.restSeconds ?? 0
        checkpoint.index += 1
        guard step != nil else { checkpoint.phase = "finished"; checkpoint.remaining = 0; voice.speak("Ta séance est parcourue. Confirme son enregistrement quand tu es prêt."); return }
        if rest > 0 { checkpoint.phase = "resting"; checkpoint.remaining = rest; voice.speak("Bien joué ! Récupère \(rest) secondes."); countdown(rest) { [weak self] in self?.prepare() } }
        else { prepare() }
    }
    func pause() {
        guard checkpoint.phase == "running" || checkpoint.phase == "resting" else { return }
        timer?.cancel(); checkpoint.pausedPhase = checkpoint.phase; checkpoint.phase = "paused"; voice.speak("Séance en pause.")
    }
    func reset() {
        guard let step, !isFinished else { return }
        timer?.cancel(); checkpoint.phase = "ready"; checkpoint.remaining = step.exercise.target; voice.speak("Chrono remis à zéro. Dis GO quand tu es prêt.")
    }
    func repeatCue() { if let step { voice.speak("\(step.exercise.name). \(step.exercise.instructions). \(step.exercise.postureTip)") } }
    func enableVoice() async { do { try await voice.enableCommands(); voiceEnabled = true; voiceError = nil } catch { voiceEnabled = false; voiceError = "Les commandes vocales sont indisponibles. Les boutons restent utilisables." } }
    func disableVoice() { voice.disableCommands(); voiceEnabled = false }
    func stop() { timer?.cancel(); voice.stop() }
}
