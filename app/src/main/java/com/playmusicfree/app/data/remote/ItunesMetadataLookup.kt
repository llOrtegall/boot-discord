package com.playmusicfree.app.data.remote

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import org.json.JSONArray
import org.json.JSONObject

object ItunesMetadataLookup {

    data class MetadataResult(
        val title: String?,
        val artist: String?,
        val album: String?,
        val artworkUrl: String?
    )

    private val editionKeywords = setOf("deluxe", "edition", "remaster", "remastered", "live")
    private val riskyAlbumKeywords = setOf(
        "soundtrack",
        "motion picture",
        "karaoke",
        "tribute",
        "kids",
        "disney"
    )

    fun searchSong(title: String, artist: String): MetadataResult? {
        val cleanTitle = title.cleanTerm()
        val cleanArtist = artist.cleanTerm()
        val queries = buildQueries(cleanTitle, cleanArtist)
        if (queries.isEmpty()) return null

        val allCandidates = mutableListOf<MetadataResult>()
        for (query in queries) {
            allCandidates += searchItunes(query)
            allCandidates += searchDeezer(query)
            allCandidates += searchMusicBrainz(cleanTitle, cleanArtist, query)
        }

        return selectBestCandidate(cleanTitle, cleanArtist, allCandidates)
    }

    private fun searchItunes(query: String): List<MetadataResult> {
        val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
        val url = "https://itunes.apple.com/search?term=$encoded&entity=song&limit=8"
        val payload = httpGet(url) ?: return emptyList()

        val root = JSONObject(payload)
        val results = root.optJSONArray("results") ?: return emptyList()
        return results.toMetadataResults { item ->
            val rawArtwork = item.optString("artworkUrl100").normalize()
            val artwork = rawArtwork?.replace("100x100bb", "600x600bb")
            MetadataResult(
                title = item.optString("trackName").normalize(),
                artist = item.optString("artistName").normalize(),
                album = item.optString("collectionName").normalize(),
                artworkUrl = artwork
            )
        }
    }

    private fun searchDeezer(query: String): List<MetadataResult> {
        val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
        val payload = httpGet("https://api.deezer.com/search?q=$encoded&limit=8") ?: return emptyList()

        val root = JSONObject(payload)
        val data = root.optJSONArray("data") ?: return emptyList()
        return data.toMetadataResults { item ->
            val albumObj = item.optJSONObject("album")
            val artwork = albumObj?.optString("cover_xl").normalize()
                ?: albumObj?.optString("cover_big").normalize()
            MetadataResult(
                title = item.optString("title").normalize(),
                artist = item.optJSONObject("artist")?.optString("name").normalize(),
                album = albumObj?.optString("title").normalize(),
                artworkUrl = artwork?.replace("http://", "https://")
            )
        }
    }

    private fun searchMusicBrainz(
        cleanTitle: String,
        cleanArtist: String,
        query: String
    ): List<MetadataResult> {
        if (cleanTitle.isBlank() && cleanArtist.isBlank()) return emptyList()

        val mbQuery = if (cleanTitle.isNotBlank() && cleanArtist.isNotBlank()) {
            "recording:\"$cleanTitle\" AND artist:\"$cleanArtist\""
        } else {
            "recording:\"$query\""
        }
        val encoded = URLEncoder.encode(mbQuery, Charsets.UTF_8.name())
        val url = "https://musicbrainz.org/ws/2/recording/?query=$encoded&fmt=json&limit=5&inc=releases+artist-credits"
        val payload = httpGet(
            url = url,
            userAgent = "PlayMusicFree/1.0 (metadata lookup)"
        ) ?: return emptyList()

        val root = JSONObject(payload)
        val recordings = root.optJSONArray("recordings") ?: return emptyList()
        return recordings.toMetadataResults { recording ->
            val artistCredit = recording.optJSONArray("artist-credit")
            val artistName = artistCredit
                ?.optJSONObject(0)
                ?.optString("name")
                .normalize()

            val releases = recording.optJSONArray("releases")
            val release = releases?.optJSONObject(0)
            val releaseTitle = release?.optString("title").normalize()
            val releaseId = release?.optString("id").normalize()
            val coverUrl = releaseId?.let { "https://coverartarchive.org/release/$it/front-500" }

            MetadataResult(
                title = recording.optString("title").normalize(),
                artist = artistName,
                album = releaseTitle,
                artworkUrl = coverUrl
            )
        }
    }

