package com.example.ai

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.Base64
import androidx.core.graphics.PathParser
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Detects inline base64 images or <svg>...</svg> code returned by text models
 * and rasterizes SVG markup into a PNG data URL for in-chat preview and gallery export.
 */
object SvgCodeToImageUtil {
    private val DATA_URL_REGEX = Regex(
        """data:image/(?:png|jpe?g|webp);base64,[A-Za-z0-9+/=\s]+""",
        RegexOption.IGNORE_CASE
    )
    private val SVG_BLOCK_REGEX = Regex(
        """<svg\b[^>]*>[\s\S]*?</svg>""",
        RegexOption.IGNORE_CASE
    )

    fun extractAndRenderCodeImage(rawText: String): String? {
        val dataUri = DATA_URL_REGEX.find(rawText)?.value?.replace("\\s".toRegex(), "")
        if (!dataUri.isNullOrBlank()) return dataUri

        val svgMarkup = SVG_BLOCK_REGEX.find(rawText)?.value ?: return null
        return renderSvgToPngDataUrl(svgMarkup)
    }

    fun containsSvgMarkup(rawText: String): Boolean = SVG_BLOCK_REGEX.containsMatchIn(rawText)

    private fun renderSvgToPngDataUrl(svg: String, outputPx: Int = 720): String? {
        return try {
            val openTag = Regex("""<svg\b([^>]*)>""", RegexOption.IGNORE_CASE).find(svg)?.groupValues?.getOrNull(1).orEmpty()
            val viewBoxParts = parseAttr(openTag, "viewBox")
                ?.trim()
                ?.split(Regex("[\\s,]+"))
                ?.mapNotNull { it.toFloatOrNull() }
            val vbMinX = viewBoxParts?.getOrNull(0) ?: 0f
            val vbMinY = viewBoxParts?.getOrNull(1) ?: 0f
            val vbWidth = viewBoxParts?.getOrNull(2)?.takeIf { it > 0f }
                ?: parseDimension(parseAttr(openTag, "width"))
                ?: 512f
            val vbHeight = viewBoxParts?.getOrNull(3)?.takeIf { it > 0f }
                ?: parseDimension(parseAttr(openTag, "height"))
                ?: 512f

            val aspect = vbWidth / vbHeight
            val bmpW = if (aspect >= 1f) outputPx else (outputPx * aspect).toInt().coerceAtLeast(128)
            val bmpH = if (aspect >= 1f) (outputPx / aspect).toInt().coerceAtLeast(128) else outputPx

            val bitmap = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)

            val scaleX = bmpW.toFloat() / vbWidth
            val scaleY = bmpH.toFloat() / vbHeight
            canvas.save()
            canvas.scale(scaleX, scaleY)
            canvas.translate(-vbMinX, -vbMinY)

            val tagRegex = Regex(
                """<(rect|circle|ellipse|line|polygon|polyline|path|text)\b([^>]*?)(?:/>|>([\s\S]*?)</\1>)""",
                RegexOption.IGNORE_CASE
            )
            tagRegex.findAll(svg).forEach { match ->
                val tag = match.groupValues[1].lowercase()
                val attrs = match.groupValues[2]
                val innerText = match.groupValues[3]
                drawSvgElement(canvas, tag, attrs, innerText, min(vbWidth, vbHeight))
            }

            canvas.restore()
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            bitmap.recycle()
            val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            "data:image/png;base64,$b64"
        } catch (_: Exception) {
            null
        }
    }

    private fun drawSvgElement(
        canvas: Canvas,
        tag: String,
        attrs: String,
        innerText: String,
        refSize: Float,
    ) {
        val fillColor = parseSvgColor(parseAttr(attrs, "fill"), defaultColor = if (tag == "line") Color.TRANSPARENT else Color.DKGRAY)
        val strokeColor = parseSvgColor(parseAttr(attrs, "stroke"), defaultColor = Color.TRANSPARENT)
        val strokeWidth = parseAttr(attrs, "stroke-width")?.toFloatOrNull() ?: 2f
        val opacity = parseAttr(attrs, "opacity")?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 1f

        val fillPaint = if (fillColor != Color.TRANSPARENT) {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = applyOpacity(fillColor, opacity)
            }
        } else null

        val strokePaint = if (strokeColor != Color.TRANSPARENT && strokeWidth > 0f) {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = applyOpacity(strokeColor, opacity)
                this.strokeWidth = strokeWidth
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
        } else null

        when (tag) {
            "rect" -> {
                val x = parseAttr(attrs, "x")?.toFloatOrNull() ?: 0f
                val y = parseAttr(attrs, "y")?.toFloatOrNull() ?: 0f
                val w = parseAttr(attrs, "width")?.toFloatOrNull() ?: return
                val h = parseAttr(attrs, "height")?.toFloatOrNull() ?: return
                val rx = parseAttr(attrs, "rx")?.toFloatOrNull() ?: 0f
                val ry = parseAttr(attrs, "ry")?.toFloatOrNull() ?: rx
                val rect = RectF(x, y, x + w, y + h)
                fillPaint?.let { canvas.drawRoundRect(rect, rx, ry, it) }
                strokePaint?.let { canvas.drawRoundRect(rect, rx, ry, it) }
            }
            "circle" -> {
                val cx = parseAttr(attrs, "cx")?.toFloatOrNull() ?: 0f
                val cy = parseAttr(attrs, "cy")?.toFloatOrNull() ?: 0f
                val r = parseAttr(attrs, "r")?.toFloatOrNull() ?: return
                fillPaint?.let { canvas.drawCircle(cx, cy, r, it) }
                strokePaint?.let { canvas.drawCircle(cx, cy, r, it) }
            }
            "ellipse" -> {
                val cx = parseAttr(attrs, "cx")?.toFloatOrNull() ?: 0f
                val cy = parseAttr(attrs, "cy")?.toFloatOrNull() ?: 0f
                val rx = parseAttr(attrs, "rx")?.toFloatOrNull() ?: return
                val ry = parseAttr(attrs, "ry")?.toFloatOrNull() ?: return
                val rect = RectF(cx - rx, cy - ry, cx + rx, cy + ry)
                fillPaint?.let { canvas.drawOval(rect, it) }
                strokePaint?.let { canvas.drawOval(rect, it) }
            }
            "line" -> {
                val x1 = parseAttr(attrs, "x1")?.toFloatOrNull() ?: 0f
                val y1 = parseAttr(attrs, "y1")?.toFloatOrNull() ?: 0f
                val x2 = parseAttr(attrs, "x2")?.toFloatOrNull() ?: 0f
                val y2 = parseAttr(attrs, "y2")?.toFloatOrNull() ?: 0f
                val p = strokePaint ?: Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    color = applyOpacity(Color.BLACK, opacity)
                    this.strokeWidth = max(strokeWidth, 1.5f)
                }
                canvas.drawLine(x1, y1, x2, y2, p)
            }
            "polygon", "polyline" -> {
                val nums = parseAttr(attrs, "points")
                    ?.trim()
                    ?.split(Regex("[\\s,]+"))
                    ?.mapNotNull { it.toFloatOrNull() }
                    ?: return
                if (nums.size < 4) return
                val path = Path()
                path.moveTo(nums[0], nums[1])
                var idx = 2
                while (idx + 1 < nums.size) {
                    path.lineTo(nums[idx], nums[idx + 1])
                    idx += 2
                }
                if (tag == "polygon") path.close()
                if (tag == "polygon") fillPaint?.let { canvas.drawPath(path, it) }
                strokePaint?.let { canvas.drawPath(path, it) }
            }
            "path" -> {
                val d = parseAttr(attrs, "d")?.takeIf { it.isNotBlank() } ?: return
                val path = runCatching { PathParser.createPathFromPathData(d) }.getOrNull() ?: return
                fillPaint?.let { canvas.drawPath(path, it) }
                strokePaint?.let { canvas.drawPath(path, it) }
            }
            "text" -> {
                val clean = innerText.replace(Regex("<[^>]+>"), "").trim()
                if (clean.isEmpty()) return
                val x = parseAttr(attrs, "x")?.toFloatOrNull() ?: 0f
                val y = parseAttr(attrs, "y")?.toFloatOrNull() ?: 24f
                val fontSize = parseDimension(parseAttr(attrs, "font-size")) ?: (refSize * 0.06f)
                val anchor = parseAttr(attrs, "text-anchor")?.lowercase()
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (fillColor != Color.TRANSPARENT) applyOpacity(fillColor, opacity) else Color.BLACK
                    textSize = fontSize
                    textAlign = when (anchor) {
                        "middle" -> Paint.Align.CENTER
                        "end" -> Paint.Align.RIGHT
                        else -> Paint.Align.LEFT
                    }
                }
                canvas.drawText(clean, x, y, textPaint)
            }
        }
    }

    private fun parseAttr(attrs: String, name: String): String? {
        val attrRegex = Regex("""(?:^|\s)$name\s*=\s*(?:"([^"]*)"|'([^']*)')""", RegexOption.IGNORE_CASE)
        val match = attrRegex.find(attrs)
        if (match != null) {
            return match.groupValues[1].ifEmpty { match.groupValues[2] }
        }
        val styleStr = Regex("""style\s*=\s*"([^"]*)"""", RegexOption.IGNORE_CASE).find(attrs)?.groupValues?.getOrNull(1)
        if (!styleStr.isNullOrBlank()) {
            val styleMatch = Regex("""(?:^|;)\s*$name\s*:\s*([^;]+)""", RegexOption.IGNORE_CASE).find(styleStr)
            if (styleMatch != null) return styleMatch.groupValues[1].trim()
        }
        return null
    }

    private fun parseDimension(raw: String?): Float? {
        val clean = raw?.trim()?.removeSuffix("px")?.removeSuffix("pt") ?: return null
        return clean.toFloatOrNull()
    }

    private fun parseSvgColor(raw: String?, defaultColor: Int): Int {
        val clean = raw?.trim()?.lowercase() ?: return defaultColor
        if (clean.isEmpty() || clean == "none" || clean == "transparent") return Color.TRANSPARENT
        if (clean.startsWith("url(")) return defaultColor
        val hex = if (clean.startsWith("#") && clean.length == 4) {
            "#${clean[1]}${clean[1]}${clean[2]}${clean[2]}${clean[3]}${clean[3]}"
        } else clean
        return runCatching { Color.parseColor(hex) }.getOrElse { defaultColor }
    }

    private fun applyOpacity(color: Int, opacity: Float): Int {
        val alpha = (Color.alpha(color) * opacity).toInt().coerceIn(0, 255)
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}
