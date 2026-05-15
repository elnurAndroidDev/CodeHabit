package com.tabletap.githubcontribsapp.di

import com.tabletap.githubcontribsapp.data.ContribsRepositoryImpl
import com.tabletap.githubcontribsapp.data.TokenRepositoryImpl
import com.tabletap.githubcontribsapp.domain.ContribsRepository
import com.tabletap.githubcontribsapp.domain.TokenRepository
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