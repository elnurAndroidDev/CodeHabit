package com.tabletap.githubcontribsapp.presentation.leetcode_auth

sealed class LeetCodeAuthIntent {
    data class Submit(val username: String) : LeetCodeAuthIntent()
    object Skip : LeetCodeAuthIntent()
}
