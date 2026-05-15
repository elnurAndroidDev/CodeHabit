package com.tabletap.githubcontribsapp.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tabletap.githubcontribsapp.domain.GetContribsUseCase
import com.tabletap.githubcontribsapp.domain.GetCurrentUserUseCase
import com.tabletap.githubcontribsapp.domain.TokenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getCurrentUser: GetCurrentUserUseCase,
    private val getContribs: GetContribsUseCase,
    private val tokenRepository: TokenRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _effect = Channel<HomeEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        onIntent(HomeIntent.LoadData)
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.LoadData -> loadData()
            HomeIntent.Logout -> logout()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val usernameResult = getCurrentUser()
            if (usernameResult.isFailure) {
                val message = usernameResult.exceptionOrNull()?.message
                Timber.w("Failed to get user: $message")
                _state.update { it.copy(isLoading = false, error = message) }
                return@launch
            }
            val username = usernameResult.getOrThrow()
            Timber.d("Loaded user: $username")

            val now = ZonedDateTime.now(ZoneOffset.UTC)
            val to = now.format(DateTimeFormatter.ISO_INSTANT)
            val from = now.minusYears(1).format(DateTimeFormatter.ISO_INSTANT)

            getContribs(username, from, to)
                .onSuccess { contribs ->
                    Timber.d("Loaded ${contribs.size} contribution days")
                    _state.update {
                        it.copy(isLoading = false, username = username, contributions = contribs)
                    }
                }
                .onFailure { e ->
                    Timber.w("Failed to load contributions: ${e.message}")
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            Timber.d("Logging out")
            tokenRepository.clearToken()
            _effect.send(HomeEffect.NavigateToAuth)
        }
    }
}
