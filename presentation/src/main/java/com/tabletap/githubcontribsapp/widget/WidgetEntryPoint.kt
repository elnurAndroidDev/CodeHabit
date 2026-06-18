package com.tabletap.githubcontribsapp.widget

import com.tabletap.githubcontribsapp.domain.github.TokenRepository
import com.tabletap.githubcontribsapp.domain.leetcode.LeetCodeProfileRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun tokenRepository(): TokenRepository
    fun leetCodeProfileRepository(): LeetCodeProfileRepository
    fun getCurrentUser(): GetCurrentUserUseCase
    fun getContribs(): GetContribsUseCase
    fun getLeetCodeContribs(): GetLeetCodeContribsUseCase
    fun widgetSourcePrefs(): WidgetSourcePrefs
}