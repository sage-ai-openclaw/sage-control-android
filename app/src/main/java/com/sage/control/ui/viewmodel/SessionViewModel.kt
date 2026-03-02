package com.sage.control.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sage.control.data.api.ConnectionStatus
import com.sage.control.data.api.OpenClawApi
import com.sage.control.data.model.Session
import com.sage.control.data.model.WebSocketEvent
import com.sage.control.data.repository.SessionRepository
import com.sage.control.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val repository: SessionRepository,
    private val api: OpenClawApi,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()

    private val _filteredSessions = MutableStateFlow<List<Session>>(emptyList())
    val filteredSessions: StateFlow<List<Session>> = _filteredSessions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showArchived = MutableStateFlow(false)
    val showArchived: StateFlow<Boolean> = _showArchived.asStateFlow()

    private val _showTrash = MutableStateFlow(false)
    val showTrash: StateFlow<Boolean> = _showTrash.asStateFlow()

    val connectionStatus: StateFlow<ConnectionStatus> = api.connectionStatus

    init {
        viewModelScope.launch {
            // Collect sessions from DB
            repository.getSessions().collect { sessions ->
                _sessions.value = sessions
                applyFilter()
            }
        }

        viewModelScope.launch {
            // Collect search query changes
            _searchQuery.collect { applyFilter() }
        }

        viewModelScope.launch {
            // Collect WebSocket events
            api.events.collect { event ->
                handleEvent(event)
            }
        }
    }

    fun loadSessions() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.syncSessions()
            _isLoading.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleArchived() {
        _showArchived.value = !_showArchived.value
        applyFilter()
    }

    fun toggleTrash() {
        _showTrash.value = !_showTrash.value
        applyFilter()
    }

    fun createSession(label: String? = null, onSuccess: (String) -> Unit = {}) {
        viewModelScope.launch {
            val session = repository.createSession(label)
            onSuccess(session.key)
        }
    }

    fun archiveSession(sessionKey: String) {
        viewModelScope.launch {
            repository.archiveSession(sessionKey)
            loadSessions()
        }
    }

    fun deleteSession(sessionKey: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionKey)
        }
    }

    fun markAsRead(sessionKey: String) {
        viewModelScope.launch {
            repository.updateUnread(sessionKey, false)
        }
    }

    private fun applyFilter() {
        val query = _searchQuery.value.lowercase()
        var filtered = _sessions.value.filter { it.status != "archived" && it.status != "trash" }

        if (_showArchived.value) {
            filtered = filtered + _sessions.value.filter { it.status == "archived" }
        }
        if (_showTrash.value) {
            filtered = filtered + _sessions.value.filter { it.status == "trash" }
        }

        if (query.isNotEmpty()) {
            filtered = filtered.filter { it.label.lowercase().contains(query) }
        }

        _filteredSessions.value = filtered.sortedByDescending { it.updatedAt }
    }

    private fun handleEvent(event: WebSocketEvent) {
        when (event.type) {
            "session_update" -> {
                loadSessions()
            }
            else -> {}
        }
    }

    val recentCount: Int
        get() = _sessions.value.count { it.status != "archived" && it.status != "trash" }

    val archivedCount: Int
        get() = _sessions.value.count { it.status == "archived" }

    val trashCount: Int
        get() = _sessions.value.count { it.status == "trash" }
}