package com.tabletap.githubcontribsapp.domain

import javax.inject.Inject

class GetContribsUseCase @Inject constructor(
    private val repository: ContribsRepository
) {
    suspend operator fun invoke(
        username: String,
        from: String,
        to: String
    ): Result<List<Contrib>> {
        return repository.getContributes(username, from, to)
    }
}