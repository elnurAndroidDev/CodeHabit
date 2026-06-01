package com.tabletap.githubcontribsapp.di

import com.tabletap.githubcontribsapp.data.github.ContribsRepositoryImpl
import com.tabletap.githubcontribsapp.data.github.TokenRepositoryImpl
import com.tabletap.githubcontribsapp.domain.github.ContribsRepository
import com.tabletap.githubcontribsapp.domain.github.TokenRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindContribsRepository(impl: ContribsRepositoryImpl): ContribsRepository

    @Binds
    @Singleton
    abstract fun bindTokenRepository(impl: TokenRepositoryImpl): TokenRepository
}