package com.tabletap.githubcontribsapp.domain.github

import com.tabletap.githubcontribsapp.domain.Contrib

interface ContribsRepository {
    suspend fun getContributes(
        username: String,
        from: String,
        to: String
    ): Result<List<Contrib>>
}
