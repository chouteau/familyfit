package com.example.triduofit.family

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "snapshot") data class SnapshotEntity(@PrimaryKey val memberId: String, val payload: String, val receivedAt: Long)
@Entity(tableName = "outbox") data class OutboxEntity(@PrimaryKey val id: String, val memberId: String, val payload: String, val createdAt: Long, val lastError: String? = null)
@Entity(tableName = "active_workout") data class ActiveWorkoutEntity(@PrimaryKey val key: Int = 1, val id: String, val memberId: String, val sessionJson: String, val dayJson: String?, val startedAt: String, val checkpointJson: String)

@Dao interface FamilyDao {
    @Query("SELECT * FROM snapshot WHERE memberId = :memberId") suspend fun snapshot(memberId: String): SnapshotEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveSnapshot(snapshot: SnapshotEntity)
    @Query("SELECT * FROM outbox WHERE memberId = :memberId ORDER BY createdAt") suspend fun pending(memberId: String): List<OutboxEntity>
    @Query("SELECT COUNT(*) FROM outbox") fun pendingCount(): Flow<Int>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun queue(item: OutboxEntity)
    @Query("DELETE FROM outbox WHERE id = :id AND memberId = :memberId") suspend fun acknowledge(id: String, memberId: String)
    @Query("UPDATE outbox SET lastError = :message WHERE id = :id") suspend fun fail(id: String, message: String)
    @Query("SELECT * FROM active_workout WHERE `key` = 1") suspend fun active(): ActiveWorkoutEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveActive(active: ActiveWorkoutEntity)
    @Query("DELETE FROM active_workout WHERE id = :id") suspend fun deleteActive(id: String)
}
@Database(entities = [SnapshotEntity::class, OutboxEntity::class, ActiveWorkoutEntity::class], version = 1, exportSchema = true)
abstract class FamilyDatabase : RoomDatabase() {
    abstract fun dao(): FamilyDao
    companion object {
        @Volatile private var instance: FamilyDatabase? = null
        fun get(context: Context) = instance ?: synchronized(this) { instance ?: Room.databaseBuilder(context.applicationContext, FamilyDatabase::class.java, "familyfit.db").build().also { instance = it } }
    }
}
