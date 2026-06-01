package com.tabletap.githubcontribsapp.presentation.home

import com.tabletap.githubcontribsapp.domain.Contrib

sealed class SourceState {
    object Loading : SourceState()
    data class Success(val contribs: List<Contrib>) : SourceState()
    data class Error(val message: String) : SourceState()
    object NotConfigured : SourceState()
}

data class HomeState(
    val githubUsername: String = "",
    val leetcodeUsername: String? = null,
    val github: SourceState = SourceState.Loading,
    val leetcode: SourceState = SourceState.Loading,
)
