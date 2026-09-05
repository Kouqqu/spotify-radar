package com.spotifyradar.app

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object StoryCardGenerator {

    fun generateCard(result: RadarAnalyzer.AnalysisResult): Bitmap {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Dark Spotify Gradient Background
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                Color.parseColor("#121212"),
                Color.parseColor("#181818"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Accent ambient spot
        val spotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                width * 0.85f, height * 0.15f, 400f,
                Color.parseColor("#331ED760"),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(width * 0.85f, height * 0.15f, 400f, spotPaint)

        // 2. Header
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1ED760")
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        canvas.drawText("SPOTIFY TOP 500 RADAR", 80f, 130f, brandPaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#888888")
            textSize = 26f
            typeface = Typeface.DEFAULT
        }
        canvas.drawText("АНАЛИЗ МУЗЫКАЛЬНОГО ВКУСА", 80f, 175f, subPaint)

        // 3. Verdict Hero Card
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1A1A1A")
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2E2E2E")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        val verdictRect = RectF(80f, 230f, width - 80f, 480f)
        canvas.drawRoundRect(verdictRect, 32f, 32f, cardPaint)
        canvas.drawRoundRect(verdictRect, 32f, 32f, borderPaint)

        val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 80f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(result.verdictEmoji, width / 2f, 330f, emojiPaint)

        val verdictTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 46f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(result.verdictTitle, width / 2f, 395f, verdictTitlePaint)

        val verdictDescPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#AAAAAA")
            textSize = 26f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(result.verdictDesc, width / 2f, 440f, verdictDescPaint)

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
            val rect = RectF(left, 520f, left + metricWidth, 700f)
            canvas.drawRoundRect(rect, 24f, 24f, cardPaint)
            canvas.drawRoundRect(rect, 24f, 24f, borderPaint)

            val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = metricColors[i]
                textSize = 52f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(metricValues[i], left + metricWidth / 2f, 615f, valPaint)

            val lblPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#888888")
                textSize = 24f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(metricLabels[i], left + metricWidth / 2f, 665f, lblPaint)
        }

        // 5. Top Artist Spotlight
        val topArtistRect = RectF(80f, 740f, width - 80f, 950f)
        canvas.drawRoundRect(topArtistRect, 28f, 28f, cardPaint)
        canvas.drawRoundRect(topArtistRect, 28f, 28f, borderPaint)

        val topTagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#888888")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.05f
        }
        canvas.drawText("САМЫЙ ПОПУЛЯРНЫЙ АРТИСТ В ПЛЕЙЛИСТЕ", 120f, 800f, topTagPaint)

        val highest = result.highestArtist
        val topName = highest?.name ?: "Нет в топе"
        val topRank = if (highest != null) "#${highest.rank} В МИРЕ" else "—"

        val topNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 46f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(topName, 120f, 865f, topNamePaint)

        val topRankPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1ED760")
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(topRank, 120f, 915f, topRankPaint)

        // 6. Top-5 List
        val listHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Твои артисты из мирового Топ-500:", 80f, 1020f, listHeaderPaint)

        val top5 = result.matchedArtists.take(5)
        var yPos = 1080f
        top5.forEachIndexed { index, artist ->
            val rowRect = RectF(80f, yPos, width - 80f, yPos + 100f)
            canvas.drawRoundRect(rowRect, 20f, 20f, cardPaint)
            canvas.drawRoundRect(rowRect, 20f, 20f, borderPaint)

            val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1ED760")
                textSize = 32f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("${index + 1}", 120f, yPos + 62f, numPaint)

            val aNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 32f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(artist.name, 175f, yPos + 62f, aNamePaint)

            val aRankPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#888888")
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("#${artist.rank}", width - 120f, yPos + 62f, aRankPaint)

            yPos += 120f
        }

        // 7. Footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#666666")
            textSize = 26f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Spotify Top 500 Radar • saaanek.github.io/spotify-radar", width / 2f, 1800f, footerPaint)

        return bitmap
    }

    fun saveToGallery(context: Context, bitmap: Bitmap): Uri? {
        val filename = "spotify_radar_${System.currentTimeMillis()}.png"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
            val file = File(imagesDir, filename)
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            Uri.fromFile(file)
        }
    }

    fun shareImage(context: Context, bitmap: Bitmap) {
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
    }
}
