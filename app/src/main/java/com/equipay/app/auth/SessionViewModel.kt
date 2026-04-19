package com.equipay.app.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SessionState {
    data object Unknown : SessionState()            // только что открыли приложение, ещё проверяем
    data object LoggedOut : SessionState()          // нужно показать Welcome/Login/SignUp
    data class NeedsPin(val email: String) : SessionState()   // залогинен, но PIN не установлен
    data class Authenticated(val userId: String, val email: String) : SessionState()
}

class SessionViewModel(
    val tokenStore: TokenStore,
    val repo: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow<SessionState>(SessionState.Unknown)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    /** Вызывается при старте приложения. */
    fun bootstrap() {
        viewModelScope.launch {
            if (!tokenStore.isLoggedIn()) {
                _state.value = SessionState.LoggedOut
                return@launch
            }
            val email = tokenStore.getEmail()
            // Если есть refresh — пробуем его обновить
            val ok = repo.tryRestoreSession()
            if (!ok) {
                _state.value = SessionState.LoggedOut
                return@launch
            }
            val userId = tokenStore.getUserId() ?: ""
            _state.value = if (!tokenStore.hasPin())
                SessionState.NeedsPin(email ?: tokenStore.getEmail() ?: "")
            else
                SessionState.Authenticated(userId, tokenStore.getEmail() ?: "")
        }
    }

    fun onAuthenticated(userId: String, email: String, hasPin: Boolean) {
        _state.value = if (!hasPin) SessionState.NeedsPin(email)
        else SessionState.Authenticated(userId, email)
    }

    fun onPinSet() {
        val email = tokenStore.getEmail() ?: ""
        val userId = tokenStore.getUserId() ?: ""
        _state.value = SessionState.Authenticated(userId, email)
    }

    fun logout() {
        viewModelScope.launch {
            repo.logout()
            _state.value = SessionState.LoggedOut
        }
    }
}
