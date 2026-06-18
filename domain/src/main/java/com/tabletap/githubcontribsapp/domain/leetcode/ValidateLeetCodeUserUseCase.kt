package com.tabletap.githubcontribsapp.domain.leetcode

import javax.inject.Inject

class ValidateLeetCodeUserUseCase @Inject constructor(
    private val repository: LeetCodeRepository
) {
    suspend operator fun invoke(username: String): Result<Unit> =
        repository.userExists(username.trim())
}
