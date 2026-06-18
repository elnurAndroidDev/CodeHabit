package com.tabletap.githubcontribsapp.domain.github

interface TokenRepository {
    fun saveToken(token: String)
    fun getToken(): String?
    fun clearToken()
}
