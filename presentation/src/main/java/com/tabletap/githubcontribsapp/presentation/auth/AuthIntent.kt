package com.tabletap.githubcontribsapp.presentation.auth

sealed class AuthIntent {
    data class Login(val token: String) : AuthIntent()
}
