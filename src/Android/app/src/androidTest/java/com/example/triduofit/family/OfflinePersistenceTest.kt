package com.example.triduofit.family

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflinePersistenceTest {
    @Test fun unacknowledgedResultsSurviveClosingAndReopeningDatabase() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "familyfit-offline-test-${java.util.UUID.randomUUID()}.db"
        var db = Room.databaseBuilder(context, FamilyDatabase::class.java, name).build()
        try {
            db.dao().queue(OutboxEntity("result-a", "member-a", "payload-original", 1))
            db.dao().queue(OutboxEntity("result-a", "member-a", "must-not-overwrite", 2))
            db.close()
            db = Room.databaseBuilder(context, FamilyDatabase::class.java, name).build()
            assertEquals("payload-original", db.dao().pending("member-a").single().payload)
            db.dao().acknowledge("result-a", "other-member")
            assertEquals(1, db.dao().pending("member-a").size)
            db.dao().acknowledge("result-a", "member-a")
            assertTrue(db.dao().pending("member-a").isEmpty())
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
