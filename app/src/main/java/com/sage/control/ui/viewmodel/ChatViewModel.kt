package com.sage.control.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sage.control.data.api.ConnectionStatus
import com.sage.control.data.api.OpenClawApi
import com.sage.control.data.model.Message
import com.sage.control.data.model.OperationEvent
import com.sage.control.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: SessionRepository,
    private val api: OpenClawApi
) : ViewModel() {

    private val _sessionKey = MutableStateFlow<String?>(null)

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _streamingContent = MutableStateFlow("")
    val streamingContent: StateFlow<String> = _streamingContent.asStateFlow()

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _inlineOperations = MutableStateFlow<List<OperationEvent>>(emptyList())
    val inlineOperations: StateFlow<List<OperationEvent>> = _inlineOperations.asStateFlow()

    private val _draft = MutableStateFlow("")
    val draft: StateFlow<String> = _draft.asStateFlow()

    private var streamingId: String? = null

    val isStreaming: Boolean
        get() = _streamingContent.value.isNotEmpty()

    init {
        viewModelScope.launch {
            _sessionKey.filterNotNull().flatMapLatest { key ->
                repository.getMessages(key)
            }.collect { messages ->
                _messages.value = messages
            }
        }

        viewModelScope.launch {
            api.events.collect { event ->
                handleEvent(event)
            }
        }

        viewModelScope.launch {
            api.connectionStatus.collect { status ->
                _connectionStatus.value = status
            }
        }
    }

    fun setSession(sessionKey: String) {
        if (_sessionKey.value == sessionKey) return
        
        _sessionKey.value = sessionKey
        _streamingContent.value = ""
        _isThinking.value = false
        _inlineOperations.value = emptyList()
        
        viewModelScope.launch {
            _isLoading.value = true
            repository.syncMessages(sessionKey)
            api.subscribe(sessionKey)
            _isLoading.value = false
        }
    }

    fun sendMessage(content: String) {
        val key = _sessionKey.value ?: return
        if (content.isBlank()) return

        viewModelScope.launch {
            // Cancel any ongoing stream
            if (_streamingContent.value.isNotEmpty() || _isThinking.value) {
                api.cancel()
                val partial = _streamingContent.value.trim()
                if (partial.isNotEmpty()) {
                    val interrupted = Message(
                        id = "partial-${System.currentTimeMillis()}",
                        sessionKey = key,
                        role = "assistant",
                        content = "$partial\n\n*(INTERRUPTED)*",
                        timestamp = System.currentTimeMillis(),
                        cancelled = true
                    )
                    repository.addMessage(interrupted)
                }
            }

            _streamingContent.value = ""
            _isThinking.value = false
            _draft.value = ""

            val userMessage = Message(
                id = "msg-${System.currentTimeMillis()}",
                sessionKey = key,
                role = "user",
                content = content,
                timestamp = System.currentTimeMillis(),
                pending = false
            )
            repository.addMessage(userMessage)

            api.sendMessage(content, key)
            _isThinking.value = true
        }
    }

    fun cancel() {
        viewModelScope.launch {
            api.cancel()
            _isThinking.value = false
            _streamingContent.value = ""
        }
    }

    fun setDraft(draft: String) {
        _draft.value = draft
    }

    private fun handleEvent(event: WebSocketEvent) {
        val key = _sessionKey.value ?: return

        when (event.type) {
            "new_message" -> {
                event.message?.let { msg ->
                    _streamingContent.value = ""
                    _isThinking.value = false
                    streamingId = null
                    _inlineOperations.value = emptyList()
                    
                    viewModelScope.launch {
                        repository.addMessage(msg)
                    }
                }
            }
            "stream" -> {
                event.delta?.let { delta ->
                    _isThinking.value = false
                    if (streamingId != event.messageId) {
                        streamingId = event.messageId
                        _streamingContent.value = delta
                    } else {
                        _streamingContent.value += delta
                    }
                }
            }
            "thinking" -> {
                event.thinking?.let { thinking ->
                    _isThinking.value = thinking
                }
            }
            "operation" -> {
                event.operation?.let { op ->
                    val current = _inlineOperations.value.toMutableList()
                    val idx = current.indexOfFirst { it.id == op.id }
                    if (idx >= 0) {
                        current[idx] = op
                    } else {
                        current.add(op)
                    }
                    _inlineOperations.value = current
                }
            }
            "status" -> {
                // Handle connection status
            }
        }
    }

    fun loadMoreMessages() {
        // Pagination if needed
    }

    override fun onCleared() {
        super.onCleared()
        // Don't disconnect, keep WebSocket alive for background notifications
    }
}