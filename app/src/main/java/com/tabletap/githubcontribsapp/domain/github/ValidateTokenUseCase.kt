package com.tabletap.githubcontribsapp.domain.github

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

class ValidateTokenUseCase @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    suspend operator fun invoke(token: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/user")
                .header("Authorization", "Bearer ${token.trim()}")
                .addHeader("Accept", "application/vnd.github+json")
                .build()
            val response = okHttpClient.newCall(request).execute()
            response.close()
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Invalid token (HTTP ${response.code})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
