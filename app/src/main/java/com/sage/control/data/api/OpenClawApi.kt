package com.sage.control.data.api

import android.content.Context
import com.sage.control.data.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenClawApi @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }

    private var baseUrl: String = ""
    private var authToken: String = ""

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            level = LogLevel.BODY
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 10000
        }
    }

    private val wsClient = HttpClient(Android) {
        install(WebSockets) {
            pingInterval = 20000
        }
        install(ContentNegotiation) {
            json(json)
        }
    }

    private var webSocketSession: WebSocketSession? = null
    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _events = MutableSharedFlow<WebSocketEvent>()
    val events: SharedFlow<WebSocketEvent> = _events.asSharedFlow()

    fun setCredentials(baseUrl: String, token: String) {
        this.baseUrl = baseUrl.removeSuffix("/")
        this.authToken = token
    }

    fun hasCredentials(): Boolean = baseUrl.isNotEmpty() && authToken.isNotEmpty()

    suspend fun connect() {
        if (!hasCredentials()) return
        
        _connectionStatus.value = ConnectionStatus.Connecting
        
        try {
            val wsUrl = baseUrl.replace("http://", "ws://").replace("https://", "wss://")
            
            webSocketSession = wsClient.webSocketSession {
                url("$wsUrl/ws?token=$authToken")
            }
            
            _connectionStatus.value = ConnectionStatus.Connected
            
            // Start receiving messages
            webSocketSession?.incoming?.consumeAsFlow()?.collect { frame ->
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        try {
                            val event = json.decodeFromString(WebSocketEvent.serializer(), text)
                            _events.emit(event)
                        } catch (e: Exception) {
                            // Try to parse as status message
                            if (text.contains("\"status\"")) {
                                // Handle status updates
                            }
                        }
                    }
                    else -> {}
                }
            }
        } catch (e: Exception) {
            _connectionStatus.value = ConnectionStatus.Error(e.message)
        }
    }

    suspend fun disconnect() {
        webSocketSession?.close()
        webSocketSession = null
        _connectionStatus.value = ConnectionStatus.Disconnected
    }

    suspend fun subscribe(sessionKey: String) {
        val message = mapOf("action" to "subscribe", "sessionKey" to sessionKey)
        webSocketSession?.send(Frame.Text(json.encodeToString(message)))
    }

    suspend fun sendMessage(content: String, sessionKey: String, attachments: List<FileAttachment>? = null) {
        val request = SendMessageRequest(content, sessionKey, attachments)
        val message = mapOf(
            "action" to "send",
            "content" to content,
            "sessionKey" to sessionKey,
            "attachments" to attachments
        )
        webSocketSession?.send(Frame.Text(json.encodeToString(message)))
    }

    suspend fun cancel() {
        val message = mapOf("action" to "cancel")
        webSocketSession?.send(Frame.Text(json.encodeToString(message)))
    }

    suspend fun listSessions(): List<Session> {
        return client.get("$baseUrl/api/sessions") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun getMessages(sessionKey: String): List<Message> {
        return client.get("$baseUrl/api/sessions/$sessionKey/messages") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun createSession(label: String? = null): Session {
        return client.post("$baseUrl/api/sessions") {
            header("Authorization", "Bearer $authToken")
            contentType(ContentType.Application.Json)
            setBody(mapOf("label" to label))
        }.body()
    }

    suspend fun archiveSession(sessionKey: String) {
        client.post("$baseUrl/api/sessions/$sessionKey/archive") {
            header("Authorization", "Bearer $authToken")
        }
    }

    suspend fun deleteSession(sessionKey: String) {
        client.delete("$baseUrl/api/sessions/$sessionKey") {
            header("Authorization", "Bearer $authToken")
        }
    }
}

sealed class ConnectionStatus {
    object Connected : ConnectionStatus()
    object Connecting : ConnectionStatus()
    object Disconnected : ConnectionStatus()
    data class Error(val message: String?) : ConnectionStatus()
}