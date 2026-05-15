package com.tabletap.githubcontribsapp.domain

interface TokenRepository {
    fun saveToken(token: String)
    fun getToken(): String?
    fun clearToken()
}
