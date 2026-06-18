package com.tabletap.githubcontribsapp.domain.leetcode

interface LeetCodeProfileRepository {
    fun saveUsername(username: String)
    fun getUsername(): String?
    fun clearUsername()

    fun isFirstRunComplete(): Boolean
    fun markFirstRunComplete()

    /** Clears both the stored username and the first-run flag (called on logout). */
    fun reset()
}
