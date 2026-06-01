package com.tabletap.githubcontribsapp.data.github

import com.apollographql.apollo.ApolloClient
import com.tabletap.GetContributionsQuery
import com.tabletap.githubcontribsapp.domain.Contrib
import com.tabletap.githubcontribsapp.domain.github.ContribsRepository
import com.tabletap.githubcontribsapp.domain.github.TokenRepository
import javax.inject.Inject

class ContribsRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient,
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
}
