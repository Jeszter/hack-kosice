package com.equipay.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.equipay.app.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConnectBankUiState(
    val isLoading: Boolean = false,
    val launchUrl: String? = null,
    val error: String? = null
)

class ConnectBankViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ConnectBankUiState())
    val uiState: StateFlow<ConnectBankUiState> = _uiState.asStateFlow()

    fun connectTatra() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.value = ConnectBankUiState(isLoading = true)

            runCatching {
                ApiClient.authApi.startTatraConnect()
            }.onSuccess { response ->
                if (response.isSuccessful) {
                    val url = response.body()?.authorizeUrl
                    if (url.isNullOrBlank()) {
                        _uiState.value = ConnectBankUiState(
                            isLoading = false,
                            error = "Сервер не вернул ссылку авторизации"
                        )
                    } else {
                        _uiState.value = ConnectBankUiState(
                            isLoading = false,
                            launchUrl = url
                        )
                    }
                } else {
                    val serverError = runCatching {
                        response.errorBody()?.string()
                    }.getOrNull()

                    _uiState.value = ConnectBankUiState(
                        isLoading = false,
                        error = "Ошибка ${response.code()}: ${serverError ?: "Не удалось начать подключение Tatra banka"}"
                    )
                }
            }.onFailure { throwable ->
                _uiState.value = ConnectBankUiState(
                    isLoading = false,
                    error = throwable.message ?: "Ошибка подключения"
                )
            }
        }
    }

    fun onLaunchConsumed() {
        _uiState.value = _uiState.value.copy(launchUrl = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}