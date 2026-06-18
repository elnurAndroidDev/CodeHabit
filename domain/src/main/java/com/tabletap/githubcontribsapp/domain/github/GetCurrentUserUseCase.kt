package com.tabletap.githubcontribsapp.domain.github

import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val repository: ContribsRepository
) {
    suspend operator fun invoke(): Result<String> = repository.getCurrentUser()
}
