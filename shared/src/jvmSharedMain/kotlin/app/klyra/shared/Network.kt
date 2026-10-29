package app.klyra.shared

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

val JsonCodec = Json { ignoreUnknownKeys = true; encodeDefaults = true }
fun JsonObject.string(key: String): String = this[key]?.jsonPrimitive?.contentOrNull.orEmpty()
fun JsonObject.bool(key: String): Boolean = this[key]?.jsonPrimitive?.booleanOrNull ?: false
fun JsonObject.number(key: String): Long = this[key]?.jsonPrimitive?.let { it.longOrNull ?: it.doubleOrNull?.toLong() } ?: 0

class HttpApi {
    val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS).retryOnConnectionFailure(true).build()
    suspend fun request(url: String, params: Map<String, String> = emptyMap(), method: String = "GET", body: JsonElement? = null, headers: Map<String, String> = emptyMap()): JsonElement = withContext(Dispatchers.IO) {
        val target = url.toHttpUrl().newBuilder().apply { params.forEach { (k,v) -> addQueryParameter(k,v) } }.build()
        val request = Request.Builder().url(target).header("User-Agent", "KLYRA/0.1 (native music client)").apply {
            headers.forEach { (k,v) -> header(k,v) }
            if (method != "GET") method(method, (body?.toString() ?: "{}").toRequestBody("application/json".toMediaType()))
        }.build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = runCatching { JsonCodec.parseToJsonElement(text).jsonObject.let { it.string("msg").ifBlank { it.string("message") }.ifBlank { it.string("error_description") } } }.getOrDefault("")
                throw IOException(message.ifBlank { "Service returned ${response.code}. Please try again." })
            }
            if (text.isBlank()) JsonNull else JsonCodec.parseToJsonElement(text)
        }
    }
}

class AudiusProvider(private val api: HttpApi) : MusicProvider {
    private val base = "https://api.audius.co/v1"
    private fun mapTrack(o: JsonObject): Track {
        val id = o.string("id"); val user = o["user"] as? JsonObject ?: buildJsonObject { }
        val art = o["artwork"] as? JsonObject
        val conditions = o["stream_conditions"]
        val gated = o.bool("is_stream_gated") || (conditions != null && conditions != JsonNull && conditions.toString() != "{}")
        val canStream = o.bool("is_streamable") && !gated
        val canDownload = o.bool("is_downloadable") && !o.bool("is_download_gated") && (o["download_conditions"] == null || o["download_conditions"] == JsonNull)
        return Track("audius:$id", o.string("title"), user.string("name"), durationMs = o.number("duration") * 1000,
            artwork = art?.string("1000x1000")?.ifBlank { art.string("480x480") }, source = Source.AUDIUS, sourceId = id,
            uri = if (canStream) "$base/tracks/$id/stream?app_name=KLYRA" else null,
            permalink = o.string("permalink").let { if (it.startsWith("/")) "https://audius.co$it" else it },
            artistId = user.string("id"), genre = o.string("genre"), streamable = canStream,
            downloadable = canDownload, downloadUri = if (canDownload) "$base/tracks/$id/download?app_name=KLYRA" else null)
    }
    private suspend fun list(path: String, params: Map<String,String> = emptyMap()): List<Track> =
        (api.request("$base/$path", params + mapOf("app_name" to "KLYRA", "limit" to "50")).jsonObject["data"] as? JsonArray).orEmpty().map { mapTrack(it.jsonObject) }
    override suspend fun search(query: String) = list("tracks/search", mapOf("query" to query))
    override suspend fun trending(genre: String?) = list("tracks/trending", genre?.let { mapOf("genre" to it) } ?: emptyMap())
    override suspend fun artistTracks(id: String) = list("users/$id/tracks")
    override suspend fun resolve(track: Track): Track {
        require(track.source == Source.AUDIUS)
        val data = api.request("$base/tracks/${track.sourceId}", mapOf("app_name" to "KLYRA")).jsonObject["data"]
        return mapTrack(when(data) { is JsonArray -> data.first().jsonObject; else -> data!!.jsonObject })
    }
    suspend fun albums(query: String): List<AlbumInfo> {
        val data = api.request("$base/playlists/search", mapOf("query" to query, "app_name" to "KLYRA")).jsonObject["data"] as? JsonArray ?: return emptyList()
        return data.map { it.jsonObject }.filter { it.bool("is_album") }.map { AlbumInfo(it.string("id"), it.string("playlist_name"), (it["user"] as? JsonObject)?.string("name").orEmpty(), emptyList()) }
    }
    suspend fun albumTracks(id: String) = list("playlists/$id/tracks")
}

