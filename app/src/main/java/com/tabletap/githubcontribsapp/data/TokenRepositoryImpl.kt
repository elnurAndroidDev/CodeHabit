package com.tabletap.githubcontribsapp.data

import android.content.Context
import androidx.core.content.edit
import com.tabletap.githubcontribsapp.domain.TokenRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

private const val PREFS_NAME = "auth_prefs"
private const val KEY_TOKEN = "github_token"

class TokenRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : TokenRepository {

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun saveToken(token: String) {
        prefs.edit { putString(KEY_TOKEN, token) }
    }

    override fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    override fun clearToken() {
        prefs.edit { remove(KEY_TOKEN) }
    }
}