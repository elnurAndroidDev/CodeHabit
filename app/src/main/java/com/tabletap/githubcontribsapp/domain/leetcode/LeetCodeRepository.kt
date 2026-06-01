package com.tabletap.githubcontribsapp.domain.leetcode

import com.tabletap.githubcontribsapp.domain.Contrib

interface LeetCodeRepository {
    /**
     * Fetches the last 53 weeks of submissions ending today, padded with zeros
     * for days without activity. Length is always a multiple of 7.
     */
    suspend fun getSubmissions(username: String): Result<List<Contrib>>

    /** Returns success(Unit) iff a public LeetCode user with this username exists. */
    suspend fun userExists(username: String): Result<Unit>
}
