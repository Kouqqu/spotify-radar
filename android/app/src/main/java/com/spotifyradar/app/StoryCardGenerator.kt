package com.spotifyradar.app

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object StoryCardGenerator {

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = StringBuilder(testLine)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                }
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var truncated = text
        while (truncated.isNotEmpty() && paint.measureText("$truncated…") > maxWidth) {
            truncated = truncated.dropLast(1)
        }
        return "$truncated…"
    }

    fun generateCard(result: RadarAnalyzer.AnalysisResult): Bitmap? {
        val width = 1080
        val height = 1920
        val bitmap = try {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        } catch (e: Throwable) {
            return null
        }
        val canvas = Canvas(bitmap)

        // 1. Background gradient
        val bgPaint = Paint().apply {
            color = Color.parseColor("#121212")
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Glow circle at the top-left
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                200f, 300f, 600f,
                intArrayOf(Color.parseColor("#261ED760"), Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(200f, 300f, 600f, glowPaint)

        // Glow circle bottom-right
        val glowPaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                900f, 1500f, 700f,
                intArrayOf(Color.parseColor("#26B388FF"), Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(900f, 1500f, 700f, glowPaint2)

        // 2. Header Brand
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1ED760")
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.1f
        }
        canvas.drawText("SPOTIFY TOP-500 RADAR", 80f, 140f, brandPaint)

        val subBrandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#888888")
            textSize = 22f
            letterSpacing = 0.05f
        }
        canvas.drawText("АНАЛИЗ МУЗЫКАЛЬНОГО ВКУСА", 80f, 175f, subBrandPaint)

        // 3. Verdict Card (Box)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1A1A1A")
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2E2E2E")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        // Expanded height from 220f to 510f so text never overflows
        val verdictRect = RectF(80f, 215f, width - 80f, 510f)
        canvas.drawRoundRect(verdictRect, 32f, 32f, cardPaint)
        canvas.drawRoundRect(verdictRect, 32f, 32f, borderPaint)

        val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 72f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(result.verdictEmoji, width / 2f, 300f, emojiPaint)

        val verdictTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(result.verdictTitle, width / 2f, 365f, verdictTitlePaint)

        val verdictDescPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#BBBBBB")
            textSize = 25f
            textAlign = Paint.Align.CENTER
        }
        // Multi-line text wrapping with comfortable max width
        val descLines = wrapText(result.verdictDesc, verdictDescPaint, width - 240f)
        var descY = if (descLines.size > 1) 420f else 435f
        for (line in descLines) {
            canvas.drawText(line, width / 2f, descY, verdictDescPaint)
            descY += 38f
        }

        // 4. Metrics Cards (3 in a row)
        val metricWidth = (width - 160f - 40f) / 3f
        val metricLabels = listOf("В Топ-500", "% Чартов", "Андеграунд")
        val metricValues = listOf(
            "${result.matchedCount}",
            "${result.chartPercentage}%",
            "${result.indieCount}"
        )
        val metricColors = listOf(
            Color.parseColor("#1ED760"),
            Color.parseColor("#1ED760"),
            Color.parseColor("#B388FF")
        )

        for (i in 0..2) {
            val left = 80f + i * (metricWidth + 20f)
            val rect = RectF(left, 535f, left + metricWidth, 715f)
            canvas.drawRoundRect(rect, 24f, 24f, cardPaint)
            canvas.drawRoundRect(rect, 24f, 24f, borderPaint)

            val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = metricColors[i]
                textSize = 50f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(metricValues[i], left + metricWidth / 2f, 625f, valPaint)

            val lblPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#888888")
                textSize = 23f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(metricLabels[i], left + metricWidth / 2f, 675f, lblPaint)
        }

        // 5. Top Artist Spotlight
        val topArtistRect = RectF(80f, 745f, width - 80f, 955f)
        canvas.drawRoundRect(topArtistRect, 28f, 28f, cardPaint)
        canvas.drawRoundRect(topArtistRect, 28f, 28f, borderPaint)

        val topTagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#888888")
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.05f
        }
        canvas.drawText("САМЫЙ ПОПУЛЯРНЫЙ АРТИСТ В ПЛЕЙЛИСТЕ", 120f, 805f, topTagPaint)

        val highest = result.highestArtist
        val rawTopName = highest?.name ?: "Нет в топе"
        val topRank = if (highest != null) "#${highest.rank} В МИРЕ" else "—"

        val topNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 44f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val safeTopName = ellipsize(rawTopName, topNamePaint, width - 240f)
        canvas.drawText(safeTopName, 120f, 865f, topNamePaint)

        val topRankPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1ED760")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(topRank, 120f, 915f, topRankPaint)

        // 6. Top-5 List
        val listHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Твои артисты из мирового Топ-500:", 80f, 1020f, listHeaderPaint)

        val top5 = result.matchedArtists.take(5)
        var yPos = 1070f
        val aNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1ED760")
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val aRankPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#888888")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        top5.forEachIndexed { index, artist ->
            val rowRect = RectF(80f, yPos, width - 80f, yPos + 96f)
            canvas.drawRoundRect(rowRect, 20f, 20f, cardPaint)
            canvas.drawRoundRect(rowRect, 20f, 20f, borderPaint)

            canvas.drawText("${index + 1}", 120f, yPos + 60f, numPaint)

            val safeArtistName = ellipsize(artist.name, aNamePaint, width - 420f)
            canvas.drawText(safeArtistName, 175f, yPos + 60f, aNamePaint)

            canvas.drawText("#${artist.rank}", width - 120f, yPos + 60f, aRankPaint)

            yPos += 116f
        }

        // 7. Footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#666666")
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Spotify Top 500 Radar • kouqqu.github.io/spotify-radar", width / 2f, 1820f, footerPaint)

        return bitmap
    }

    fun saveToGallery(context: Context, bitmap: Bitmap): Uri? {
        val filename = "spotify_radar_${System.currentTimeMillis()}.png"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SpotifyRadar")
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    }
                }
                uri
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = File(imagesDir, "SpotifyRadar")
                appDir.mkdirs()
                val file = File(appDir, filename)
                FileOutputStream(file).use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                Uri.fromFile(file)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    fun shareImage(context: Context, bitmap: Bitmap) {
        try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "spotify_radar_story.png")
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Поделиться карточкой"))
        } catch (e: Throwable) {
            e.printStackTrace()
            Toast.makeText(context, "Не удалось открыть меню отправки", Toast.LENGTH_SHORT).show()
        }
    }
}
