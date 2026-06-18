package com.tabletap.githubcontribsapp.presentation.auth

sealed class AuthEffect {
    object NavigateToLeetCodeAuth : AuthEffect()
}
