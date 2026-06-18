package com.tabletap.githubcontribsapp.domain.github

import com.tabletap.githubcontribsapp.domain.Contrib
import javax.inject.Inject

class GetContribsUseCase @Inject constructor(
    private val repository: ContribsRepository
) {
    suspend operator fun invoke(
        username: String,
        from: String,
        to: String
    ): Result<List<Contrib>> = repository.getContributes(username, from, to)
}
