package com.tabletap.githubcontribsapp.presentation.home

sealed class HomeIntent {
    object LoadData : HomeIntent()
    object Logout : HomeIntent()
    object EditLeetCode : HomeIntent()
}