class LyricsProvider(private val api: HttpApi) {
    suspend fun get(track: Track): Lyrics {
        require(track.durationMs > 0) { "Lyrics matching needs the track duration." }
        val params = mutableMapOf("track_name" to track.title, "artist_name" to track.artist, "duration" to (track.durationMs / 1000).toString())
        if (track.album.isNotBlank()) params["album_name"] = track.album
        val o = api.request("https://lrclib.net/api/get", params).jsonObject
        require(kotlin.math.abs(o.number("duration") - track.durationMs / 1000) <= 3) { "No matching lyrics for this version." }
        return Lyrics(LrcParser.parse(o.string("syncedLyrics")), o.string("plainLyrics"), o.bool("instrumental"))
    }
}

class MetadataProviders(private val api: HttpApi, private val lastFmKey: String, private val youtubeKey: String) {
    private val musicBrainzLock = kotlinx.coroutines.sync.Mutex()
    private var lastMusicBrainzCall = 0L
    suspend fun artist(name: String): ArtistInfo {
        require(lastFmKey.isNotBlank()) { "Artist biographies need a Last.fm API key in this build." }
        val o = api.request("https://ws.audioscrobbler.com/2.0/", mapOf("method" to "artist.getinfo", "artist" to name, "api_key" to lastFmKey, "format" to "json")).jsonObject["artist"]?.jsonObject ?: error("Artist unavailable")
        val similar = ((o["similar"] as? JsonObject)?.get("artist") as? JsonArray).orEmpty().map { it.jsonObject.string("name") }
        return ArtistInfo(name, (o["bio"] as? JsonObject)?.string("summary").orEmpty().replace(Regex("<[^>]+>"), ""), o.string("url"), similar)
    }
    suspend fun musicBrainz(query: String): List<Track> {
        musicBrainzLock.lock()
        try {
        kotlinx.coroutines.delay((1100 - (System.currentTimeMillis() - lastMusicBrainzCall)).coerceAtLeast(0))
        lastMusicBrainzCall = System.currentTimeMillis()
        val data = api.request("https://musicbrainz.org/ws/2/recording/", mapOf("query" to query, "fmt" to "json", "limit" to "25")).jsonObject["recordings"] as? JsonArray ?: return emptyList()
        return data.map { item -> val o = item.jsonObject
            Track("mb:${o.string("id")}", o.string("title"), (o["artist-credit"] as? JsonArray).orEmpty().joinToString { it.jsonObject.string("name") }, durationMs = o.number("length"), source = Source.MUSICBRAINZ, sourceId = o.string("id"), permalink = "https://musicbrainz.org/recording/${o.string("id")}") }
        } finally { musicBrainzLock.unlock() }
    }
    suspend fun youtube(query: String): List<Track> {
        require(youtubeKey.isNotBlank()) { "YouTube search needs a configured API key." }
        val items = api.request("https://www.googleapis.com/youtube/v3/search", mapOf("part" to "snippet", "type" to "video", "q" to query, "key" to youtubeKey, "maxResults" to "25")).jsonObject["items"] as? JsonArray ?: return emptyList()
        return items.map { val o = it.jsonObject; val id = o["id"]!!.jsonObject.string("videoId"); val s = o["snippet"]!!.jsonObject
            Track("youtube:$id", s.string("title"), s.string("channelTitle"), source = Source.YOUTUBE, sourceId = id, artwork = s["thumbnails"]?.jsonObject?.get("high")?.jsonObject?.string("url"), permalink = "https://www.youtube.com/watch?v=$id") }
    }
}
