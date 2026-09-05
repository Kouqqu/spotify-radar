package com.spotifyradar.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val tag: String,
    val title: String,
    val body: String,
    val downloadUrl: String
)

object UpdateChecker {
    private const val GITHUB_API_URL = "https://api.github.com/repos/Kouqqu/spotify-radar/releases/latest"

    suspend fun checkForUpdates(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_API_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "SpotifyRadarApp")
            conn.connectTimeout = 6000
            conn.readTimeout = 6000

            if (conn.responseCode == 200) {
                val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonText)
                val tagName = json.optString("tag_name", "")
                val name = json.optString("name", "Новая версия")
                val body = json.optString("body", "")

                val latestRun = tagName.substringAfterLast("-", "0").toIntOrNull() ?: 0
                if (latestRun > AppVersion.CURRENT_RUN_NUMBER) {
                    val prefs = context.getSharedPreferences("spotify_radar_prefs", Context.MODE_PRIVATE)
                    val ignored = prefs.getString("ignored_update_tag", null)
                    if (ignored != tagName) {
                        var downloadUrl = "https://github.com/Kouqqu/spotify-radar/releases/latest"
                        val assets = json.optJSONArray("assets")
                        if (assets != null) {
                            for (i in 0 until assets.length()) {
                                val asset = assets.getJSONObject(i)
                                if (asset.optString("name", "").endsWith(".apk")) {
                                    downloadUrl = asset.optString("browser_download_url", downloadUrl)
                                    break
                                }
                            }
                        }
                        return@withContext UpdateInfo(
                            tag = tagName,
                            title = name,
                            body = body,
                            downloadUrl = downloadUrl
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    fun ignoreVersion(context: Context, tag: String) {
        val prefs = context.getSharedPreferences("spotify_radar_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("ignored_update_tag", tag).apply()
    }
}
