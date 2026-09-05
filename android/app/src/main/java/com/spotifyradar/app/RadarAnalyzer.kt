package com.spotifyradar.app

object RadarAnalyzer {

    data class MatchedArtist(
        val name: String,
        val rank: Int,
        val tracks: List<String>
    )

    data class IndieArtist(
        val name: String,
        val tracks: List<String>
    )

    data class AnalysisResult(
        val totalTracks: Int,
        val matchedCount: Int,
        val indieCount: Int,
        val chartPercentage: Int,
        val highestArtist: MatchedArtist?,
        val matchedArtists: List<MatchedArtist>,
        val indieArtists: List<IndieArtist>,
        val verdictTitle: String,
        val verdictDesc: String,
        val verdictEmoji: String
    )

    private val SPLIT_REGEX = Regex(
        "[,/&]|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bfeaturing\\b|\\bwith\\b|\\bx\\b",
        RegexOption.IGNORE_CASE
    )

    fun splitArtists(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return raw.split(SPLIT_REGEX).map { it.trim() }.filter { it.isNotBlank() }
    }

    fun analyze(trackEntries: List<Pair<String, String>>): AnalysisResult {
        val matchedMap = mutableMapOf<String, Pair<Int, MutableList<String>>>() // cleaned -> (rank, list of songs)
        val matchedDisplayNames = mutableMapOf<String, String>()
        val indieMap = mutableMapOf<String, MutableList<String>>() // cleaned -> list of songs
        val indieDisplayNames = mutableMapOf<String, String>()

        var chartTracksCount = 0

        for ((trackTitle, artistLine) in trackEntries) {
            val artists = splitArtists(artistLine)
            var hasChartArtist = false

            for (artist in artists) {
                val cleaned = KworbData.clean(artist)
                if (cleaned.isBlank()) continue

                val match = KworbData.ARTIST_MAP[cleaned]
                if (match != null) {
                    hasChartArtist = true
                    val (canonicalName, rank) = match
                    val entry = matchedMap.getOrPut(cleaned) { Pair(rank, mutableListOf()) }
                    if (trackTitle.isNotBlank() && !entry.second.contains(trackTitle)) {
                        entry.second.add(trackTitle)
                    }
                    matchedDisplayNames[cleaned] = canonicalName
                } else {
                    val entry = indieMap.getOrPut(cleaned) { mutableListOf() }
                    if (trackTitle.isNotBlank() && !entry.contains(trackTitle)) {
                        entry.add(trackTitle)
                    }
                    if (!indieDisplayNames.containsKey(cleaned)) {
                        indieDisplayNames[cleaned] = artist
                    }
                }
            }

            if (hasChartArtist) {
                chartTracksCount++
            }
        }

        // Clean out any indie that was also matched
        for (matchedKey in matchedMap.keys) {
            indieMap.remove(matchedKey)
            indieDisplayNames.remove(matchedKey)
        }

        val matchedList = matchedMap.map { (key, pair) ->
            MatchedArtist(
                name = matchedDisplayNames[key] ?: key,
                rank = pair.first,
                tracks = pair.second
            )
        }.sortedBy { it.rank }

        val indieList = indieMap.map { (key, tracks) ->
            IndieArtist(
                name = indieDisplayNames[key] ?: key,
                tracks = tracks
            )
        }.sortedByDescending { it.tracks.size }

        val totalTracks = if (trackEntries.isNotEmpty()) trackEntries.size else matchedList.size + indieList.size
        val chartPercentage = if (totalTracks > 0) ((chartTracksCount.toFloat() / totalTracks) * 100).toInt() else 0
        val highestArtist = matchedList.firstOrNull()

        val (verdictTitle, verdictDesc, verdictEmoji) = getVerdict(chartPercentage)

        return AnalysisResult(
            totalTracks = totalTracks,
            matchedCount = matchedList.size,
            indieCount = indieList.size,
            chartPercentage = chartPercentage,
            highestArtist = highestArtist,
            matchedArtists = matchedList,
            indieArtists = indieList,
            verdictTitle = verdictTitle,
            verdictDesc = verdictDesc,
            verdictEmoji = verdictEmoji
        )
    }

    private fun getVerdict(percentage: Int): Triple<String, String, String> {
        return when {
            percentage >= 70 -> Triple(
                "Мейнстрим-икона",
                "Твой плейлист дышит в унисон с мировыми трендами Spotify. Абсолютный радио-хит!",
                "🔥"
            )
            percentage >= 45 -> Triple(
                "Поп-ценитель",
                "Отличный баланс между мировыми блокбастерами и свежим собственным вкусом.",
                "🎧"
            )
            percentage >= 20 -> Triple(
                "Жанровый гурман",
                "Ты слушаешь чарты выборочно, отдавая приоритет любимой атмосфере и стилю.",
                "💎"
            )
            else -> Triple(
                "Охотник за андеграундом",
                "Мировые топы проходят мимо тебя. Чистый инди-вкус и независимый поиск!",
                "🛸"
            )
        }
    }

    fun parseCsv(csvText: String): List<Pair<String, String>> {
        val lines = csvText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val headerTokens = parseCsvLine(lines[0])
        val trackNameIdx = headerTokens.indexOfFirst {
            it.equals("Track Name", ignoreCase = true) || it.equals("Название", ignoreCase = true)
        }
        val artistNameIdx = headerTokens.indexOfFirst {
            it.equals("Artist Name(s)", ignoreCase = true) ||
            it.equals("Artist Name", ignoreCase = true) ||
            it.equals("Артист", ignoreCase = true) ||
            it.equals("Исполнитель", ignoreCase = true)
        }

        val result = mutableListOf<Pair<String, String>>()

        val startLine = if (trackNameIdx != -1 || artistNameIdx != -1) 1 else 0

        for (i in startLine until lines.size) {
            val tokens = parseCsvLine(lines[i])
            if (tokens.isEmpty()) continue

            val track = if (trackNameIdx != -1 && trackNameIdx < tokens.size) tokens[trackNameIdx] else "Трек #$i"
            val artist = if (artistNameIdx != -1 && artistNameIdx < tokens.size) tokens[artistNameIdx] else tokens[0]

            if (artist.isNotBlank()) {
                result.add(Pair(track, artist))
            }
        }

        return result
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        for (ch in line) {
            when {
                ch == '\"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    tokens.add(sb.toString().trim())
                    sb.clear()
                }
                else -> sb.append(ch)
            }
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

    fun parseText(text: String): List<Pair<String, String>> {
        val items = text.split(Regex("[\n,]")).map { it.trim() }.filter { it.isNotBlank() }
        return items.mapIndexed { idx, artist -> Pair("Трек ${idx + 1}", artist) }
    }
}
