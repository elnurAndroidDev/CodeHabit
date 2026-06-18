package com.tabletap.githubcontribsapp.domain.github

import com.tabletap.githubcontribsapp.domain.Contrib

interface ContribsRepository {
    suspend fun getContributes(
        username: String,
        from: String,
        to: String
    ): Result<List<Contrib>>

    /** Returns the login of the user the stored token belongs to. */
    suspend fun getCurrentUser(): Result<String>

    /** Returns success(Unit) iff the given GitHub token is valid. */
    suspend fun validateToken(token: String): Result<Unit>
}
