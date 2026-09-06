package com.spotifyradar.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom

data class SpotifyUser(
    val name: String,
    val avatarUrl: String?
)

data class SpotifyPlaylist(
    val id: String,
    val name: String,
    val trackCount: Int,
    val imageUrl: String?
)

object SpotifyAuthManager {
    const val CLIENT_ID = "10a760e2d5df4b3f81303d4bdd74ddff"
    const val REDIRECT_URI = "spotifyradar://callback"
    const val SCOPES = "playlist-read-private playlist-read-collaborative user-library-read"

    private const val PREFS = "spotify_radar_auth"
    private const val KEY_TOKEN = "access_token"
    private const val KEY_VERIFIER = "code_verifier"

    fun getSavedToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_TOKEN, null)
    }

    fun saveToken(context: Context, token: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun logout(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_TOKEN).remove(KEY_VERIFIER).apply()
    }

    fun startAuth(context: Context) {
        val verifier = generateCodeVerifier()
        val challenge = generateCodeChallenge(verifier)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_VERIFIER, verifier)
            .apply()

        val authUrl = Uri.parse("https://accounts.spotify.com/authorize").buildUpon()
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("scope", SCOPES)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("show_dialog", "true")
            .build()

        val intent = Intent(Intent.ACTION_VIEW, authUrl)
        context.startActivity(intent)
    }

    suspend fun exchangeCodeForToken(context: Context, code: String): String? = withContext(Dispatchers.IO) {
        try {
            val verifier = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_VERIFIER, "") ?: ""

            val url = URL("https://accounts.spotify.com/api/token")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.doOutput = true

            val postParams = "client_id=$CLIENT_ID" +
                    "&grant_type=authorization_code" +
                    "&code=" + URLEncoder.encode(code, "UTF-8") +
                    "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, "UTF-8") +
                    "&code_verifier=" + URLEncoder.encode(verifier, "UTF-8")

            conn.outputStream.use { it.write(postParams.toByteArray(Charsets.UTF_8)) }

            if (conn.responseCode == 200) {
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val token = json.optString("access_token")
                if (token.isNotBlank()) {
                    saveToken(context, token)
                    return@withContext token
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    suspend fun fetchCurrentUser(token: String): SpotifyUser? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.spotify.com/v1/me")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("Authorization", "Bearer $token")
            if (conn.responseCode == 200) {
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val name = json.optString("display_name", json.optString("id", "Пользователь"))
                val images = json.optJSONArray("images")
                var avatarUrl: String? = null
                if (images != null && images.length() > 0) {
                    avatarUrl = images.getJSONObject(0).optString("url")
                }
                return@withContext SpotifyUser(name, avatarUrl)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    suspend fun fetchUserPlaylists(token: String): List<SpotifyPlaylist> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SpotifyPlaylist>()
        try {
            val url = URL("https://api.spotify.com/v1/me/playlists?limit=50")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("Authorization", "Bearer $token")
            if (conn.responseCode == 200) {
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val items = json.optJSONArray("items")
                if (items != null) {
                    for (i in 0 until items.length()) {
                        val item = items.optJSONObject(i) ?: continue
                        val id = item.optString("id")
                        val name = item.optString("name", "Без названия")
                        val tracksObj = item.optJSONObject("tracks")
                        val count = tracksObj?.optInt("total", 0) ?: 0
                        val images = item.optJSONArray("images")
                        val img = if (images != null && images.length() > 0) images.getJSONObject(0).optString("url") else null
                        list.add(SpotifyPlaylist(id, name, count, img))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    suspend fun fetchPlaylistTracks(token: String, playlistId: String): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Pair<String, String>>()
        try {
            var urlStr: String? = "https://api.spotify.com/v1/playlists/$playlistId/tracks?limit=100"
            while (urlStr != null && tracks.size < 500) {
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("Authorization", "Bearer $token")
                if (conn.responseCode == 200) {
                    val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                    val items = json.optJSONArray("items")
                    if (items != null) {
                        for (i in 0 until items.length()) {
                            val item = items.optJSONObject(i) ?: continue
                            val trackObj = item.optJSONObject("track") ?: continue
                            val title = trackObj.optString("name", "Без названия")
                            val artistsArr = trackObj.optJSONArray("artists")
                            val artistNames = mutableListOf<String>()
                            if (artistsArr != null) {
                                for (j in 0 until artistsArr.length()) {
                                    val art = artistsArr.getJSONObject(j)
                                    artistNames.add(art.optString("name"))
                                }
                            }
                            val artistLine = artistNames.joinToString(", ")
                            tracks.add(Pair(title, artistLine))
                        }
                    }
                    urlStr = if (json.has("next") && !json.isNull("next")) json.getString("next") else null
                } else {
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        tracks
    }

    private fun generateCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val code = ByteArray(64)
        secureRandom.nextBytes(code)
        return Base64.encodeToString(code, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun generateCodeChallenge(verifier: String): String {
        val bytes = verifier.toByteArray(Charsets.US_ASCII)
        val md = MessageDigest.getInstance("SHA-256")
        md.update(bytes, 0, bytes.size)
        val digest = md.digest()
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
