package com.sage.control.data.repository

import com.sage.control.data.api.OpenClawApi
import com.sage.control.data.db.*
import com.sage.control.data.model.Message
import com.sage.control.data.model.Session
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val api: OpenClawApi,
    private val db: SageDatabase
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun getSessions(): Flow<List<Session>> {
        return db.sessionDao().getAll().map { entities ->
            entities.map { it.toModel() }
        }
    }

    fun getSessionsByStatus(status: String): Flow<List<Session>> {
        return db.sessionDao().getByStatus(status).map { entities ->
            entities.map { it.toModel() }
        }
    }

    fun getMessages(sessionKey: String): Flow<List<Message>> {
        return db.messageDao().getBySession(sessionKey).map { entities ->
            entities.map { it.toModel(json) }
        }
    }

    suspend fun syncSessions() {
        try {
            val sessions = api.listSessions()
            db.sessionDao().insertAll(sessions.map { it.toEntity() })
        } catch (e: Exception) {
            // Keep local data on error
        }
    }

    suspend fun syncMessages(sessionKey: String) {
        try {
            val messages = api.getMessages(sessionKey)
            db.messageDao().insertAll(messages.map { it.toEntity(json) })
        } catch (e: Exception) {
            // Keep local data on error
        }
    }

    suspend fun createSession(label: String? = null): Session {
        val session = api.createSession(label)
        db.sessionDao().insert(session.toEntity())
        return session
    }

    suspend fun archiveSession(sessionKey: String) {
        api.archiveSession(sessionKey)
    }

    suspend fun deleteSession(sessionKey: String) {
        api.deleteSession(sessionKey)
        db.sessionDao().delete(sessionKey)
        db.messageDao().deleteBySession(sessionKey)
    }

    suspend fun updateUnread(sessionKey: String, unread: Boolean) {
        db.sessionDao().updateUnread(sessionKey, unread)
    }

    suspend fun addMessage(message: Message) {
        db.messageDao().insert(message.toEntity(json))
    }

    // Mappers
    private fun SessionEntity.toModel() = Session(
        key = key,
        label = label,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
        unread = unread,
        model = model
    )

    private fun Session.toEntity() = SessionEntity(
        key = key,
        label = label,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
        unread = unread,
        model = model
    )

    private fun MessageEntity.toModel(json: Json): Message {
        return Message(
            id = id,
            sessionKey = sessionKey,
            role = role,
            content = content,
            timestamp = timestamp,
            pending = pending,
            attachments = attachmentsJson?.let { json.decodeFromString(it) },
            operations = operationsJson?.let { json.decodeFromString(it) },
            isSync = isSync,
            isThinking = isThinking,
            cancelled = cancelled
        )
    }

    private fun Message.toEntity(json: Json): MessageEntity {
        return MessageEntity(
            id = id,
            sessionKey = sessionKey ?: "",
            role = role,
            content = content,
            timestamp = timestamp,
            pending = pending,
            attachmentsJson = attachments?.let { json.encodeToString(it) },
            operationsJson = operations?.let { json.encodeToString(it) },
            isSync = isSync,
            isThinking = isThinking,
            cancelled = cancelled
        )
    }
}