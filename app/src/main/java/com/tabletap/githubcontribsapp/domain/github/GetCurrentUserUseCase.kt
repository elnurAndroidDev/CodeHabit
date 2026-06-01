package com.tabletap.githubcontribsapp.domain.github

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val tokenRepository: TokenRepository
) {
    suspend operator fun invoke(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val token = tokenRepository.getToken()
                ?: return@withContext Result.failure(Exception("No token stored"))
            val request = Request.Builder()
                .url("https://api.github.com/user")
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github+json")
                .build()
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}"))
            }
            val login = JSONObject(response.body!!.string()).getString("login")
            Result.success(login)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
