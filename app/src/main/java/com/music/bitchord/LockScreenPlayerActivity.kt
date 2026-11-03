package com.music.bitchord

import android.animation.ValueAnimator
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil3.compose.AsyncImage
import com.google.common.util.concurrent.ListenableFuture
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.playback.PlaybackService
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** User-opened music-only surface. Never dismisses the keyguard or launches from the background. */
class LockScreenPlayerActivity : ComponentActivity() {
    private var connection: ListenableFuture<MediaController>? = null
    private var controller by mutableStateOf<MediaController?>(null)
    private var connectionError by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) setShowWhenLocked(true)
        else @Suppress("DEPRECATION") window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        // Deliberately no dismiss-keyguard, turn-screen-on, overlay or keep-screen-on flags.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Color.White, onPrimary = Color.Black)) {
                Surface(color = Color(0xFF111113)) { LockScreenPlayer(controller, connectionError, onClose = ::finish) }
            }
        }
    }
    override fun onStart() {
        super.onStart()
        connectionError = null
        val future = MediaController.Builder(this, SessionToken(this, ComponentName(this, PlaybackService::class.java))).buildAsync()
        connection = future
        future.addListener({
            if (connection === future) runCatching { future.get() }
                .onSuccess { controller = it }
                .onFailure { connectionError = "Could not connect to playback. Return to Klyra and try again." }
        }, ContextCompat.getMainExecutor(this))
    }
    override fun onStop() {
        controller = null
        connection?.let(MediaController::releaseFuture)
        connection = null
        super.onStop()
    }
}

@Composable
private fun LockScreenPlayer(player: MediaController?, error: String?, onClose: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val reduceMotion by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    val reduceBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    var revision by remember { mutableIntStateOf(0) }
    var position by remember { mutableLongStateOf(0L) }
    var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var visible by remember { mutableStateOf(false) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) { revision++ }
        }
        player?.addListener(listener)
        onDispose { player?.removeListener(listener) }
    }
    LaunchedEffect(player, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            visible = true
            try {
                while (isActive) {
                    position = player?.currentPosition?.coerceAtLeast(0L) ?: 0L
                    clock = System.currentTimeMillis()
                    delay(500)
                }
            } finally { visible = false }
        }
    }
    val metadata = remember(player, revision) { player?.mediaMetadata }
    val playing = remember(player, revision) { player?.isPlaying == true }
    val duration = remember(player, revision) { player?.duration?.coerceAtLeast(0L) ?: 0L }
    val artwork: Any? = metadata?.artworkData ?: metadata?.artworkUri
    val hasTrack = player?.currentMediaItem != null
    val motion = visible && playing && !reduceMotion && ValueAnimator.areAnimatorsEnabled()
    val scale = if (motion) {
        val transition = rememberInfiniteTransition(label = "artwork breathing")
        transition.animateFloat(1f, 1.025f, infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse), label = "artwork scale").value
    } else 1f
    Box(Modifier.fillMaxSize().background(Color(0xFF111113))) {
        if (artwork != null && !reduceBlur) AsyncImage(artwork, null, Modifier.fillMaxSize().blur(50.dp), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .58f)))
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
            val wide = maxWidth > maxHeight
            val artSize = if (wide) minOf(maxHeight - 24.dp, maxWidth * .45f) else minOf(maxWidth, maxHeight * .40f, 440.dp)
            val cover: @Composable () -> Unit = {
                Box(Modifier.size(artSize.coerceAtLeast(80.dp)).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = .07f)), contentAlignment = Alignment.Center) {
                    if (artwork != null) AsyncImage(artwork, metadata?.albumTitle?.toString() ?: "Album artwork",
                        Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale }, contentScale = ContentScale.Crop)
                    else Icon(Icons.Rounded.MusicNote, null, Modifier.size(72.dp), tint = Color.White.copy(alpha = .5f))
                }
            }
            val controls: @Composable () -> Unit = {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color.White.copy(alpha = .09f)).padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(metadata?.title?.toString() ?: if (player == null && error == null) "Connecting…" else "Nothing playing", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(metadata?.artist?.toString() ?: error ?: "Start a song in Klyra to use this player.", color = Color.White.copy(alpha = .66f), maxLines = 3, fontSize = 14.sp)
                    var scrub by remember(metadata?.title, player?.currentMediaItem?.mediaId) { mutableStateOf<Float?>(null) }
                    val seekable = duration > 0 && player?.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) == true
                    Slider(value = scrub ?: if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f,
                        onValueChange = { scrub = it }, enabled = seekable,
                        onValueChangeFinished = { scrub?.let { player?.seekTo((it * duration).toLong()) }; scrub = null })
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(lockTime(position), fontSize = 12.sp, color = Color.White.copy(alpha = .65f))
                        Text(lockTime(duration), fontSize = 12.sp, color = Color.White.copy(alpha = .65f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { player?.seekToPrevious() }, enabled = player?.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS) == true) { Icon(Icons.Rounded.SkipPrevious, "Previous track", Modifier.size(32.dp)) }
                        FilledIconButton(onClick = { if (playing) player?.pause() else player?.play() },
                            enabled = hasTrack && player?.isCommandAvailable(Player.COMMAND_PLAY_PAUSE) == true,
                            modifier = Modifier.size(64.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Color.Black)) {
                            Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (playing) "Pause" else "Play", Modifier.size(36.dp))
                        }
                        IconButton(onClick = { player?.seekToNextMediaItem() }, enabled = player?.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM) == true) { Icon(Icons.Rounded.SkipNext, "Next track", Modifier.size(32.dp)) }
                    }
                }
            }
            val clockHeader: @Composable () -> Unit = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date(clock)), color = Color.White.copy(alpha = .75f), fontSize = 15.sp)
                    Text(android.text.format.DateFormat.getTimeFormat(context).format(Date(clock)), fontSize = if (wide) 38.sp else 56.sp, fontWeight = FontWeight.Medium)
                }
            }
            if (wide) Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
                cover()
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    clockHeader(); controls(); TextButton(onClose) { Text("Close cover screen") }
                }
            } else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                clockHeader(); cover(); controls()
                TextButton(onClose) { Text("Close cover screen") }
            }
        }
    }
}

private fun lockTime(ms: Long): String = "%d:%02d".format(ms / 60000, (ms / 1000) % 60)
