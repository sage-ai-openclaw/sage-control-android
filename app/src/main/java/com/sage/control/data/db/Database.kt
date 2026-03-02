package com.sage.control.data.db

import androidx.room.*
import com.sage.control.data.model.Message
import com.sage.control.data.model.Session
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val key: String,
    val label: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val unread: Boolean,
    val model: String?
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val sessionKey: String,
    val role: String,
    val content: String,
    val timestamp: Long,
    val pending: Boolean,
    val attachmentsJson: String?,
    val operationsJson: String?,
    val isSync: Boolean,
    val isThinking: Boolean,
    val cancelled: Boolean
)

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE status = :status ORDER BY updatedAt DESC")
    fun getByStatus(status: String): Flow<List<SessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<SessionEntity>)

    @Update
    suspend fun update(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE key = :key")
    suspend fun delete(key: String)

    @Query("UPDATE sessions SET unread = :unread WHERE key = :key")
    suspend fun updateUnread(key: String, unread: Boolean)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE sessionKey = :sessionKey ORDER BY timestamp ASC")
    fun getBySession(sessionKey: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE sessionKey = :sessionKey")
    suspend fun deleteBySession(sessionKey: String)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun delete(id: String)
}

@Database(entities = [SessionEntity::class, MessageEntity::class], version = 1)
abstract class SageDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun messageDao(): MessageDao
}