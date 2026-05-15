package com.tabletap.githubcontribsapp.domain

interface ContribsRepository {
    suspend fun getContributes(
        username: String,
        from: String,
        to: String
    ): Result<List<Contrib>>
}