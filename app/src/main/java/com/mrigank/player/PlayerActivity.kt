package com.mrigank.player

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerActivity : ComponentActivity() {

    companion object {
        const val ACTION_PIP = "com.mrigank.player.PIP_ACTION"
    }

    private lateinit var prefs: Prefs
    private var player by mutableStateOf<ExoPlayer?>(null)
    private var pipMode by mutableStateOf(false)
    private val fx = AudioFx()
    private var videoRect: Rect? = null
    private val area = intArrayOf(0, 0, 0, 0)
    private var subMap: Map<Long, Library.SubFile> = emptyMap()
    private var startToken = 0
    private var lastIndex = 0

    private val actions = PlayerActions(
        back = { finish() },
        pip = { enterPip() },
        pickSub = { subPicker.launch(arrayOf("*/*")) },
        cast = { castScreen() },
        rotate = { lock ->
            requestedOrientation = if (lock) {
                ActivityInfo.SCREEN_ORIENTATION_LOCKED
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        },
        save = { saveProgress() },
        area = { l, t, r, b ->
            area[0] = l
            area[1] = t
            area[2] = r
            area[3] = b
            setVideoRect(l, t, r, b)
            updatePip()
        }
    )

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
            fx.bind(audioSessionId)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val previous = lastIndex
            lastIndex = player?.currentMediaItemIndex ?: previous
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                val v = PlayQueue.videos.getOrNull(previous)
                if (v != null) prefs.setPosition(v.id, -1L)
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (reason != Player.DISCONTINUITY_REASON_AUTO_TRANSITION &&
                oldPosition.mediaItemIndex != newPosition.mediaItemIndex
            ) {
                val v = PlayQueue.videos.getOrNull(oldPosition.mediaItemIndex)
                if (v != null && oldPosition.positionMs > 3000L) {
                    prefs.setPosition(v.id, oldPosition.positionMs)
                }
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

    override fun onPause() {
        super.onPause()
        saveProgress()
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
        fx.release()
    }

    private fun itemFor(v: Video): MediaItem {
        val b = MediaItem.Builder()
            .setUri(v.uri)
            .setMediaId(v.id.toString())
            .setMediaMetadata(MediaMetadata.Builder().setTitle(v.name).build())
        val sub = subMap[v.id]
        if (sub != null) b.setSubtitleConfigurations(listOf(plSubConfig(sub.uri, sub.name)))
        return b.build()
    }

    private fun applyExternalSub(uri: Uri) {
        val p = player ?: return
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
            // not every provider allows this
        }
        val name = plQueryName(contentResolver, uri)
        val idx = p.currentMediaItemIndex
        val position = p.currentPosition
        val newItem = p.getMediaItemAt(idx).buildUpon()
            .setSubtitleConfigurations(listOf(plSubConfig(uri, name)))
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
        LastPlayed.set(applicationContext, v.id)
        val d = p.duration
        val pos = p.currentPosition
        if (d != C.TIME_UNSET && d > 0L && pos >= d - 10_000L) {
            prefs.setPosition(v.id, -1L)
        } else if (pos > 3000L) {
            prefs.setPosition(v.id, pos)
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

    private fun castScreen() {
        try {
            startActivity(Intent(Settings.ACTION_CAST_SETTINGS))
        } catch (e: Exception) {
            // ignore
        }
    }

    @Composable
    private fun PlayerRoot() {
        val p = player
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (p != null) PlayerUi(p, prefs, fx, pipMode, actions)
        }
    }
}
