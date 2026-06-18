package com.tabletap.githubcontribsapp.presentation.home

sealed class HomeEffect {
    object NavigateToAuth : HomeEffect()
    object NavigateToLeetCodeAuth : HomeEffect()
}
