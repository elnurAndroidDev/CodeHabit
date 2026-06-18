package com.tabletap.githubcontribsapp.domain.leetcode

import com.tabletap.githubcontribsapp.domain.Contrib
import javax.inject.Inject

class GetLeetCodeContribsUseCase @Inject constructor(
    private val repository: LeetCodeRepository
) {
    suspend operator fun invoke(username: String): Result<List<Contrib>> =
        repository.getSubmissions(username)
}
