package com.sage.control.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sage.control.data.api.OpenClawApi
import com.sage.control.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val api: OpenClawApi
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val isLoggedIn: StateFlow<Boolean> = settingsRepository.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val serverUrl: StateFlow<String> = settingsRepository.serverUrl
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    val authToken: StateFlow<String> = settingsRepository.authToken
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    fun login(serverUrl: String, authToken: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Validate and normalize URL
                val normalizedUrl = serverUrl.trim().removeSuffix("/")
                val finalUrl = if (normalizedUrl.startsWith("http")) {
                    normalizedUrl
                } else {
                    "http://$normalizedUrl"
                }

                // Test connection
                api.setCredentials(finalUrl, authToken)
                api.connect()

                // Save credentials
                settingsRepository.saveCredentials(finalUrl, authToken)
                
                onSuccess()
            } catch (e: Exception) {
                _error.value = e.message ?: "Connection failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            api.disconnect()
            settingsRepository.clearCredentials()
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun autoConnect() {
        viewModelScope.launch {
            val url = serverUrl.value
            val token = authToken.value
            if (url.isNotEmpty() && token.isNotEmpty()) {
                api.setCredentials(url, token)
                try {
                    api.connect()
                } catch (e: Exception) {
                    // Silent fail on auto-connect
                }
            }
        }
    }
}