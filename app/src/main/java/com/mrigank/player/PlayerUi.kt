package com.mrigank.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class PlayerActions(
    val back: () -> Unit,
    val pip: () -> Unit,
    val pickSub: () -> Unit,
    val cast: () -> Unit,
    val rotate: (Boolean) -> Unit,
    val save: () -> Unit,
    val area: (Int, Int, Int, Int) -> Unit
)

private val Shade = Color(0x99141620)

@Composable
internal fun PlayerUi(p: ExoPlayer, prefs: Prefs, fx: AudioFx, pipMode: Boolean, act: PlayerActions) {
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
    var flashSide by remember(p) { mutableIntStateOf(0) }
    var touch by remember(p) { mutableIntStateOf(0) }
    var dragging by remember(p) { mutableStateOf(false) }
    var dragVal by remember(p) { mutableFloatStateOf(0f) }
    var muted by remember(p) { mutableStateOf(p.volume == 0f) }
    var audioOnly by remember(p) { mutableStateOf(false) }
    var rotLocked by remember { mutableStateOf(false) }
    var resize by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    val consumed = remember(p) { BooleanArray(1) }
    val queue = PlayQueue.videos

    fun skip(ms: Long) {
        val d = p.duration
        val limit = if (d == C.TIME_UNSET || d < 0L) Long.MAX_VALUE else d
        p.seekTo((p.currentPosition + ms).coerceIn(0L, limit))
        flash = (if (ms > 0L) "+" else "−") + abs(ms / 1000L) + "s"
        flashSide = if (ms > 0L) 1 else -1
        touch++
    }

    fun toggle() {
        if (p.isPlaying) {
            p.pause()
        } else {
            if (p.playbackState == Player.STATE_ENDED) p.seekTo(0L)
            p.play()
        }
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
            if (n % 20 == 0) act.save()
            delay(250)
        }
    }

    LaunchedEffect(flash) {
        if (flash.isNotEmpty()) {
            delay(800)
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
                v.subtitleView?.setApplyEmbeddedStyles(false)
                v.subtitleView?.setStyle(
                    CaptionStyleCompat(
                        android.graphics.Color.WHITE,
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                        CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                        android.graphics.Color.BLACK,
                        null
                    )
                )
                v
            },
            update = { v ->
                v.player = p
                v.resizeMode = resize
                v.subtitleView?.setBottomPaddingFraction(if (controls && !locked) 0.34f else 0.06f)
            },
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { c ->
                    val r = c.boundsInWindow()
                    act.area(r.left.toInt(), r.top.toInt(), r.right.toInt(), r.bottom.toInt())
                }
        )

        if (!pipMode) {
            // Touch layer: tap = show/hide, double tap = left back / middle play-pause / right forward
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
                                    val third = size.width / 3f
                                    if (offset.x < third) {
                                        skip(-step)
                                    } else if (offset.x > third * 2f) {
                                        skip(step)
                                    } else {
                                        toggle()
                                        flash = if (p.playWhenReady) "Play" else "Pause"
                                        flashSide = 0
                                    }
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
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 22.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(Shade)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    T("2x  ▶▶", 16, Color.White, FontWeight.Bold)
                }
            }

            if (flash.isNotEmpty()) {
                Box(
                    Modifier
                        .align(
                            when (flashSide) {
                                -1 -> Alignment.CenterStart
                                1 -> Alignment.CenterEnd
                                else -> Alignment.Center
                            }
                        )
                        .padding(horizontal = 72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Shade)
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
                        PlGlyph(PlIc.Lock, Color.White, 24.dp)
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
                AnimatedVisibility(
                    visible = controls,
                    enter = fadeIn(tween(180)),
                    exit = fadeOut(tween(220))
                ) {
                    PlayerControls(
                        p, prefs,
                        CtlInfo(
                            queue.getOrNull(index)?.name ?: "", playing, pos, dur,
                            dragging, dragVal, muted, audioOnly, rotLocked
                        ),
                        CtlActions(
                            sheet = { sheet = it },
                            drag = {
                                dragging = true
                                dragVal = it
                                touch++
                            },
                            dragEnd = {
                                p.seekTo(dragVal.toLong())
                                dragging = false
                            },
                            mute = {
                                muted = !muted
                                p.volume = if (muted) 0f else 1f
                                touch++
                            },
                            audioOnly = {
                                audioOnly = !audioOnly
                                p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                                    .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, audioOnly)
                                    .build()
                                touch++
                            },
                            rotate = {
                                rotLocked = !rotLocked
                                act.rotate(rotLocked)
                                flash = if (rotLocked) "Rotation locked" else "Rotation auto"
                                flashSide = 0
                            },
                            lock = {
                                locked = true
                                controls = true
                                touch++
                            },
                            skip = { skip(it) },
                            toggle = { toggle() },
                            aspect = {
                                resize = when (resize) {
                                    AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                }
                                flash = when (resize) {
                                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Zoom"
                                    AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Stretch"
                                    else -> "Fit"
                                }
                                flashSide = 0
                            }
                        ),
                        act
                    )
                }

                // Up next card
                val next = queue.getOrNull(index + 1)
                if (next != null && dur > 0L && (dur - pos) in 1L..20_000L && sheet.isEmpty()) {
                    Column(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 20.dp, bottom = if (controls) 170.dp else 24.dp)
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
                MoreSheet(
                    p = p,
                    fx = fx,
                    speed = speed,
                    onSpeed = {
                        speed = it
                        p.setPlaybackSpeed(it)
                    },
                    sleepMin = sleepMin,
                    sleepEnd = sleepEnd,
                    onSleep = { m, end ->
                        sleepMin = m
                        sleepEnd = end
                    },
                    aPos = aPos,
                    bPos = bPos,
                    onAb = { a, b ->
                        aPos = a
                        bPos = b
                    },
                    onClose = { sheet = "" }
                )
            }
            if (sheet == "tracks") {
                TracksSheet(p, prefs, tracks, { act.pickSub() }, { sheet = "" })
            }
            if (sheet == "queue") {
                QueueSheet(p, index) { sheet = "" }
            }
        }
    }
}
