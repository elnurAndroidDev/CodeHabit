package com.tabletap.githubcontribsapp.data.leetcode

import android.content.Context
import androidx.core.content.edit
import com.tabletap.githubcontribsapp.domain.leetcode.LeetCodeProfileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

private const val PREFS_NAME = "auth_prefs"
private const val KEY_LEETCODE_USERNAME = "leetcode_username"
private const val KEY_FIRST_RUN_COMPLETE = "first_run_complete"

class LeetCodeProfileRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : LeetCodeProfileRepository {

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun saveUsername(username: String) {
        prefs.edit { putString(KEY_LEETCODE_USERNAME, username) }
    }

    override fun getUsername(): String? =
        prefs.getString(KEY_LEETCODE_USERNAME, null)

    override fun clearUsername() {
        prefs.edit { remove(KEY_LEETCODE_USERNAME) }
    }

    override fun isFirstRunComplete(): Boolean =
        prefs.getBoolean(KEY_FIRST_RUN_COMPLETE, false)

    override fun markFirstRunComplete() {
        prefs.edit { putBoolean(KEY_FIRST_RUN_COMPLETE, true) }
    }

    override fun reset() {
        prefs.edit {
            remove(KEY_LEETCODE_USERNAME)
            remove(KEY_FIRST_RUN_COMPLETE)
        }
    }
}
