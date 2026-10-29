package app.klyra.shared

import kotlinx.serialization.Serializable

@Serializable
enum class Source { LOCAL, AUDIUS, YOUTUBE, MUSICBRAINZ }

@Serializable
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationMs: Long = 0,
    val artwork: String? = null,
    val source: Source,
    val sourceId: String = id,
    val uri: String? = null,
    val permalink: String? = null,
    val artistId: String? = null,
    val genre: String = "",
    val streamable: Boolean = false,
    val downloadable: Boolean = false,
    val downloadUri: String? = null
) {
    val nativePlayable get() = streamable && source in setOf(Source.LOCAL, Source.AUDIUS)
    val offline get() = source == Source.LOCAL
    val capabilityLabel get() = when {
        offline -> "On this device"
        nativePlayable -> "Audius · Full track"
        source == Source.YOUTUBE -> "Watch on YouTube"
        else -> "Metadata only"
    }
    fun cloudSafe() = copy(uri = if (source == Source.LOCAL) null else uri, downloadUri = null,
        artwork = artwork?.takeUnless { it.startsWith("content:") || it.startsWith("file:") })
}

@Serializable
data class Playlist(val id: String, val name: String, val description: String = "", val trackIds: List<String> = emptyList(), val cover: String? = null)
@Serializable
data class ListeningEvent(val track: Track, val startedAt: Long, val listenedMs: Long)
@Serializable
data class Preferences(
    val appearance: String = "system", val reducedMotion: Boolean = false,
    val lyricsOffsetMs: Long = 0, val lyricsSize: Float = 32f,
    val lyricsIntensity: Float = 0.55f, val historyEnabled: Boolean = true,
    val genres: List<String> = emptyList(), val displayName: String = "",
    val minimizeToTray: Boolean = true
)
@Serializable
data class QueueSnapshot(val tracks: List<Track> = emptyList(), val index: Int = 0, val positionMs: Long = 0, val shuffle: Boolean = false, val repeat: Int = 0)
@Serializable
data class LyricWord(val timeMs: Long, val text: String)
@Serializable
data class LyricLine(val timeMs: Long, val text: String, val words: List<LyricWord> = emptyList())
@Serializable
data class Lyrics(val lines: List<LyricLine> = emptyList(), val plain: String = "", val instrumental: Boolean = false, val attribution: String = "LRCLIB")
@Serializable
data class CloudRecord(val id: String, val kind: String, val payload: String, val revision: Long = 0, val deleted: Boolean = false)
@Serializable
data class Session(val accessToken: String, val refreshToken: String, val userId: String, val email: String, val expiresAt: Long)
data class ArtistInfo(val name: String, val biography: String, val url: String?, val similar: List<String> = emptyList())
data class AlbumInfo(val id: String, val title: String, val artist: String, val tracks: List<Track>)

interface MusicProvider {
    suspend fun search(query: String): List<Track>
    suspend fun trending(genre: String? = null): List<Track>
    suspend fun artistTracks(id: String): List<Track>
    suspend fun resolve(track: Track): Track
}

object LrcParser {
    private val stamps = Regex("\\[(\\d+):(\\d{2})(?:[.:](\\d{1,3}))?]")
    private val wordStamps = Regex("<(\\d+):(\\d{2})(?:[.:](\\d{1,3}))?>")
    private fun time(match: MatchResult): Long {
        val fraction = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0
        return match.groupValues[1].toLong() * 60_000 + match.groupValues[2].toLong() * 1000 + fraction
    }
    fun parse(input: String): List<LyricLine> {
        val offset = Regex("\\[offset:([+-]?\\d+)]").find(input)?.groupValues?.get(1)?.toLongOrNull() ?: 0
        return input.lineSequence().flatMap { raw ->
            val matches = stamps.findAll(raw).toList()
            val body = raw.replace(stamps, "").trim()
            val words = wordStamps.findAll(body).toList().mapIndexed { i, match ->
                val next = wordStamps.findAll(body).toList().getOrNull(i + 1)?.range?.first ?: body.length
                LyricWord((time(match) + offset).coerceAtLeast(0), body.substring(match.range.last + 1, next))
            }
            matches.map { LyricLine((time(it) + offset).coerceAtLeast(0), body.replace(wordStamps, ""), words) }
        }.sortedBy { it.timeMs }.toList()
    }
    fun activeIndex(lines: List<LyricLine>, positionMs: Long): Int {
        var low = 0; var high = lines.lastIndex; var answer = -1
        while (low <= high) { val mid = (low + high) / 2; if (lines[mid].timeMs <= positionMs) { answer = mid; low = mid + 1 } else high = mid - 1 }
        return answer
    }
}

object Recommendations {
    fun rank(candidates: List<Track>, liked: List<Track>, events: List<ListeningEvent>, genres: List<String>): List<Track> {
        val artistScores = (liked.map { it.artist to 5L } + events.map { it.track.artist to (it.listenedMs / 30_000).coerceAtMost(5) }).groupBy({ it.first }, { it.second }).mapValues { it.value.sum() }
        val recent = events.takeLast(10).map { it.track.id }.toSet()
        return candidates.distinctBy { it.id }.sortedByDescending {
            (artistScores[it.artist] ?: 0) + (if (it.genre in genres) 5 else 0) - (if (it.id in recent) 3 else 0)
        }
    }
}
