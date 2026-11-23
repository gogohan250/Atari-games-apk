package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import com.example.data.model.AtariSystem
import com.example.data.model.GameEntity
import java.io.File
import java.io.FileOutputStream

object CoverGenerator {
    fun ensureCoverCached(context: Context, game: GameEntity): String {
        val cacheDir = File(context.cacheDir, "covers").apply { mkdirs() }
        val file = File(cacheDir, "${game.id}.png")
        if (file.exists() && file.length() > 100L) return file.absolutePath

        val width = 260
        val height = 260
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val baseColor = try {
            android.graphics.Color.parseColor(game.colorHex)
        } catch (_: Exception) {
            0xFFFF5A1F.toInt()
        }

        val darkerColor = darkenColor(baseColor, 0.58f)
        val lighterColor = lightenColor(baseColor, 0.14f)

        // Two-tone diagonal split matching attached Atari style guide
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                lighterColor, baseColor, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val diagPath = Path().apply {
            moveTo(width * 0.15f, height.toFloat())
            lineTo(width.toFloat(), height * 0.25f)
            lineTo(width.toFloat(), height.toFloat())
            close()
        }
        val diagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkerColor
        }
        canvas.drawPath(diagPath, diagPaint)

        // Subtle Atari 4-stripe accent at bottom-left
        val stripes = intArrayOf(0xFFFFB000.toInt(), 0xFFFF7A1F.toInt(), 0xFFFF5A1F.toInt(), 0xFFD62828.toInt())
        val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        stripes.forEachIndexed { idx, col ->
            stripePaint.color = col
            val y = height - 28f + idx * 5f
            canvas.drawRect(16f, y, 74f, y + 3f, stripePaint)
        }

        // System tag
        val sys = AtariSystem.fromCode(game.system)
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x55000000
        }
        canvas.drawRoundRect(16f, 16f, 114f, 44f, 10f, 10f, badgePaint)

        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 18f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        canvas.drawText(sys.shortName, 26f, 36f, badgeTextPaint)

        // Title text centered
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = if (game.title.length > 12) 24f else 30f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            setShadowLayer(4f, 0f, 2f, 0x88000000.toInt())
        }
        val shortTitle = if (game.title.length > 16) game.title.take(15) + "…" else game.title
        canvas.drawText(shortTitle, width / 2f, height / 2f + 10f, titlePaint)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
        }
        bitmap.recycle()
        return file.absolutePath
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        val r = (android.graphics.Color.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (android.graphics.Color.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (android.graphics.Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return android.graphics.Color.rgb(r, g, b)
    }

    private fun lightenColor(color: Int, factor: Float): Int {
        val r = (android.graphics.Color.red(color) + (255 - android.graphics.Color.red(color)) * factor).toInt().coerceIn(0, 255)
        val g = (android.graphics.Color.green(color) + (255 - android.graphics.Color.green(color)) * factor).toInt().coerceIn(0, 255)
        val b = (android.graphics.Color.blue(color) + (255 - android.graphics.Color.blue(color)) * factor).toInt().coerceIn(0, 255)
        return android.graphics.Color.rgb(r, g, b)
    }
}
