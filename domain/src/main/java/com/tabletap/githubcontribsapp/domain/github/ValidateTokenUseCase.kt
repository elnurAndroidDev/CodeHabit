package com.tabletap.githubcontribsapp.domain.github

import javax.inject.Inject

class ValidateTokenUseCase @Inject constructor(
    private val repository: ContribsRepository
) {
    suspend operator fun invoke(token: String): Result<Unit> =
        repository.validateToken(token.trim())
}
