package com.music.bitchord

import android.app.NotificationManager
import android.content.ComponentName
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.music.bitchord.data.model.Song
import com.music.bitchord.playback.PlaybackService
import com.music.bitchord.playback.toMediaItem
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Real Media3 service, decoder, session and notification; no fake player or resolver. */
@UnstableApi
@RunWith(AndroidJUnit4::class)
class KlyraPlaybackInstrumentedTest {
    @Test fun localPlaybackSurvivesBackgroundSeekAndControllerReconnect() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val fixture = File(context.cacheDir, "klyra-playback-test.wav")
        val pcm = ByteArray(22050 * 2 * 40)
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            .put("RIFF".toByteArray()).putInt(36 + pcm.size).put("WAVEfmt ".toByteArray())
            .putInt(16).putShort(1).putShort(1).putInt(22050).putInt(44100).putShort(2).putShort(16)
            .put("data".toByteArray()).putInt(pcm.size).array()
        fixture.outputStream().use { it.write(header); it.write(pcm) }
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        var controller: MediaController? = null
        suspend fun connect(): MediaController {
            val future = withContext(Dispatchers.Main) {
                MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java))).buildAsync()
            }
            return withContext(Dispatchers.IO) { future.get(20, TimeUnit.SECONDS) }
        }
        suspend fun awaitState(check: (MediaController) -> Boolean) = withTimeout(30_000) {
            while (!withContext(Dispatchers.Main) {
                val active = checkNotNull(controller)
                active.playerError?.let { throw AssertionError("Media3 playback failed: ${it.errorCodeName}", it) }
                check(active)
            }) delay(100)
        }
        try {
            controller = connect()
            withContext(Dispatchers.Main) {
                controller!!.volume = 0f
                val song = Song(videoId = "local:klyra-fixture", title = "KLYRA playback test", artist = "Local fixture", thumbnailUrl = null, localUri = Uri.fromFile(fixture).toString())
                controller!!.setMediaItem(song.toMediaItem())
                controller!!.prepare()
                controller!!.play()
            }
            awaitState { it.playbackState == Player.STATE_READY && it.currentPosition > 800 }
            scenario.moveToState(Lifecycle.State.CREATED)
            awaitState { it.isPlaying && it.currentPosition > 1800 }
            withContext(Dispatchers.Main) { controller!!.seekTo(10_000) }
            awaitState { it.isPlaying && it.currentPosition > 10_500 }
            withContext(Dispatchers.Main) { controller!!.pause() }
            awaitState { !it.isPlaying }
            val paused = withContext(Dispatchers.Main) { controller!!.currentPosition }
            delay(400)
            assertTrue("Paused clock drifted", kotlin.math.abs(withContext(Dispatchers.Main) { controller!!.currentPosition } - paused) < 100)
            withContext(Dispatchers.Main) { controller!!.release() }
            controller = connect()
            assertEquals("local:klyra-fixture", withContext(Dispatchers.Main) { controller!!.currentMediaItem?.mediaId })
            withContext(Dispatchers.Main) { controller!!.play() }
            awaitState { it.isPlaying && it.currentPosition > paused + 500 }
            val notifications = context.getSystemService(NotificationManager::class.java).activeNotifications
            assertTrue("Media playback notification was not posted", notifications.isNotEmpty())
        } finally {
            withContext(Dispatchers.Main) { controller?.stop(); controller?.clearMediaItems(); controller?.release() }
            scenario.close()
            fixture.delete()
        }
    }
}
