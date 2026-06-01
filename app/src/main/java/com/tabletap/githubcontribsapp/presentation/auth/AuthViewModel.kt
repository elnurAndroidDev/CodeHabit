package com.tabletap.githubcontribsapp.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tabletap.githubcontribsapp.domain.github.TokenRepository
import com.tabletap.githubcontribsapp.domain.github.ValidateTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val tokenRepository: TokenRepository,
    private val validateToken: ValidateTokenUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _effect = Channel<AuthEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.Login -> login(intent.token)
        }
    }

    private fun login(token: String) {
        viewModelScope.launch {
            Timber.d("Login attempt")
            _state.update { it.copy(isLoading = true, error = null) }
            validateToken(token)
                .onSuccess {
                    Timber.d("Token valid, saving and navigating to LeetCode setup")
                    tokenRepository.saveToken(token.trim())
                    _effect.send(AuthEffect.NavigateToLeetCodeAuth)
                }
                .onFailure { e ->
                    Timber.w("Token validation failed: ${e.message}")
                    _state.update { it.copy(isLoading = false, error = e.message ?: "Something went wrong") }
                }
        }
    }
}
