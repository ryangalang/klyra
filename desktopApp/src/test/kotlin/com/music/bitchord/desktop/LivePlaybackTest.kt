package com.music.bitchord.desktop

import com.music.bitchord.data.innertube.InnerTubeXResolver
import com.music.bitchord.data.innertube.potoken.PoTokenGenerator
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.Song
import java.nio.file.Files
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assume.assumeTrue
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.test.assertNull

/** Opt-in tests use actual providers/audio output (muted), never fake stream URLs. */
class LivePlaybackTest {
    private fun enabled() = assumeTrue(System.getenv("KLYRA_LIVE_TEST") == "1")

    @Test fun mainstreamCatalogAndNativePlayback() = runBlocking {
        enabled()
        InnerTubeXResolver.init(
            filesDir = DesktopMediaCache.directory.toFile(),
            store = DesktopInnerTubeXStore,
            poTokenProvider = PoTokenGenerator(
                createMinter = { DesktopPoTokenWebView.getNewPoTokenGenerator() },
                available = { DesktopPoTokenWebView.available },
            ).asTokenProvider(),
        )
        val songs = mutableListOf<Song>()
        for (query in listOf("BINI Pantropiko", "BLACKPINK Lovesick Girls", "Bruno Mars Just The Way You Are", "Eraserheads With A Smile")) {
            val results = withTimeout(60_000) { DesktopSearchClient.search(query).getOrThrow() }
            val tracks = results.filterIsInstance<SearchResult.Track>().map { it.song }
            assertTrue(tracks.isNotEmpty(), "No song results for $query")
            println("CATALOG: $query -> ${tracks.size} tracks; first=${tracks.first().title} / ${tracks.first().artist}")
            songs += tracks.first()
        }
        withTimeout(240_000) { exercisePlayer(songs.first(), minimumDuration = 60_000) }
    }

    @Test fun localFallbackPlaysSeeksAndPauses() = runBlocking {
        enabled()
        val file = Files.createTempFile("klyra-local-playback-", ".wav")
        val rate = 22050
        val samples = rate * 20
        val bytes = ByteBuffer.allocate(44 + samples * 2).order(ByteOrder.LITTLE_ENDIAN)
        bytes.put("RIFF".toByteArray()).putInt(36 + samples * 2).put("WAVEfmt ".toByteArray())
        bytes.putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
        bytes.put("data".toByteArray()).putInt(samples * 2)
        repeat(samples) { bytes.putShort((kotlin.math.sin(it * 2.0 * Math.PI * 220 / rate) * 1000).toInt().toShort()) }
        Files.write(file, bytes.array())
        try {
            val song = Song("local:klyra-fixture", "KLYRA local test", "Generated test signal", null, localUri = file.toUri().toString())
            withTimeout(60_000) { exercisePlayer(song, minimumDuration = 19_000) }
        } finally { Files.deleteIfExists(file) }
    }

    private suspend fun exercisePlayer(song: Song, minimumDuration: Long) {
        val engine = DesktopPlaybackEngine(onEnded = {})
        try {
            engine.setVolume(0f)
            engine.load(song)
            while (engine.state.value.positionMs < 800) {
                assertNull(engine.state.value.error, "Playback failed")
                delay(100)
            }
            assertTrue(engine.state.value.durationMs >= minimumDuration, "Expected full track duration")
            println("PLAYBACK: ${song.title}; durationMs=${engine.state.value.durationMs}; clock=${engine.state.value.positionMs}; source=${engine.state.value.streamSourceId}")
            engine.seekTo(8_000)
            while (engine.state.value.awaitingAudio || engine.state.value.positionMs < 8_500) {
                assertNull(engine.state.value.error)
                delay(100)
            }
            engine.pause()
            while (engine.state.value.isPlaying) delay(50)
            val paused = engine.state.value.positionMs
            delay(400)
            assertTrue(kotlin.math.abs(engine.state.value.positionMs - paused) < 100)
            engine.play()
            while (engine.state.value.positionMs < paused + 400) delay(100)
            println("TRANSPORT: seek, pause and resume passed")
        } finally { engine.shutdown(5_000) }
    }
}
