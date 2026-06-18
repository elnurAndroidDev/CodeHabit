package com.tabletap.githubcontribsapp.widget

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetSourcePrefs @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val prefs get() = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)

    fun set(widgetId: Int, source: WidgetSource) =
        prefs.edit().putString("widget_${widgetId}_source", source.name).apply()

    fun get(widgetId: Int): WidgetSource? =
        prefs.getString("widget_${widgetId}_source", null)
            ?.let { runCatching { WidgetSource.valueOf(it) }.getOrNull() }

    fun remove(widgetId: Int) =
        prefs.edit().remove("widget_${widgetId}_source").apply()
}
