package com.mrigank.player

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import java.util.Locale
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerActivity : ComponentActivity() {

    companion object {
        const val ACTION_PIP = "com.mrigank.player.PIP_ACTION"
    }

    private lateinit var prefs: Prefs
    private var player by mutableStateOf<ExoPlayer?>(null)
    private var pipMode by mutableStateOf(false)
    private var boostPercent by mutableIntStateOf(100)
    private var eqPreset by mutableIntStateOf(-1)
    private var enhancer: LoudnessEnhancer? = null
    private var equalizer: Equalizer? = null
    private var videoRect: Rect? = null
    private val area = intArrayOf(0, 0, 0, 0)
    private var subMap: Map<Long, Library.SubFile> = emptyMap()
    private var startToken = 0
    private var lastIndex = 0

    private val subPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) applyExternalSub(uri)
    }

    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val p = player ?: return
            val step = prefs.skipShort * 1000L
            when (intent?.getIntExtra("cmd", 0)) {
                1 -> p.seekTo((p.currentPosition - step).coerceAtLeast(0L))
                2 -> if (p.isPlaying) p.pause() else p.play()
                3 -> p.seekTo(p.currentPosition + step)
            }
            updatePip()
        }
    }

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePip()
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            setVideoRect(area[0], area[1], area[2], area[3])
            updatePip()
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            bindFx(audioSessionId)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val previous = lastIndex
            lastIndex = player?.currentMediaItemIndex ?: previous
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                val v = PlayQueue.videos.getOrNull(previous)
                if (v != null) prefs.setPosition(v.id, -1L)
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                val p = player
                if (p != null) {
                    val v = PlayQueue.videos.getOrNull(p.currentMediaItemIndex)
                    if (v != null) prefs.setPosition(v.id, -1L)
                }
            }
        }
    }

    // ---------------------------------------------------------------- lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(applicationContext)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= 28) {
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = lp
        }
        hideBars()
        ContextCompat.registerReceiver(
            this,
            pipReceiver,
            IntentFilter(ACTION_PIP),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        setContent {
            AppTheme(prefs) {
                PlayerRoot()
            }
        }
        startPlayback()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        startPlayback()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideBars()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT < 31 && prefs.autoPip && player?.isPlaying == true) {
            enterPip()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pipMode = isInPictureInPictureMode
    }

    override fun onStop() {
        super.onStop()
        saveProgress()
        player?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(pipReceiver)
        } catch (e: Exception) {
            // ignore
        }
        startToken++
        releasePlayer()
    }

    private fun hideBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    // ---------------------------------------------------------------- playback

    private fun startPlayback() {
        val data = intent?.data
        if (intent?.action == Intent.ACTION_VIEW && data != null) {
            val last = data.lastPathSegment ?: "Video"
            val name = last.substringAfterLast('/').substringBeforeLast('.')
            PlayQueue.videos = listOf(
                Video(data.toString().hashCode().toLong(), data, name, 0L, 0L, 0L, Library.OTHER, 0, 0)
            )
            PlayQueue.index = 0
        }
        val videos = PlayQueue.videos
        if (videos.isEmpty()) {
            finish()
            return
        }
        val startIndex = PlayQueue.index.coerceIn(0, videos.size - 1)
        releasePlayer()
        val token = ++startToken
        lifecycleScope.launch {
            val matches: Map<Long, Library.SubFile> = if (prefs.rememberSub) {
                withContext(Dispatchers.IO) {
                    Library.match(videos, Library.listSubs(applicationContext, prefs.subFolder))
                }
            } else {
                emptyMap()
            }
            if (token == startToken) {
                subMap = matches
                createPlayer(videos, startIndex)
            }
        }
    }

    private fun createPlayer(videos: List<Video>, startIndex: Int) {
        val attrs = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
        val p = ExoPlayer.Builder(this)
            .setAudioAttributes(attrs, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        val items = videos.map { itemFor(it) }
        val resume = prefs.position(videos[startIndex].id)
        p.setMediaItems(items, startIndex, if (resume > 3000L) resume else 0L)

        val params = p.trackSelectionParameters.buildUpon()
        if (prefs.rememberSub) {
            if (prefs.subOff) {
                params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            } else {
                val lang = prefs.subLang
                if (lang != null) params.setPreferredTextLanguage(lang)
            }
        }
        val audioLang = prefs.audioLang
        if (audioLang != null) params.setPreferredAudioLanguage(audioLang)
        p.trackSelectionParameters = params.build()

        p.addListener(listener)
        lastIndex = startIndex
        p.prepare()
        p.playWhenReady = true
        player = p
    }

    private fun releasePlayer() {
        saveProgress()
        val p = player
        player = null
        if (p != null) {
            p.removeListener(listener)
            p.release()
        }
        try {
            enhancer?.release()
        } catch (e: Exception) {
            // ignore
        }
        try {
            equalizer?.release()
        } catch (e: Exception) {
            // ignore
        }
        enhancer = null
        equalizer = null
    }

    private fun itemFor(v: Video): MediaItem {
        val b = MediaItem.Builder()
            .setUri(v.uri)
            .setMediaId(v.id.toString())
            .setMediaMetadata(MediaMetadata.Builder().setTitle(v.name).build())
        val sub = subMap[v.id]
        if (sub != null) b.setSubtitleConfigurations(listOf(subConfig(sub.uri, sub.name)))
        return b.build()
    }

    private fun subConfig(uri: Uri, name: String): MediaItem.SubtitleConfiguration {
        val ext = name.substringAfterLast('.', "").lowercase()
        val mime = when (ext) {
            "vtt" -> MimeTypes.TEXT_VTT
            "ass", "ssa" -> MimeTypes.TEXT_SSA
            else -> MimeTypes.APPLICATION_SUBRIP
        }
        return MediaItem.SubtitleConfiguration.Builder(uri)
            .setMimeType(mime)
            .setLabel(name)
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()
    }

    private fun queryName(uri: Uri): String {
        try {
            val cursor = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val n = c.getString(0)
                    if (n != null) return n
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        return uri.lastPathSegment ?: "subtitle.srt"
    }

    private fun applyExternalSub(uri: Uri) {
        val p = player ?: return
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
            // not every provider allows this
        }
        val name = queryName(uri)
        val idx = p.currentMediaItemIndex
        val position = p.currentPosition
        val newItem = p.getMediaItemAt(idx).buildUpon()
            .setSubtitleConfigurations(listOf(subConfig(uri, name)))
            .build()
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .build()
        prefs.subOff = false
        p.replaceMediaItem(idx, newItem)
        p.seekTo(idx, position)
    }

    private fun saveProgress() {
        val p = player ?: return
        val v = PlayQueue.videos.getOrNull(p.currentMediaItemIndex) ?: return
        val d = p.duration
        val pos = p.currentPosition
        if (d != C.TIME_UNSET && d > 0L && pos >= d - 10_000L) {
            prefs.setPosition(v.id, -1L)
        } else if (pos > 3000L) {
            prefs.setPosition(v.id, pos)
        }
    }

    // ---------------------------------------------------------------- audio effects

    private fun bindFx(session: Int) {
        if (session == C.AUDIO_SESSION_ID_UNSET || session == 0) return
        try {
            enhancer?.release()
            enhancer = LoudnessEnhancer(session)
            applyBoost()
        } catch (e: Exception) {
            enhancer = null
        }
        try {
            equalizer?.release()
            val eq = Equalizer(0, session)
            equalizer = eq
            if (eqPreset >= 0 && eqPreset < eq.numberOfPresets.toInt()) {
                eq.usePreset(eqPreset.toShort())
                eq.setEnabled(true)
            } else {
                eq.setEnabled(false)
            }
        } catch (e: Exception) {
            equalizer = null
        }
    }

    private fun applyBoost() {
        val fx = enhancer ?: return
        try {
            if (boostPercent <= 100) {
                fx.setEnabled(false)
            } else {
                fx.setTargetGain((2000.0 * log10(boostPercent / 100.0)).toInt())
                fx.setEnabled(true)
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun eqNames(): List<String> {
        val eq = equalizer ?: return emptyList()
        return try {
            val n = eq.numberOfPresets.toInt()
            (0 until n).map { eq.getPresetName(it.toShort()) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun setEq(index: Int) {
        eqPreset = index
        val eq = equalizer ?: return
        try {
            if (index < 0) {
                eq.setEnabled(false)
            } else {
                eq.usePreset(index.toShort())
                eq.setEnabled(true)
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    // ---------------------------------------------------------------- picture in picture

    private fun remoteAction(icon: Int, title: String, cmd: Int): RemoteAction {
        val intent = Intent(ACTION_PIP).setPackage(packageName).putExtra("cmd", cmd)
        val pending = PendingIntent.getBroadcast(
            this,
            cmd,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return RemoteAction(Icon.createWithResource(this, icon), title, title, pending)
    }

    private fun pipParams(): PictureInPictureParams {
        val p = player
        val size = p?.videoSize
        var w = size?.width ?: 0
        var h = size?.height ?: 0
        if (w <= 0 || h <= 0) {
            w = 16
            h = 9
        }
        val ratio = (w.toFloat() / h.toFloat()).coerceIn(0.45f, 2.3f)
        val playing = p?.isPlaying == true
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational((ratio * 1000f).toInt(), 1000))
            .setActions(
                listOf(
                    remoteAction(R.drawable.ic_rew, "Back", 1),
                    remoteAction(if (playing) R.drawable.ic_pause else R.drawable.ic_play, "Play or pause", 2),
                    remoteAction(R.drawable.ic_fwd, "Forward", 3)
                )
            )
        if (Build.VERSION.SDK_INT >= 31) {
            builder.setAutoEnterEnabled(playing && prefs.autoPip)
            builder.setSeamlessResizeEnabled(true)
        }
        val rect = videoRect
        if (rect != null) builder.setSourceRectHint(rect)
        return builder.build()
    }

    private fun updatePip() {
        try {
            setPictureInPictureParams(pipParams())
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun enterPip() {
        try {
            enterPictureInPictureMode(pipParams())
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun setVideoRect(l: Int, t: Int, r: Int, b: Int) {
        val cw = r - l
        val ch = b - t
        if (cw <= 0 || ch <= 0) return
        val vs = player?.videoSize
        if (vs == null || vs.width <= 0 || vs.height <= 0) {
            videoRect = Rect(l, t, r, b)
            return
        }
        val vr = vs.width.toFloat() / vs.height.toFloat()
        val cr = cw.toFloat() / ch.toFloat()
        videoRect = if (vr > cr) {
            val vh = (cw / vr).toInt()
            val top = t + (ch - vh) / 2
            Rect(l, top, r, top + vh)
        } else {
            val vw = (ch * vr).toInt()
            val left = l + (cw - vw) / 2
            Rect(left, t, left + vw, b)
        }
    }

    // ---------------------------------------------------------------- small helpers

    private fun trackLabel(f: Format, n: Int): String {
        val label = f.label
        if (label != null && label.isNotBlank()) return label
        val lang = f.language
        if (lang != null && lang != "und") {
            val d = Locale.forLanguageTag(lang).displayLanguage
            return if (d.isNotBlank()) d else lang.uppercase()
        }
        return "Track $n"
    }

    private fun speedLabel(s: Float): String {
        return if (s == s.toInt().toFloat()) "${s.toInt()}x" else "${s}x"
    }

    // ---------------------------------------------------------------- UI

    @Composable
    private fun PlayerRoot() {
        val p = player
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (p != null) PlayerUi(p)
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun Flow(content: @Composable () -> Unit) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }

    @Composable
    private fun PanelTitle(text: String) {
        T(text.uppercase(), 12, LocalPal.current.muted, FontWeight.SemiBold)
    }

    @Composable
    private fun TrackRow(label: String, selected: Boolean, onClick: () -> Unit) {
        val pal = LocalPal.current
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            T(
                label,
                16,
                if (selected) pal.accent else pal.onBg,
                if (selected) FontWeight.SemiBold else FontWeight.Normal,
                2,
                Modifier.weight(1f)
            )
            if (selected) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(pal.accent))
            }
        }
    }

    @Composable
    private fun SheetPanel(onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
        val pal = LocalPal.current
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000))
                    .pointerInput(Unit) { detectTapGestures { onClose() } }
            )
            Column(
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .widthIn(max = 380.dp)
                    .fillMaxWidth(0.55f)
                    .background(pal.surface)
                    .windowInsetsPadding(WindowInsets.displayCutout)
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
                    .pointerInput(Unit) { detectTapGestures { } },
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content
            )
        }
    }

    @Composable
    private fun SkipBtn(label: String, onClick: () -> Unit) {
        Box(
            Modifier
                .height(40.dp)
                .widthIn(min = 56.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x26FFFFFF))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            T(label, 14, Color.White, FontWeight.SemiBold)
        }
    }

    @Composable
    private fun PlayerUi(p: ExoPlayer) {
        val pal = LocalPal.current
        val scope = rememberCoroutineScope()

        var playing by remember(p) { mutableStateOf(p.isPlaying) }
        var pos by remember(p) { mutableLongStateOf(0L) }
        var dur by remember(p) { mutableLongStateOf(0L) }
        var index by remember(p) { mutableIntStateOf(p.currentMediaItemIndex) }
        var tracks by remember(p) { mutableStateOf(p.currentTracks) }
        var controls by remember(p) { mutableStateOf(true) }
        var locked by remember(p) { mutableStateOf(false) }
        var sheet by remember(p) { mutableStateOf("") }
        var hold by remember(p) { mutableStateOf(false) }
        var speed by remember(p) { mutableFloatStateOf(1f) }
        var aPos by remember(p) { mutableLongStateOf(-1L) }
        var bPos by remember(p) { mutableLongStateOf(-1L) }
        var sleepMin by remember(p) { mutableIntStateOf(0) }
        var sleepEnd by remember(p) { mutableStateOf(false) }
        var flash by remember(p) { mutableStateOf("") }
        var touch by remember(p) { mutableIntStateOf(0) }
        var dragging by remember(p) { mutableStateOf(false) }
        var dragVal by remember(p) { mutableFloatStateOf(0f) }
        val consumed = remember(p) { BooleanArray(1) }
        val queue = PlayQueue.videos

        fun skip(ms: Long) {
            val d = p.duration
            val limit = if (d == C.TIME_UNSET || d < 0L) Long.MAX_VALUE else d
            p.seekTo((p.currentPosition + ms).coerceIn(0L, limit))
            flash = (if (ms > 0L) "+" else "−") + abs(ms / 1000L) + "s"
            touch++
        }

        DisposableEffect(p) {
            val l = object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    playing = isPlaying
                }

                override fun onTracksChanged(t: Tracks) {
                    tracks = t
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    index = p.currentMediaItemIndex
                    aPos = -1L
                    bPos = -1L
                    if (sleepEnd && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                        p.pause()
                        sleepEnd = false
                    }
                }
            }
            p.addListener(l)
            onDispose { p.removeListener(l) }
        }

        LaunchedEffect(p) {
            var n = 0
            while (true) {
                pos = p.currentPosition
                val d = p.duration
                dur = if (d == C.TIME_UNSET || d < 0L) 0L else d
                if (aPos >= 0L && bPos > aPos && pos >= bPos) p.seekTo(aPos)
                n++
                if (n % 20 == 0) saveProgress()
                delay(250)
            }
        }

        LaunchedEffect(flash) {
            if (flash.isNotEmpty()) {
                delay(900)
                flash = ""
            }
        }

        LaunchedEffect(controls, playing, sheet, touch, locked) {
            if (controls && (playing || locked) && sheet.isEmpty()) {
                delay(if (locked) 2500L else 3500L)
                controls = false
            }
        }

        LaunchedEffect(sleepMin) {
            if (sleepMin > 0) {
                delay(sleepMin * 60_000L)
                p.pause()
                sleepMin = 0
            }
        }

        BackHandler(enabled = sheet.isNotEmpty()) { sheet = "" }
        BackHandler(enabled = locked) { }

        Box(Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    val v = PlayerView(ctx)
                    v.useController = false
                    v.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    v.player = p
                    v
                },
                update = { v ->
                    v.player = p
                    v.subtitleView?.setBottomPaddingFraction(if (controls && !locked) 0.22f else 0.06f)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { c ->
                        val r = c.boundsInWindow()
                        area[0] = r.left.toInt()
                        area[1] = r.top.toInt()
                        area[2] = r.right.toInt()
                        area[3] = r.bottom.toInt()
                        setVideoRect(area[0], area[1], area[2], area[3])
                        updatePip()
                    }
            )

            if (!pipMode) {
                // Touch layer: tap, double tap to skip, press and hold for 2x
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(locked) {
                            detectTapGestures(
                                onPress = {
                                    consumed[0] = false
                                    if (locked) {
                                        tryAwaitRelease()
                                    } else {
                                        val job = scope.launch {
                                            delay(350)
                                            consumed[0] = true
                                            hold = true
                                            p.setPlaybackSpeed(2f)
                                        }
                                        tryAwaitRelease()
                                        job.cancel()
                                        if (hold) {
                                            hold = false
                                            p.setPlaybackSpeed(speed)
                                        }
                                    }
                                },
                                onDoubleTap = { offset ->
                                    if (!locked) {
                                        val step = prefs.skipShort * 1000L
                                        if (offset.x < size.width / 2f) skip(-step) else skip(step)
                                    }
                                },
                                onTap = {
                                    if (!consumed[0]) {
                                        controls = !controls
                                        touch++
                                    }
                                }
                            )
                        }
                )

                if (hold) {
                    Row(
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 22.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .background(Color(0x99141620))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        T("2x", 16, Color.White, FontWeight.Bold)
                        Glyph(Ic.Play, Color.White, 16.dp)
                        Glyph(Ic.Play, Color.White, 16.dp)
                    }
                }

                if (flash.isNotEmpty()) {
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x99141620))
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        T(flash, 18, Color.White, FontWeight.Bold)
                    }
                }

                if (locked) {
                    if (controls) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(20.dp)
                                .clip(CircleShape)
                                .background(Color(0x99000000))
                                .pointerInput(Unit) {
                                    detectTapGestures(onLongPress = {
                                        locked = false
                                        controls = true
                                        touch++
                                    })
                                }
                                .padding(14.dp)
                        ) {
                            Glyph(Ic.Lock, Color.White, 24.dp)
                        }
                        T(
                            "Hold the lock to unlock",
                            13,
                            Color(0xCCFFFFFF),
                            FontWeight.Normal,
                            1,
                            Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
                        )
                    }
                } else {
                    if (controls) {
                        // Top bar
                        Row(
                            Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .background(Brush.verticalGradient(listOf(Color(0xB3000000), Color.Transparent)))
                                .windowInsetsPadding(WindowInsets.displayCutout)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconBtn(Ic.Back, Color.White, { finish() })
                            T(
                                queue.getOrNull(index)?.name ?: "",
                                16,
                                Color.White,
                                FontWeight.SemiBold,
                                1,
                                Modifier.weight(1f).padding(horizontal = 4.dp)
                            )
                            IconBtn(Ic.List, Color.White, { sheet = "queue" })
                            IconBtn(Ic.Pip, Color.White, { enterPip() })
                            IconBtn(Ic.Lock, Color.White, {
                                locked = true
                                controls = true
                                touch++
                            })
                            Box(
                                Modifier
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .clickable { sheet = "tracks" }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                T("CC", 15, Color.White, FontWeight.Bold)
                            }
                            IconBtn(Ic.More, Color.White, { sheet = "more" })
                        }

                        // Bottom pill
                        Column(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                                .padding(horizontal = 16.dp)
                                .widthIn(max = 560.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(28.dp))
                                .background(Color(0xB3121316))
                                .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(28.dp))
                                .pointerInput(Unit) { detectTapGestures { } }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                T(fmt(if (dragging) dragVal.toLong() else pos), 12, Color.White)
                                Slider(
                                    value = (if (dragging) dragVal else pos.toFloat())
                                        .coerceIn(0f, max(dur, 1L).toFloat()),
                                    onValueChange = {
                                        dragging = true
                                        dragVal = it
                                        touch++
                                    },
                                    onValueChangeFinished = {
                                        p.seekTo(dragVal.toLong())
                                        dragging = false
                                    },
                                    valueRange = 0f..max(dur, 1L).toFloat(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color.White,
                                        inactiveTrackColor = Color(0x40FFFFFF)
                                    ),
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                                )
                                T(fmt(dur), 12, Color.White)
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SkipBtn("−${prefs.skipLong}") { skip(-prefs.skipLong * 1000L) }
                                SkipBtn("−${prefs.skipShort}") { skip(-prefs.skipShort * 1000L) }
                                Box(
                                    Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .clickable {
                                            if (p.isPlaying) {
                                                p.pause()
                                            } else {
                                                if (p.playbackState == Player.STATE_ENDED) p.seekTo(0L)
                                                p.play()
                                            }
                                            touch++
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Glyph(if (playing) Ic.Pause else Ic.Play, Color.Black, 28.dp)
                                }
                                SkipBtn("+${prefs.skipShort}") { skip(prefs.skipShort * 1000L) }
                                SkipBtn("+${prefs.skipLong}") { skip(prefs.skipLong * 1000L) }
                            }
                        }
                    }

                    // Up next card
                    val next = queue.getOrNull(index + 1)
                    if (next != null && dur > 0L && (dur - pos) in 1L..20_000L && sheet.isEmpty()) {
                        Column(
                            Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 20.dp, bottom = if (controls) 150.dp else 24.dp)
                                .widthIn(max = 260.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xCC121316))
                                .clickable { p.seekToNextMediaItem() }
                                .padding(12.dp)
                        ) {
                            T("Up next", 11, Color(0xFFB0B4BA), FontWeight.SemiBold)
                            T(next.name, 14, Color.White, FontWeight.SemiBold, 2)
                        }
                    }
                }

                // Side sheets
                if (sheet == "more") {
                    SheetPanel({ sheet = "" }) {
                        PanelTitle("Speed")
                        Flow {
                            for (s in listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)) {
                                Chip(speedLabel(s), abs(speed - s) < 0.01f) {
                                    speed = s
                                    p.setPlaybackSpeed(s)
                                }
                            }
                        }

                        PanelTitle("Sleep timer")
                        Flow {
                            for (m in listOf(0, 10, 15, 30, 45, 60)) {
                                Chip(if (m == 0) "Off" else "$m min", sleepMin == m && !sleepEnd) {
                                    sleepMin = m
                                    sleepEnd = false
                                }
                            }
                            Chip("End of video", sleepEnd) {
                                sleepEnd = true
                                sleepMin = 0
                            }
                        }

                        PanelTitle("A-B repeat")
                        Flow {
                            Chip(if (aPos >= 0L) "A  " + fmt(aPos) else "Set A", aPos >= 0L) {
                                aPos = p.currentPosition
                                if (bPos <= aPos) bPos = -1L
                            }
                            Chip(if (bPos >= 0L) "B  " + fmt(bPos) else "Set B", bPos >= 0L) {
                                if (aPos >= 0L && p.currentPosition > aPos) bPos = p.currentPosition
                            }
                            Chip("Clear", false) {
                                aPos = -1L
                                bPos = -1L
                            }
                        }

                        Row(Modifier.fillMaxWidth()) {
                            T("VOLUME BOOST", 12, pal.muted, FontWeight.SemiBold, 1, Modifier.weight(1f))
                            T("$boostPercent%", 14, pal.onBg, FontWeight.SemiBold)
                        }
                        Slider(
                            value = boostPercent.toFloat(),
                            onValueChange = {
                                boostPercent = it.toInt()
                                applyBoost()
                            },
                            valueRange = 100f..200f,
                            colors = SliderDefaults.colors(
                                thumbColor = pal.accent,
                                activeTrackColor = pal.accent,
                                inactiveTrackColor = pal.onBg.copy(alpha = 0.2f)
                            )
                        )

                        PanelTitle("Equalizer")
                        val names = eqNames()
                        if (names.isEmpty()) {
                            T("Not available on this device", 14, pal.muted)
                        } else {
                            Flow {
                                Chip("Off", eqPreset < 0) { setEq(-1) }
                                for (i in names.indices) {
                                    Chip(names[i], eqPreset == i) { setEq(i) }
                                }
                            }
                        }
                    }
                }

                if (sheet == "tracks") {
                    SheetPanel({ sheet = "" }) {
                        val textGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
                        val textOff = p.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
                        PanelTitle("Subtitles")
                        TrackRow("Off", textOff || textGroups.none { it.isSelected }) {
                            p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                .build()
                            prefs.subOff = true
                            sheet = ""
                        }
                        var n = 0
                        for (g in textGroups) {
                            for (i in 0 until g.length) {
                                n++
                                val label = trackLabel(g.getTrackFormat(i), n)
                                TrackRow(label, g.isTrackSelected(i) && !textOff) {
                                    p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                        .setOverrideForType(TrackSelectionOverride(g.mediaTrackGroup, i))
                                        .build()
                                    prefs.subOff = false
                                    prefs.subLang = g.getTrackFormat(i).language
                                    sheet = ""
                                }
                            }
                        }
                        TrackRow("Load a subtitle file", false) {
                            sheet = ""
                            subPicker.launch(arrayOf("*/*"))
                        }
                        if (prefs.subFolder == null) {
                            T(
                                "Tip: choose a subtitle folder in Settings and subtitles will be applied to every video automatically.",
                                13,
                                pal.muted,
                                FontWeight.Normal,
                                4
                            )
                        }

                        val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
                        var audioCount = 0
                        for (g in audioGroups) audioCount += g.length
                        if (audioCount > 1) {
                            PanelTitle("Audio")
                            var m = 0
                            for (g in audioGroups) {
                                for (i in 0 until g.length) {
                                    m++
                                    val label = trackLabel(g.getTrackFormat(i), m)
                                    TrackRow(label, g.isTrackSelected(i)) {
                                        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                                            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                                            .setOverrideForType(TrackSelectionOverride(g.mediaTrackGroup, i))
                                            .build()
                                        prefs.audioLang = g.getTrackFormat(i).language
                                        sheet = ""
                                    }
                                }
                            }
                        }
                    }
                }

                if (sheet == "queue") {
                    SheetPanel({ sheet = "" }) {
                        PanelTitle("Playing queue")
                        for (i in queue.indices) {
                            TrackRow(queue[i].name, i == index) {
                                p.seekToDefaultPosition(i)
                                sheet = ""
                            }
                        }
                    }
                }
            }
        }
    }
}
