package com.tabletap.githubcontribsapp.presentation.splash

sealed class SplashState {
    object Loading : SplashState()
    object NavigateToHome : SplashState()
    object NavigateToAuth : SplashState()
}
