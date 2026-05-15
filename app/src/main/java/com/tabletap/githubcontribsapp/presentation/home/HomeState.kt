package com.tabletap.githubcontribsapp.presentation.home

import com.tabletap.githubcontribsapp.domain.Contrib

data class HomeState(
    val isLoading: Boolean = false,
    val username: String = "",
    val contributions: List<Contrib> = emptyList(),
    val error: String? = null
)
