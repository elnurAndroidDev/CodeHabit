package com.tabletap.githubcontribsapp.di

import com.tabletap.githubcontribsapp.data.leetcode.LeetCodeProfileRepositoryImpl
import com.tabletap.githubcontribsapp.data.leetcode.LeetCodeRepositoryImpl
import com.tabletap.githubcontribsapp.domain.leetcode.LeetCodeProfileRepository
import com.tabletap.githubcontribsapp.domain.leetcode.LeetCodeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LeetCodeModule {

    @Binds
    @Singleton
    abstract fun bindLeetCodeRepository(
        impl: LeetCodeRepositoryImpl
    ): LeetCodeRepository

    @Binds
    @Singleton
    abstract fun bindLeetCodeProfileRepository(
        impl: LeetCodeProfileRepositoryImpl
    ): LeetCodeProfileRepository
}
