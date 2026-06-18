package com.tabletap.githubcontribsapp.presentation.leetcode_auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tabletap.githubcontribsapp.domain.leetcode.LeetCodeProfileRepository
import com.tabletap.githubcontribsapp.domain.leetcode.LeetCodeRepository
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
class LeetCodeAuthViewModel @Inject constructor(
    private val profileRepository: LeetCodeProfileRepository,
    private val leetCodeRepository: LeetCodeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LeetCodeAuthState())
    val state: StateFlow<LeetCodeAuthState> = _state.asStateFlow()

    private val _effect = Channel<LeetCodeAuthEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: LeetCodeAuthIntent) {
        when (intent) {
            is LeetCodeAuthIntent.Submit -> submit(intent.username)
            LeetCodeAuthIntent.Skip -> skip()
        }
    }

    private fun submit(username: String) {
        val trimmed = username.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            Timber.d("Validating LeetCode user: $trimmed")
            _state.update { it.copy(isLoading = true, error = null) }
            leetCodeRepository.getSubmissions(trimmed)
                .onSuccess {
                    profileRepository.saveUsername(trimmed)
                    profileRepository.markFirstRunComplete()
                    _effect.send(LeetCodeAuthEffect.NavigateToHome)
                }
                .onFailure { e ->
                    Timber.w("LeetCode validation failed: ${e.message}")
                    _state.update {
                        it.copy(isLoading = false, error = e.message ?: "Could not verify username")
                    }
                }
        }
    }

    private fun skip() {
        viewModelScope.launch {
            profileRepository.markFirstRunComplete()
            _effect.send(LeetCodeAuthEffect.NavigateToHome)
        }
    }
}