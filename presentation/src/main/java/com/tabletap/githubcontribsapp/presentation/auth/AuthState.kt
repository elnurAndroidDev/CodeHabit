package com.tabletap.githubcontribsapp.presentation.auth

data class AuthState(
    val isLoading: Boolean = false,
    val error: String? = null
)
