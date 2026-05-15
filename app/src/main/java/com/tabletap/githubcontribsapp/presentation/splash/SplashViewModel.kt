package com.tabletap.githubcontribsapp.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tabletap.githubcontribsapp.domain.TokenRepository
import com.tabletap.githubcontribsapp.domain.ValidateTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val tokenRepository: TokenRepository,
    private val validateToken: ValidateTokenUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<SplashState>(SplashState.Loading)
    val state: StateFlow<SplashState> = _state.asStateFlow()

    init {
        checkToken()
    }

    private fun checkToken() {
        viewModelScope.launch {
            val token = tokenRepository.getToken()
            if (token == null) {
                Timber.d("No stored token → navigating to auth")
                _state.value = SplashState.NavigateToAuth
                return@launch
            }
            Timber.d("Stored token found, validating…")
            validateToken(token)
                .onSuccess {
                    Timber.d("Token valid → navigating to home")
                    _state.value = SplashState.NavigateToHome
                }
                .onFailure { e ->
                    Timber.w("Token invalid (${e.message}), clearing → navigating to auth")
                    tokenRepository.clearToken()
                    _state.value = SplashState.NavigateToAuth
                }
        }
    }
}
