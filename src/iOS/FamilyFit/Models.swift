import Foundation
import SwiftData

struct FamilyMember: Codable, Identifiable, Sendable {
    let id: String
    let nickname: String
    let age: Int
    let weightKg: Double
    let goal: String
    let level: String
    let isAdmin: Bool
}
struct DeviceGrant: Codable, Sendable { let token: String; let deviceId: String; let member: FamilyMember }
struct InvitationPreview: Codable, Sendable { let memberId: String; let nickname: String; let expiresAtUtc: String }
struct DeviceAuthorization: Codable { let token: String; let deviceId: String; let memberId: String; let server: String }
struct FamilyLocation: Codable, Identifiable, Sendable { let id: String; let memberId: String; let name: String; let equipmentIds: String }
struct SessionExercise: Codable, Identifiable, Sendable {
    let id: String
    let name: String
    let equipmentId: String
    let muscle: String
    let targetType: String
    let instructions: String
    let postureTip: String
    let imageUrl: String?
    let videoUrl: String?
    let sets: Int
    let target: Int
    let restSeconds: Int
}
struct SessionDay: Codable, Identifiable, Sendable {
    let id: String
    let programId: String
    let date: String
    let title: String
    let locationId: String?
    let locationName: String
    let durationMinutes: Int
    let isRest: Bool
    let exercises: [SessionExercise]
    var calendarDate: Date { FamilyDates.day(date) ?? .distantPast }
    var steps: [WorkoutStep] { exercises.flatMap { exercise in (0..<exercise.sets).map { WorkoutStep(exercise: exercise, set: $0 + 1) } } }
}
struct WorkoutStep: Identifiable { let exercise: SessionExercise; let set: Int; var id: String { "\(exercise.id)-\(set)" } }
struct FamilyResult: Codable, Identifiable, Sendable { let id: String; let memberId: String; let programDayId: String; let status: String }
struct FamilySnapshot: Codable, Sendable {
    let serverTimeUtc: String
    let member: FamilyMember
    let members: [FamilyMember]
    let locations: [FamilyLocation]
    let days: [SessionDay]
    let results: [FamilyResult]
}
struct ExercisePerformance: Codable, Sendable {
    let exerciseId: String
    let completedSets: Int
    let repetitions: Int?
    let durationSeconds: Int?
    let weightKg: Double?
    let skipped: Bool
}
struct WorkoutSubmission: Codable, Sendable { let id: String; let programDayId: String; let startedAtUtc: String; let finishedAtUtc: String; let exercises: [ExercisePerformance] }
struct WorkoutCheckpoint: Codable {
    var index = 0
    var remaining = 0
    var phase = "ready"
    var completed: [Int] = []
    var skipped: [Int] = []
    var pausedPhase = "running"
}
@Model final class CachedSnapshot {
    @Attribute(.unique) var memberId: String
    var payload: Data
    var receivedAt: Date
    init(memberId: String, payload: Data) { self.memberId = memberId; self.payload = payload; receivedAt = .now }
}
@Model final class PendingWorkout {
    @Attribute(.unique) var id: String
    var memberId: String
    var payload: Data
    var createdAt: Date
    var lastError: String?
    init(id: String, memberId: String, payload: Data) { self.id = id; self.memberId = memberId; self.payload = payload; createdAt = .now }
}
@Model final class ActiveWorkout {
    @Attribute(.unique) var id: String
    var memberId: String
    var dayPayload: Data
    var startedAtUtc: String
    var checkpointPayload: Data
    init(memberId: String, day: SessionDay) throws {
        id = UUID().uuidString.lowercased(); self.memberId = memberId; dayPayload = try JSONEncoder().encode(day)
        startedAtUtc = FamilyDates.instant(.now); checkpointPayload = try JSONEncoder().encode(WorkoutCheckpoint(remaining: day.steps.first?.exercise.target ?? 0))
    }
}
enum FamilyDates {
    static var paris: Calendar { var calendar = Calendar(identifier: .gregorian); calendar.timeZone = TimeZone(identifier: "Europe/Paris")!; return calendar }
    static func day(_ value: String) -> Date? { let formatter = DateFormatter(); formatter.calendar = paris; formatter.timeZone = paris.timeZone; formatter.locale = Locale(identifier: "en_US_POSIX"); formatter.dateFormat = "yyyy-MM-dd"; return formatter.date(from: value) }
    static func instant(_ value: Date) -> String { let formatter = ISO8601DateFormatter(); formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]; return formatter.string(from: value) }
    static func displayDay(_ value: String) -> String { guard let date = day(value) else { return value }; let formatter = DateFormatter(); formatter.locale = Locale(identifier: "fr_FR"); formatter.timeZone = paris.timeZone; formatter.dateFormat = "EEEE d MMMM"; return formatter.string(from: date) }
}
