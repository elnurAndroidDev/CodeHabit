package com.tabletap.githubcontribsapp.data.leetcode

import com.tabletap.githubcontribsapp.domain.Contrib
import com.tabletap.githubcontribsapp.domain.leetcode.LeetCodeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

private const val LEETCODE_GRAPHQL_URL = "https://leetcode.com/graphql"
private const val WEEKS_TO_SHOW = 53
private const val DAYS_TO_SHOW = WEEKS_TO_SHOW * 7

private val JSON = "application/json; charset=utf-8".toMediaType()

private const val USER_CALENDAR_QUERY = """
query userCalendar(${'$'}username: String!, ${'$'}year: Int) {
  matchedUser(username: ${'$'}username) {
    userCalendar(year: ${'$'}year) {
      submissionCalendar
    }
  }
}
"""

private const val MATCHED_USER_QUERY = """
query matchedUser(${'$'}username: String!) {
  matchedUser(username: ${'$'}username) { username }
}
"""

class LeetCodeRepositoryImpl @Inject constructor(
    private val okHttpClient: OkHttpClient
) : LeetCodeRepository {

    override suspend fun getSubmissions(username: String): Result<List<Contrib>> =
        withContext(Dispatchers.IO) {
            try {
                val today = LocalDate.now(ZoneOffset.UTC)
                val firstDay = today.minusDays((DAYS_TO_SHOW - 1).toLong())

                val years = (firstDay.year..today.year).toList()
                val counts = mutableMapOf<LocalDate, Int>()
                for (year in years) {
                    val yearMap = fetchYearCalendar(username, year).getOrElse {
                        return@withContext Result.failure(it)
                    }
                    counts.putAll(yearMap)
                }

                val series = (0 until DAYS_TO_SHOW).map { offset ->
                    val date = firstDay.plusDays(offset.toLong())
                    Contrib(
                        date = date.toString(),
                        count = counts[date] ?: 0
                    )
                }
                Result.success(series)
            } catch (e: Exception) {
                Timber.w("LeetCode getSubmissions failed: ${e.message}")
                Result.failure(e)
            }
        }

    override suspend fun userExists(username: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val variables = JSONObject().put("username", username)
                val data = postGraphQL(MATCHED_USER_QUERY, variables)
                val matched = data.optJSONObject("matchedUser")
                if (matched != null && !matched.isNull("username")) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("LeetCode user not found"))
                }
            } catch (e: Exception) {
                Timber.w("LeetCode userExists failed: ${e.message}")
                Result.failure(e)
            }
        }

    private fun fetchYearCalendar(username: String, year: Int): Result<Map<LocalDate, Int>> =
        try {
            val variables = JSONObject()
                .put("username", username)
                .put("year", year)
            val data = postGraphQL(USER_CALENDAR_QUERY, variables)
            val matched = data.optJSONObject("matchedUser")
                ?: return Result.failure(Exception("LeetCode user not found"))
            val calendar = matched.optJSONObject("userCalendar")
            val rawSubmissionCalendar = calendar?.optString("submissionCalendar", "{}") ?: "{}"
            val map = mutableMapOf<LocalDate, Int>()
            val json = JSONObject(rawSubmissionCalendar)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val epochSeconds = key.toLongOrNull() ?: continue
                val date = LocalDate.ofEpochDay(epochSeconds / 86_400L)
                map[date] = json.optInt(key, 0)
            }
            Result.success(map)
        } catch (e: Exception) {
            Result.failure(e)
        }

    private fun postGraphQL(query: String, variables: JSONObject): JSONObject {
        val body = JSONObject()
            .put("query", query)
            .put("variables", variables)
            .toString()
            .toRequestBody(JSON)
        val request = Request.Builder()
            .url(LEETCODE_GRAPHQL_URL)
            .post(body)
            .header("Content-Type", "application/json")
            .header("Referer", "https://leetcode.com")
            .header("User-Agent", "GithubContribsApp/1.0")
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("LeetCode HTTP ${response.code}")
            }
            val raw = response.body.string()
            val payload = JSONObject(raw)
            if (payload.has("errors")) {
                throw Exception("LeetCode GraphQL error: ${payload.getJSONArray("errors")}")
            }
            return payload.optJSONObject("data") ?: throw Exception("LeetCode empty data")
        }
    }
}