    private fun selectBestCandidate(
        sourceTitle: String,
        sourceArtist: String,
        candidates: List<MetadataResult>
    ): MetadataResult? {
        if (candidates.isEmpty()) return null

        val sourceBase = "$sourceTitle $sourceArtist".lowercase()
        val deduped = LinkedHashMap<String, MetadataResult>()
        candidates.forEach { candidate ->
            val key = listOf(candidate.title, candidate.artist, candidate.album, candidate.artworkUrl)
                .joinToString("|") { it.orEmpty().lowercase() }
            deduped.putIfAbsent(key, candidate)
        }

        val scored = deduped.values
            .map { candidate ->
                val titleScore = similarity(sourceTitle, candidate.title.orEmpty().cleanTerm())
                val artistScore = if (sourceArtist.isBlank()) 0.5f else similarity(
                    sourceArtist,
                    candidate.artist.orEmpty().cleanTerm()
                )
                val metadataBonus = if (!candidate.artworkUrl.isNullOrBlank()) 0.06f else 0f
                val editionPenalty = candidate.editionPenalty(sourceBase)
                val albumPenalty = candidate.albumPenalty(sourceBase)
                val score = (titleScore * 0.72f) + (artistScore * 0.28f) + metadataBonus - editionPenalty - albumPenalty
                ScoredCandidate(
                    candidate = candidate,
                    score = score,
                    titleScore = titleScore
                )
            }
            .sortedByDescending { it.score }

        val best = scored.firstOrNull() ?: return null
        val requiredScore = if (sourceArtist.isBlank()) 0.64f else 0.50f
        val requiredTitleScore = if (sourceTitle.isBlank()) 0f else 0.48f

        return if (best.score >= requiredScore && best.titleScore >= requiredTitleScore) {
            best.candidate
        } else {
            null
        }
    }

    private fun MetadataResult.editionPenalty(sourceBase: String): Float {
        val combined = listOf(title, album).joinToString(" ").lowercase()
        val hasUnexpectedEditionWord = editionKeywords.any { keyword ->
            keyword in combined && keyword !in sourceBase
        }
        return if (hasUnexpectedEditionWord) 0.22f else 0f
    }

    private fun MetadataResult.albumPenalty(sourceBase: String): Float {
        val albumLower = album.orEmpty().lowercase()
        if (albumLower.isBlank()) return 0f

        val hasRiskyKeyword = riskyAlbumKeywords.any { keyword ->
            keyword in albumLower && keyword !in sourceBase
        }
        return if (hasRiskyKeyword) 0.16f else 0f
    }

    private fun similarity(a: String, b: String): Float {
        val left = a.normalizeForMatch()
        val right = b.normalizeForMatch()
        if (left.isBlank() || right.isBlank()) return 0f
        if (left == right) return 1f

        val leftTokens = left.split(' ').filter { it.isNotBlank() }.toSet()
        val rightTokens = right.split(' ').filter { it.isNotBlank() }.toSet()
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) return 0f

        val intersection = leftTokens.intersect(rightTokens).size.toFloat()
        val union = leftTokens.union(rightTokens).size.toFloat()
        return intersection / union
    }

    private fun httpGet(
        url: String,
        userAgent: String = "PlayMusicFree/1.0"
    ): String? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7000
            readTimeout = 7000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", userAgent)
        }

        return try {
            if (connection.responseCode !in 200..299) return null
            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun buildQueries(title: String, artist: String): List<String> {
        val queries = LinkedHashSet<String>()
        if (title.isNotBlank() && artist.isNotBlank()) queries.add("$title $artist")
        if (title.isNotBlank()) queries.add(title)
        if (artist.isNotBlank()) queries.add(artist)
        return queries.toList()
    }

    private fun JSONArray.toMetadataResults(
        mapper: (JSONObject) -> MetadataResult
    ): List<MetadataResult> {
        val list = mutableListOf<MetadataResult>()
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            val mapped = mapper(item)
            if (mapped.hasUsefulMetadata()) list += mapped
        }
        return list
    }

    private fun MetadataResult.hasUsefulMetadata(): Boolean {
        return !title.isNullOrBlank() ||
            !artist.isNullOrBlank() ||
            !album.isNullOrBlank() ||
            !artworkUrl.isNullOrBlank()
    }

    private fun String.cleanTerm(): String {
        val value = normalize().orEmpty()
        if (value.isBlank()) return ""
        return when (value.lowercase()) {
            "unknown", "unknown artist", "unknown album" -> ""
            else -> value
        }
    }

    private fun String?.normalize(): String? = this?.trim()?.ifBlank { null }

    private fun String.normalizeForMatch(): String {
        return lowercase()
            .replace(Regex("\\([^)]*\\)"), " ")
            .replace(Regex("\\[[^]]*]"), " ")
            .replace("&", " ")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private data class ScoredCandidate(
        val candidate: MetadataResult,
        val score: Float,
        val titleScore: Float
    )
}
