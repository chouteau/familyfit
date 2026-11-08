import Foundation
import AVFoundation
import Speech

@MainActor final class VoiceCoach: NSObject, AVSpeechSynthesizerDelegate {
    var onCommand: ((String) -> Void)?
    private let speech = AVSpeechSynthesizer()
    private let engine = AVAudioEngine()
    private let recognizer = SFSpeechRecognizer(locale: Locale(identifier: "fr-FR"))
    private var recognition: SFSpeechRecognitionTask?
    private var request: SFSpeechAudioBufferRecognitionRequest?
    private var commandsEnabled = false
    private var tapped = false
    private var lastCommand = ""
    private var lastCommandTime = Date.distantPast
    override init() { super.init(); speech.delegate = self }
    func speak(_ text: String) {
        stopListening()
        speech.stopSpeaking(at: .immediate)
        let utterance = AVSpeechUtterance(string: text)
        utterance.voice = AVSpeechSynthesisVoice(language: "fr-FR"); utterance.rate = AVSpeechUtteranceDefaultSpeechRate
        speech.speak(utterance)
    }
    func enableCommands() async throws {
        let speechPermission = await withCheckedContinuation { continuation in SFSpeechRecognizer.requestAuthorization { continuation.resume(returning: $0) } }
        let microphonePermission = await withCheckedContinuation { continuation in AVAudioApplication.requestRecordPermission { continuation.resume(returning: $0) } }
        guard speechPermission == .authorized, microphonePermission, recognizer?.isAvailable == true else { throw FamilyError.message("Microphone ou reconnaissance vocale indisponible.") }
        let session = AVAudioSession.sharedInstance()
        try session.setCategory(.playAndRecord, mode: .default, options: [.defaultToSpeaker, .allowBluetooth, .duckOthers])
        try session.setActive(true)
        commandsEnabled = true
        if !speech.isSpeaking { try startListening() }
    }
    func disableCommands() { commandsEnabled = false; stopListening() }
    private func startListening() throws {
        guard commandsEnabled, !speech.isSpeaking, recognizer?.isAvailable == true else { return }
        stopListening()
        let input = engine.inputNode
        let format = input.outputFormat(forBus: 0)
        guard format.sampleRate > 0, format.channelCount > 0 else { throw FamilyError.message("Entrée audio indisponible.") }
        let request = SFSpeechAudioBufferRecognitionRequest(); request.shouldReportPartialResults = true
        if recognizer?.supportsOnDeviceRecognition == true { request.requiresOnDeviceRecognition = true }
        self.request = request
        input.installTap(onBus: 0, bufferSize: 1024, format: format) { buffer, _ in request.append(buffer) }; tapped = true
        engine.prepare(); try engine.start()
        recognition = recognizer?.recognitionTask(with: request) { [weak self] result, error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                if let text = result?.bestTranscription.formattedString { self.interpret(text) }
                if error != nil || result?.isFinal == true {
                    self.stopListening()
                    try? await Task.sleep(for: .milliseconds(400))
                    if self.commandsEnabled, !self.speech.isSpeaking { try? self.startListening() }
                }
            }
        }
    }
    private func interpret(_ input: String) {
        let text = input.folding(options: [.diacriticInsensitive, .caseInsensitive], locale: Locale(identifier: "fr-FR"))
        let command: String?
        if text.contains("pause") || text.contains("stop") { command = "pause" }
        else if text.contains("zero") || text.contains("recommence") { command = "reset" }
        else if text.contains("suivant") || text.contains("passe") { command = "next" }
        else if text.contains("repete") || text.contains("conseil") { command = "repeat" }
        else if text.contains("go") || text.contains("demarre") || text.contains("reprend") || text.contains("c'est parti") { command = "go" }
        else { command = nil }
        guard let command, command != lastCommand || Date.now.timeIntervalSince(lastCommandTime) > 2 else { return }
        lastCommand = command; lastCommandTime = .now; onCommand?(command)
    }
    private func stopListening() {
        recognition?.cancel(); recognition = nil; request?.endAudio(); request = nil
        if engine.isRunning { engine.stop() }
        if tapped { engine.inputNode.removeTap(onBus: 0); tapped = false }
    }
    nonisolated func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) { Task { @MainActor [weak self] in guard let self, self.commandsEnabled else { return }; try? self.startListening() } }
    func stop() { commandsEnabled = false; stopListening(); speech.stopSpeaking(at: .immediate); try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation) }
}
