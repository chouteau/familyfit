import XCTest
import SwiftData
@testable import FamilyFit

final class ContractTests: XCTestCase {
    private func exampleDay() throws -> SessionDay {
        let payload = #"{"id":"00000000-0000-0000-0000-000000000001","programId":"00000000-0000-0000-0000-000000000002","date":"2026-11-09","title":"Gainage","locationId":null,"locationName":"Maison","durationMinutes":20,"isRest":false,"exercises":[{"id":"plank","name":"Gainage","equipmentId":"BODYWEIGHT","muscle":"Core","targetType":"time","instructions":"Garde le dos droit.","postureTip":"Respire.","imageUrl":null,"videoUrl":null,"sets":3,"target":40,"restSeconds":20}],"futureServerField":true}"#
        return try JSONDecoder().decode(SessionDay.self, from: Data(payload.utf8))
    }
    func testServerContractIgnoresFutureFieldsAndExpandsAllSets() throws {
        let day = try exampleDay()
        XCTAssertEqual(day.steps.count, 3)
        XCTAssertEqual(day.steps.map(\.set), [1, 2, 3])
        XCTAssertEqual(Set(day.steps.map(\.id)).count, 3)
    }
    @MainActor func testInterruptedWorkoutAndUnacknowledgedResultSurviveNewContext() throws {
        let schema = Schema([ActiveWorkout.self, PendingWorkout.self])
        let container = try ModelContainer(for: schema, configurations: [ModelConfiguration(schema: schema, isStoredInMemoryOnly: true)])
        let context = ModelContext(container); context.autosaveEnabled = false
        let active = try ActiveWorkout(memberId: "member-a", day: exampleDay())
        var checkpoint = WorkoutCheckpoint(index: 1, remaining: 23, phase: "paused", completed: [0])
        checkpoint.skipped = []
        active.checkpointPayload = try JSONEncoder().encode(checkpoint)
        context.insert(active); context.insert(PendingWorkout(id: "result-a", memberId: "member-a", payload: Data("{}".utf8))); try context.save()
        let reopened = ModelContext(container)
        let restored = try XCTUnwrap(reopened.fetch(FetchDescriptor<ActiveWorkout>()).first)
        let progress = try JSONDecoder().decode(WorkoutCheckpoint.self, from: restored.checkpointPayload)
        XCTAssertEqual(progress.remaining, 23); XCTAssertEqual(progress.completed, [0]); XCTAssertEqual(progress.phase, "paused")
        XCTAssertEqual(try reopened.fetchCount(FetchDescriptor<PendingWorkout>()), 1)
    }
    func testCalendarUsesParisDateWithoutConvertingToLocalDeviceZone() throws {
        let day = try exampleDay()
        XCTAssertEqual(FamilyDates.paris.component(.weekday, from: day.calendarDate), 2)
        XCTAssertEqual(FamilyDates.paris.component(.day, from: day.calendarDate), 9)
    }
}
