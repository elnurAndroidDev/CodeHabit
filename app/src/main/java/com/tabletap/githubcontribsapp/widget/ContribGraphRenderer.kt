package com.tabletap.githubcontribsapp.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.tabletap.githubcontribsapp.domain.Contrib
import com.tabletap.githubcontribsapp.presentation.home.components.HeatmapStyle
import com.tabletap.githubcontribsapp.presentation.home.components.MonthBlock
import com.tabletap.githubcontribsapp.presentation.home.components.buildMonthBlocks
import com.tabletap.githubcontribsapp.presentation.home.components.buildWeeks
import com.tabletap.githubcontribsapp.presentation.home.components.githubPaletteInts
import com.tabletap.githubcontribsapp.presentation.home.components.leetcodePaletteInts
import com.tabletap.githubcontribsapp.presentation.home.components.monthAbbrev
import com.tabletap.githubcontribsapp.presentation.home.components.paletteIndex
import java.time.LocalDate

object ContribGraphRenderer {

    fun render(
        contribs: List<Contrib>,
        style: HeatmapStyle,
        widthPx: Int,
        heightPx: Int,
        density: Float
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        when (style) {
            HeatmapStyle.GitHub -> renderGitHub(canvas, contribs, widthPx, heightPx, density)
            HeatmapStyle.LeetCode -> renderLeetCode(canvas, contribs, widthPx, heightPx, density)
        }
        return bitmap
    }

    private fun renderGitHub(
        canvas: Canvas,
        contribs: List<Contrib>,
        widthPx: Int,
        heightPx: Int,
        density: Float
    ) {
        val padding = 4f * density
        val labelTextPx = 11f * density
        val labelAreaPx = labelTextPx + 4f * density
        val gap = 1.5f * density

        val gridHeight = heightPx - labelAreaPx - padding * 2
        val cellSize = (gridHeight - gap * 6f) / 7f
        val stride = cellSize + gap
        val maxWeeks = ((widthPx - padding * 2 + gap) / stride).toInt().coerceAtLeast(1)

        val allWeeks = buildWeeks(contribs) ?: return
        val weeks = if (allWeeks.size <= maxWeeks) allWeeks else allWeeks.takeLast(maxWeeks)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = RectF()
        val cornerRadius = cellSize * 0.18f
        val gridTop = labelAreaPx + padding

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF6E7681.toInt()
            textSize = labelTextPx
        }
        val labelBaselineY = labelTextPx

        var lastLabeledMonth: java.time.Month? = null
        weeks.forEachIndexed { weekIndex, week ->
            val x = padding + weekIndex * stride
            week.days.forEachIndexed { dayIndex, count ->
                if (count != null) {
                    paint.color = githubPaletteInts[paletteIndex(count)]
                    rect.set(x, gridTop + dayIndex * stride, x + cellSize, gridTop + dayIndex * stride + cellSize)
                    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
                }
            }
            val month = week.weekFirstDay.month
            if (month != lastLabeledMonth &&
                (week.startsNewMonth || (weekIndex == 0 && lastLabeledMonth == null))
            ) {
                canvas.drawText(monthAbbrev(month), x, labelBaselineY, labelPaint)
                lastLabeledMonth = month
            }
        }
    }

    private fun renderLeetCode(
        canvas: Canvas,
        contribs: List<Contrib>,
        widthPx: Int,
        heightPx: Int,
        density: Float
    ) {
        val allBlocks = buildMonthBlocks(contribs) ?: return
        val byDate = contribs.mapNotNull { c ->
            runCatching { LocalDate.parse(c.date) to c.count }.getOrNull()
        }.toMap()

        val padding = 4f * density
        val labelTextPx = 11f * density
        val labelAreaPx = labelTextPx + 4f * density
        val monthGap = 6f * density
        val gap = 1.5f * density

        val gridHeight = heightPx - labelAreaPx - padding * 2
        val cellSize = (gridHeight - gap * 6f) / 7f
        val stride = cellSize + gap
        val gridTop = labelAreaPx + padding

        val visibleBlocks = fitBlocks(allBlocks, widthPx.toFloat(), padding, stride, gap, monthGap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = RectF()
        val cornerRadius = cellSize * 0.18f

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF6E7681.toInt()
            textSize = labelTextPx
        }
        val labelBaselineY = labelTextPx

        var x = padding
        visibleBlocks.forEachIndexed { blockIndex, block ->
            if (blockIndex > 0) x += monthGap

            val n = block.weekColumns.size
            val blockWidth = n * stride - gap

            val label = monthAbbrev(block.month)
            val labelWidth = labelPaint.measureText(label)
            val labelX = x + (blockWidth - labelWidth) / 2f
            canvas.drawText(label, labelX, labelBaselineY, labelPaint)

            block.weekColumns.forEachIndexed { weekIndex, week ->
                val colX = x + weekIndex * stride
                week.forEachIndexed { dayIndex, date ->
                    if (date != null) {
                        val count = byDate[date] ?: 0
                        paint.color = leetcodePaletteInts[paletteIndex(count)]
                        rect.set(colX, gridTop + dayIndex * stride, colX + cellSize, gridTop + dayIndex * stride + cellSize)
                        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
                    }
                }
            }

            x += blockWidth
        }
    }

    private fun fitBlocks(
        blocks: List<MonthBlock>,
        widthPx: Float,
        padding: Float,
        stride: Float,
        gap: Float,
        monthGap: Float
    ): List<MonthBlock> {
        val result = mutableListOf<MonthBlock>()
        var usedWidth = padding * 2
        for (block in blocks.reversed()) {
            val n = block.weekColumns.size
            val blockWidth = n * stride - gap + (if (result.isNotEmpty()) monthGap else 0f)
            if (usedWidth + blockWidth > widthPx) break
            result.add(0, block)
            usedWidth += blockWidth
        }
        return result.ifEmpty { blocks.takeLast(1) }
    }
}
