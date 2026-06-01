package com.tabletap.githubcontribsapp.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tabletap.githubcontribsapp.domain.Contrib
import com.tabletap.githubcontribsapp.domain.github.GetContribsUseCase
import com.tabletap.githubcontribsapp.domain.github.GetCurrentUserUseCase
import com.tabletap.githubcontribsapp.domain.github.TokenRepository
import com.tabletap.githubcontribsapp.domain.leetcode.GetLeetCodeContribsUseCase
import com.tabletap.githubcontribsapp.domain.leetcode.LeetCodeProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getCurrentUser: GetCurrentUserUseCase,
    private val getContribs: GetContribsUseCase,
    private val getLeetCodeContribs: GetLeetCodeContribsUseCase,
    private val tokenRepository: TokenRepository,
    private val leetCodeProfileRepository: LeetCodeProfileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _effect = Channel<HomeEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        onIntent(HomeIntent.LoadData)
    }

    private fun recomputeCombined() {
        val gh = _state.value.github
        val lc = _state.value.leetcode
        val combined = when {
            gh is SourceState.Loading || lc is SourceState.Loading -> SourceState.Loading
            else -> {
                val ghList = (gh as? SourceState.Success)?.contribs ?: emptyList()
                val lcList = (lc as? SourceState.Success)?.contribs ?: emptyList()
                if (ghList.isEmpty() && lcList.isEmpty()) {
                    SourceState.Error("No data available from either source")
                } else {
                    SourceState.Success(mergeContribs(ghList, lcList))
                }
            }
        }
        _state.update { it.copy(combined = combined) }
    }

    private fun mergeContribs(a: List<Contrib>, b: List<Contrib>): List<Contrib> {
        val map = mutableMapOf<String, Int>()
        for (c in a) map[c.date] = (map[c.date] ?: 0) + c.count
        for (c in b) map[c.date] = (map[c.date] ?: 0) + c.count
        val today = LocalDate.now(ZoneOffset.UTC)
        val firstDay = today.minusDays(370L)
        return (0..370).map { offset ->
            val date = firstDay.plusDays(offset.toLong())
            Contrib(date = date.toString(), count = map[date.toString()] ?: 0)
        }
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.LoadData -> loadData()
            HomeIntent.Logout -> logout()
            HomeIntent.EditLeetCode -> viewModelScope.launch {
                _effect.send(HomeEffect.NavigateToLeetCodeAuth)
            }
        }
    }

    private fun loadData() {
        val leetCodeUsername = leetCodeProfileRepository.getUsername()
        _state.update {
            it.copy(
                leetcodeUsername = leetCodeUsername,
                github = SourceState.Loading,
                leetcode = if (leetCodeUsername == null) SourceState.NotConfigured else SourceState.Loading
            )
        }

        viewModelScope.launch {
            val githubJob = async { loadGithub() }
            val leetcodeJob = async {
                if (leetCodeUsername != null) {
                    loadLeetCode(leetCodeUsername)
                } else {
                    recomputeCombined()
                }
            }
            githubJob.await()
            leetcodeJob.await()
        }
    }

    private suspend fun loadGithub() {
        val usernameResult = getCurrentUser()
        if (usernameResult.isFailure) {
            val message = usernameResult.exceptionOrNull()?.message ?: "Could not load GitHub user"
            Timber.w("Failed to get GitHub user: $message")
            _state.update { it.copy(github = SourceState.Error(message)) }
            return
        }
        val username = usernameResult.getOrThrow()
        _state.update { it.copy(githubUsername = username) }

        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val to = now.format(DateTimeFormatter.ISO_INSTANT)
        val from = now.minusYears(1).format(DateTimeFormatter.ISO_INSTANT)

        getContribs(username, from, to)
            .onSuccess { contribs ->
                Timber.d("Loaded ${contribs.size} GitHub contribution days")
                _state.update { it.copy(github = SourceState.Success(contribs)) }
                recomputeCombined()
            }
            .onFailure { e ->
                Timber.w("Failed to load GitHub contributions: ${e.message}")
                _state.update {
                    it.copy(github = SourceState.Error(e.message ?: "Could not load GitHub contributions"))
                }
                recomputeCombined()
            }
    }

    private suspend fun loadLeetCode(username: String) {
        getLeetCodeContribs(username)
            .onSuccess { contribs ->
                Timber.d("Loaded ${contribs.size} LeetCode submission days")
                _state.update { it.copy(leetcode = SourceState.Success(contribs)) }
                recomputeCombined()
            }
            .onFailure { e ->
                Timber.w("Failed to load LeetCode submissions: ${e.message}")
                _state.update {
                    it.copy(leetcode = SourceState.Error(e.message ?: "Could not load LeetCode submissions"))
                }
                recomputeCombined()
            }
    }

    private fun logout() {
        viewModelScope.launch {
            Timber.d("Logging out")
            tokenRepository.clearToken()
            leetCodeProfileRepository.reset()
            _effect.send(HomeEffect.NavigateToAuth)
        }
    }
}
