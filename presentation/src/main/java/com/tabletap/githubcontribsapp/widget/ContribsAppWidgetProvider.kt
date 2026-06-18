package com.tabletap.githubcontribsapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.tabletap.githubcontribsapp.domain.Contrib
import com.tabletap.githubcontribsapp.presentation.MainActivity
import com.tabletap.githubcontribsapp.presentation.R
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class ContribsAppWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS) ?: intArrayOf()
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                    try {
                        val mgr = AppWidgetManager.getInstance(context)
                        ids.forEach { id -> refreshWidget(context, mgr, id) }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_REFRESH -> {
                val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                    try {
                        if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                            refreshWidget(context, AppWidgetManager.getInstance(context), id)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            else -> super.onReceive(context, intent)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val ep = EntryPointAccessors.fromApplication<WidgetEntryPoint>(context)
        appWidgetIds.forEach { ep.widgetSourcePrefs().remove(it) }
    }

    companion object {
        const val ACTION_REFRESH = "com.tabletap.githubcontribsapp.widget.ACTION_REFRESH"

        private const val TITLE_AREA_DP = 32
        private const val ROOT_PADDING_DP = 16

        private data class SourceData(val title: String, val contribs: List<Contrib>?)

        fun scheduleUpdate(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                refreshWidget(context, appWidgetManager, widgetId)
            }
        }

        private suspend fun refreshWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int
        ) {
            val ep = EntryPointAccessors.fromApplication<WidgetEntryPoint>(context)
            val source = ep.widgetSourcePrefs().get(widgetId) ?: return
            val data = fetchData(ep, source)
            val views = buildRemoteViews(context, appWidgetManager, widgetId, data)
            appWidgetManager.updateAppWidget(widgetId, views)
        }

        private suspend fun fetchData(ep: WidgetEntryPoint, source: WidgetSource): SourceData =
            when (source) {
                WidgetSource.GitHub -> fetchGitHub(ep)
                WidgetSource.LeetCode -> fetchLeetCode(ep)
                WidgetSource.Combined -> {
                    val gh = fetchGitHub(ep).contribs.orEmpty()
                    val lc = fetchLeetCode(ep).contribs.orEmpty()
                    val merged = if (gh.isEmpty() && lc.isEmpty()) null else mergeContribs(gh, lc)
                    SourceData(title = "Combined", contribs = merged)
                }
            }

        private suspend fun fetchGitHub(ep: WidgetEntryPoint): SourceData {
            val username = ep.contribsRepository().getCurrentUser().getOrNull()
                ?: return SourceData(title = "GitHub", contribs = null)
            val now = ZonedDateTime.now(ZoneOffset.UTC)
            val to = now.format(DateTimeFormatter.ISO_INSTANT)
            val from = now.minusYears(1).format(DateTimeFormatter.ISO_INSTANT)
            val contribs = ep.contribsRepository().getContributes(username, from, to).getOrNull()
            return SourceData(title = "GitHub · @$username", contribs = contribs)
        }

        private suspend fun fetchLeetCode(ep: WidgetEntryPoint): SourceData {
            val username = ep.leetCodeProfileRepository().getUsername()
                ?: return SourceData(title = "LeetCode", contribs = null)
            val contribs = ep.leetCodeRepository().getSubmissions(username).getOrNull()
            return SourceData(title = "LeetCode · @$username", contribs = contribs)
        }

        private fun mergeContribs(a: List<Contrib>, b: List<Contrib>): List<Contrib> {
            val map = mutableMapOf<String, Int>()
            for (c in a) map[c.date] = (map[c.date] ?: 0) + c.count
            for (c in b) map[c.date] = (map[c.date] ?: 0) + c.count
            val today = LocalDate.now(ZoneOffset.UTC)
            val firstDay = today.minusDays(370L)
            return (0..370).map { offset ->
                val date = firstDay.plusDays(offset.toLong())
                Contrib(date = date.toString(), count = map[date.toString()] ?: 0)
            }
        }

        private fun buildRemoteViews(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
            data: SourceData
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_contribs)

            views.setTextViewText(R.id.widget_title, data.title)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPi = PendingIntent.getActivity(
                context, widgetId, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openPi)

            val refreshIntent = Intent(context, ContribsAppWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            val refreshPi = PendingIntent.getBroadcast(
                context, widgetId + 10000, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh, refreshPi)

            val contribs = data.contribs
            if (contribs.isNullOrEmpty()) {
                views.setViewVisibility(R.id.widget_image, View.GONE)
                views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
                return views
            }

            views.setViewVisibility(R.id.widget_image, View.VISIBLE)
            views.setViewVisibility(R.id.widget_empty, View.GONE)

            val options = appWidgetManager.getAppWidgetOptions(widgetId)
            val density = context.resources.displayMetrics.density
            val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 250)
            val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 110)
            val contentWidthDp = (widthDp - ROOT_PADDING_DP).coerceAtLeast(40)
            val contentHeightDp = (heightDp - ROOT_PADDING_DP - TITLE_AREA_DP).coerceAtLeast(40)
            val widthPx = (contentWidthDp * density).toInt()
            val heightPx = (contentHeightDp * density).toInt()

            val bitmap = ContribGraphRenderer.render(contribs, widthPx, heightPx, density)
            views.setImageViewBitmap(R.id.widget_image, bitmap)

            return views
        }
    }
}