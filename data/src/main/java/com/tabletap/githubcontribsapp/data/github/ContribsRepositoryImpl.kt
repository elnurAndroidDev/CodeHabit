package com.tabletap.githubcontribsapp.data.github

import com.apollographql.apollo.ApolloClient
import com.tabletap.GetContributionsQuery
import com.tabletap.githubcontribsapp.domain.Contrib
import com.tabletap.githubcontribsapp.domain.github.ContribsRepository
import com.tabletap.githubcontribsapp.domain.github.TokenRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject

class ContribsRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient,
    private val okHttpClient: OkHttpClient,
    private val tokenRepository: TokenRepository
) : ContribsRepository {

    override suspend fun getContributes(
        username: String,
        from: String,
        to: String
    ): Result<List<Contrib>> {
        return try {
            val token = tokenRepository.getToken().orEmpty()
            val response = apolloClient.query(
                GetContributionsQuery(login = username, from = from, to = to)
            ).addHttpHeader("Authorization", "Bearer $token")
             .execute()

            if (response.data == null) {
                Result.failure(Exception("No data"))
            } else {
                val contributes = response.data!!.user!!
                    .contributionsCollection.contributionCalendar.weeks.flatMap { week ->
                        week.contributionDays.map { day ->
                            Contrib(date = day.date as String, count = day.contributionCount)
                        }
                    }
                Result.success(contributes)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): Result<String> = withContext(Dispatchers.IO) {
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

    override suspend fun validateToken(token: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/user")
                .header("Authorization", "Bearer $token")
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
