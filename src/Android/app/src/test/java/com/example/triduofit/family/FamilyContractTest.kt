package com.example.triduofit.family

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class FamilyContractTest {
    @Test fun publishedDayKeepsEveryPrescribedSetAndIgnoresFutureServerFields() {
        val day = Json { ignoreUnknownKeys = true }.decodeFromString<FamilyDay>("""{"id":"day-a","programId":"program-a","date":"2026-11-09","title":"Séance personnelle","locationName":"Ma salle","isRest":false,"durationMinutes":20,"futureServerField":true,"exercises":[{"id":"plank","name":"Gainage","equipmentId":"BODYWEIGHT","targetType":"time","sets":3,"target":40,"restSeconds":20}]}""")
        val session = day.nativeSession()
        assertEquals("Séance personnelle", session.title)
        assertEquals("day-a", session.programDayId)
        assertEquals(3, session.exercises.size)
        assertEquals(listOf("plank", "plank", "plank"), session.exercises.map { it.id })
        assertTrue(session.exercises[2].name.contains("3/3"))
        assertEquals(40, session.exercises[2].targetCount)
    }
    @Test fun checkpointRetainsSkippedAndCompletedSetsSeparatelyAcrossSerialization() {
        val checkpoint = WorkoutCheckpoint(2, 23, "PAUSED", listOf(0), listOf(1))
        val saved = Json.encodeToString(WorkoutCheckpoint.serializer(), checkpoint)
        val restored = Json.decodeFromString<WorkoutCheckpoint>(saved)
        assertEquals(23, restored.secondsRemaining)
        assertEquals(listOf(0), restored.completedIndices)
        assertEquals(listOf(1), restored.skippedIndices)
        assertEquals("PAUSED", restored.stepState)
    }
}
