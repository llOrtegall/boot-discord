package com.playmusicfree.app.data.remote

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.Normalizer
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

    private data class Candidate(
        val source: Source,
        val data: MetadataResult
    )

    private data class ScoredCandidate(
        val candidate: Candidate,
        val score: Float,
        val titleScore: Float,
        val artistScore: Float
    )

    private data class ParsedSongHint(
        val title: String,
        val artist: String
    )

    private enum class Source(val weight: Float) {
        ITUNES(0.08f),
        DEEZER(0.08f),
        AUDIODB(0.06f),
        MUSICBRAINZ(0.04f)
    }

    private val noisyKeywords = setOf(
        "remix", "mixed", "live", "karaoke", "tribute", "sped up", "nightcore"
    )
    private val riskyAlbumKeywords = setOf(
        "soundtrack", "motion picture", "disney", "kids", "tribute", "karaoke"
    )

    fun searchSong(title: String, artist: String): MetadataResult? {
        val cleanTitle = title.cleanInput()
        val cleanArtist = artist.cleanInput()
        val parsed = parseSongHint(cleanTitle, cleanArtist)

        val searchTitle = parsed.title
        val searchArtist = parsed.artist
        if (searchTitle.isBlank() && searchArtist.isBlank()) return null

        val queries = buildQueries(searchTitle, searchArtist)
        val candidates = mutableListOf<Candidate>()

        for (query in queries) {
            candidates += searchItunes(query)
            candidates += searchDeezer(query)
            candidates += searchMusicBrainz(searchTitle, searchArtist, query)
        }
        candidates += searchAudioDb(searchTitle, searchArtist)

        return selectBestCandidate(searchTitle, searchArtist, candidates)
    }

    private fun searchItunes(query: String): List<Candidate> {
        val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
        val payload = httpGet("https://itunes.apple.com/search?term=$encoded&entity=song&limit=10")
            ?: return emptyList()

        val root = JSONObject(payload)
        val results = root.optJSONArray("results") ?: return emptyList()
        return results.toCandidates(Source.ITUNES) { item ->
            val rawArtwork = item.optString("artworkUrl100").normalize()
            MetadataResult(
                title = item.optString("trackName").normalize(),
                artist = item.optString("artistName").normalize(),
                album = item.optString("collectionName").normalize(),
                artworkUrl = rawArtwork
                    ?.replace("100x100bb", "600x600bb")
                    ?.replace("http://", "https://")
            )
        }
    }

    private fun searchDeezer(query: String): List<Candidate> {
        val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
        val payload = httpGet("https://api.deezer.com/search?q=$encoded&limit=10")
            ?: return emptyList()

        val root = JSONObject(payload)
        val data = root.optJSONArray("data") ?: return emptyList()
        return data.toCandidates(Source.DEEZER) { item ->
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

    private fun searchAudioDb(title: String, artist: String): List<Candidate> {
        if (title.isBlank() || artist.isBlank()) return emptyList()

        val encodedTitle = URLEncoder.encode(title, Charsets.UTF_8.name())
        val encodedArtist = URLEncoder.encode(artist, Charsets.UTF_8.name())
        val payload = httpGet(
            "https://www.theaudiodb.com/api/v1/json/2/searchtrack.php?s=$encodedArtist&t=$encodedTitle"
        ) ?: return emptyList()

        val root = JSONObject(payload)
        val tracks = root.optJSONArray("track") ?: return emptyList()
        return tracks.toCandidates(Source.AUDIODB) { item ->
            val artwork = item.optString("strTrackThumb").normalize()
                ?.replace("http://", "https://")
            MetadataResult(
                title = item.optString("strTrack").normalize(),
                artist = item.optString("strArtist").normalize(),
                album = item.optString("strAlbum").normalize(),
                artworkUrl = artwork
            )
        }
    }

    private fun searchMusicBrainz(
        sourceTitle: String,
        sourceArtist: String,
        query: String
    ): List<Candidate> {
        if (sourceTitle.isBlank() && sourceArtist.isBlank()) return emptyList()

        val mbQuery = when {
            sourceTitle.isNotBlank() && sourceArtist.isNotBlank() ->
                "recording:\"$sourceTitle\" AND artist:\"$sourceArtist\""
            else -> "recording:\"$query\""
        }
        val encoded = URLEncoder.encode(mbQuery, Charsets.UTF_8.name())
        val url =
            "https://musicbrainz.org/ws/2/recording/?query=$encoded&fmt=json&limit=8&inc=releases+artist-credits"
        val payload = httpGet(
            url = url,
            userAgent = "PlayMusicFree/1.0 (metadata lookup)"
        ) ?: return emptyList()

        val root = JSONObject(payload)
        val recordings = root.optJSONArray("recordings") ?: return emptyList()
        return recordings.toCandidates(Source.MUSICBRAINZ) { recording ->
            val artistCredit = recording.optJSONArray("artist-credit")
            val artistName = artistCredit
                ?.optJSONObject(0)
                ?.optString("name")
                .normalize()

            val release = recording.optJSONArray("releases")?.optJSONObject(0)
            val album = release?.optString("title").normalize()
            val releaseId = release?.optString("id").normalize()
            MetadataResult(
                title = recording.optString("title").normalize(),
                artist = artistName,
                album = album,
                artworkUrl = releaseId
                    ?.let { "https://coverartarchive.org/release/$it/front-500" }
            )
        }
    }

    private fun selectBestCandidate(
        sourceTitle: String,
        sourceArtist: String,
        candidates: List<Candidate>
    ): MetadataResult? {
        if (candidates.isEmpty()) return null

        val sourceTitleNorm = sourceTitle.normalizeForMatch()
        val sourceArtistNorm = sourceArtist.normalizeForMatch()
        val sourceBase = "$sourceTitleNorm $sourceArtistNorm"

        val deduped = LinkedHashMap<String, Candidate>()
        candidates.forEach { candidate ->
            val key = listOf(
                candidate.data.title,
                candidate.data.artist,
                candidate.data.album,
                candidate.data.artworkUrl
            ).joinToString("|") { it.orEmpty().normalizeForMatch() }
            deduped.putIfAbsent(key, candidate)
        }

        val scored = deduped.values
            .map { candidate ->
                val titleScore = similarity(sourceTitleNorm, candidate.data.title.orEmpty().normalizeForMatch())
                val artistScore = if (sourceArtistNorm.isBlank()) {
                    0.45f
                } else {
                    similarity(sourceArtistNorm, candidate.data.artist.orEmpty().normalizeForMatch())
                }
                val exactTitleBonus = if (titleScore >= 0.95f) 0.18f else 0f
                val exactArtistBonus = if (artistScore >= 0.95f) 0.10f else 0f
                val artworkBonus = if (!candidate.data.artworkUrl.isNullOrBlank()) 0.09f else 0f
                val sourceBonus = candidate.source.weight
                val titlePenalty = candidate.titlePenalty(sourceBase)
                val albumPenalty = candidate.albumPenalty(sourceBase)
                val score = (titleScore * 0.65f) +
                    (artistScore * 0.25f) +
                    exactTitleBonus +
                    exactArtistBonus +
                    artworkBonus +
                    sourceBonus -
                    titlePenalty -
                    albumPenalty

                ScoredCandidate(
                    candidate = candidate,
                    score = score,
                    titleScore = titleScore,
                    artistScore = artistScore
                )
            }
            .sortedByDescending { it.score }

        val best = scored.firstOrNull() ?: return null
        val minTitle = if (sourceTitleNorm.isBlank()) 0f else 0.33f
        val minArtist = if (sourceArtistNorm.isBlank()) 0f else 0.18f

        if (best.titleScore >= minTitle && best.artistScore >= minArtist && best.score >= 0.41f) {
            return best.candidate.data
        }

        // Soft fallback: prefer candidates with artwork and decent title match.
        val soft = scored.firstOrNull {
            !it.candidate.data.artworkUrl.isNullOrBlank() && it.titleScore >= 0.30f
        }
        return soft?.candidate?.data
    }

    private fun Candidate.titlePenalty(sourceBase: String): Float {
        val titleNorm = data.title.orEmpty().normalizeForMatch()
        val hasUnexpectedNoisyKeyword = noisyKeywords.any { key ->
            key in titleNorm && key !in sourceBase
        }
        return if (hasUnexpectedNoisyKeyword) 0.28f else 0f
    }

    private fun Candidate.albumPenalty(sourceBase: String): Float {
        val albumNorm = data.album.orEmpty().normalizeForMatch()
        if (albumNorm.isBlank()) return 0f

        val hasRiskyKeyword = riskyAlbumKeywords.any { key ->
            key in albumNorm && key !in sourceBase
        }
        return if (hasRiskyKeyword) 0.16f else 0f
    }

    private fun parseSongHint(title: String, artist: String): ParsedSongHint {
        var cleanTitle = title
            .replace(Regex("^\\d+\\s*[-._)]\\s*"), "")
            .replace(Regex("^track\\s*\\d+\\s*[-._)]\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        var cleanArtist = artist

        // Common filename pattern: "Artist - Title"
        if (cleanArtist.isBlank() && cleanTitle.contains(" - ")) {
            val parts = cleanTitle.split(" - ", limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                cleanArtist = parts[0].trim()
                cleanTitle = parts[1].trim()
            }
        }

        return ParsedSongHint(
            title = cleanTitle.cleanInput(),
            artist = cleanArtist.cleanInput()
        )
    }

    private fun similarity(left: String, right: String): Float {
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
            connectTimeout = 8000
            readTimeout = 8000
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

    private fun JSONArray.toCandidates(
        source: Source,
        mapper: (JSONObject) -> MetadataResult
    ): List<Candidate> {
        val list = mutableListOf<Candidate>()
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            val mapped = mapper(item)
            if (mapped.hasUsefulMetadata()) {
                list += Candidate(source = source, data = mapped)
            }
        }
        return list
    }

    private fun MetadataResult.hasUsefulMetadata(): Boolean {
        return !title.isNullOrBlank() ||
            !artist.isNullOrBlank() ||
            !album.isNullOrBlank() ||
            !artworkUrl.isNullOrBlank()
    }

    private fun String.cleanInput(): String {
        val value = normalize().orEmpty()
        if (value.isBlank()) return ""
        return when (value.lowercase()) {
            "unknown", "unknown artist", "unknown album", "<unknown>" -> ""
            else -> value
        }
    }

    private fun String?.normalize(): String? = this?.trim()?.ifBlank { null }

    private fun String.normalizeForMatch(): String {
        if (isBlank()) return ""
        val ascii = Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")

        return ascii.lowercase()
            .replace(Regex("\\([^)]*\\)"), " ")
            .replace(Regex("\\[[^]]*]"), " ")
            .replace(Regex("\\b(ft|feat|featuring)\\b.*"), " ")
            .replace("&", " ")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
