package com.equipay.app.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.equipay.app.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val signedIn: Boolean = false
)

class AuthViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AuthRepository.get(app)

    private val _state = MutableStateFlow(AuthUiState(signedIn = repo.isSignedIn()))
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    val isSignedIn: Boolean get() = repo.isSignedIn()
    val hasPin: Boolean get() = repo.hasPin()
    val hasSeenOnboarding: Boolean get() = repo.hasSeenOnboarding()
    val isBiometricEnabled: Boolean get() = repo.isBiometricEnabled()
    val currentUser get() = repo.currentUser()

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun signUp(name: String, email: String, password: String, onDone: () -> Unit) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repo.signUp(name, email, password)
                .onSuccess {
                    _state.update { it.copy(loading = false, signedIn = true) }
                    onDone()
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = e.message ?: "Sign up failed") }
                }
        }
    }

    fun signIn(email: String, password: String, onDone: () -> Unit) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repo.signIn(email, password)
                .onSuccess {
                    _state.update { it.copy(loading = false, signedIn = true) }
                    onDone()
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = e.message ?: "Sign in failed") }
                }
        }
    }

    fun signInWithGoogle(onDone: () -> Unit) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repo.signInWithGoogle().onSuccess {
                _state.update { it.copy(loading = false, signedIn = true) }
                onDone()
            }.onFailure { e ->
                _state.update { it.copy(loading = false, error = e.message ?: "Google sign-in failed") }
            }
        }
    }

    fun setPin(pin: String) = repo.savePin(pin)
    fun verifyPin(pin: String): Boolean = repo.checkPin(pin)
    fun setBiometric(enabled: Boolean) = repo.setBiometricEnabled(enabled)
    fun markOnboardingSeen() = repo.setOnboardingSeen()

    fun signOut() {
        repo.signOut()
        _state.update { AuthUiState(signedIn = false) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                AuthViewModel(app)
            }
        }
    }
}
