package com.mrigank.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import kotlin.math.max

/** What the controls show right now. */
internal class CtlInfo(
    val title: String,
    val playing: Boolean,
    val pos: Long,
    val dur: Long,
    val dragging: Boolean,
    val dragVal: Float,
    val muted: Boolean,
    val audioOnly: Boolean,
    val rotLocked: Boolean
)

/** What the controls can do. */
internal class CtlActions(
    val sheet: (String) -> Unit,
    val drag: (Float) -> Unit,
    val dragEnd: () -> Unit,
    val mute: () -> Unit,
    val audioOnly: () -> Unit,
    val rotate: () -> Unit,
    val lock: () -> Unit,
    val skip: (Long) -> Unit,
    val toggle: () -> Unit,
    val aspect: () -> Unit
)

@Composable
internal fun PlayerControls(
    p: ExoPlayer,
    prefs: Prefs,
    info: CtlInfo,
    c: CtlActions,
    act: PlayerActions
) {
    val pal = LocalPal.current
    Box(Modifier.fillMaxSize()) {
        // Faint shading only, no bar
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(96.dp)
                .background(Brush.verticalGradient(listOf(Color(0x73000000), Color.Transparent)))
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(170.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x8C000000))))
        )

        // Top row
        Row(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.displayCutout)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlBtn(PlIc.Back, { act.back() })
            PlBtn(PlIc.Queue, { c.sheet("queue") })
            Box(Modifier.weight(1f).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                T(info.title, 13, Color(0xC7FFFFFF), FontWeight.Medium)
            }
            PlBtn(PlIc.Cast, { act.cast() })
            PlBtn(if (info.muted) PlIc.Mute else PlIc.Vol, { c.mute() })
            PlBtn(PlIc.Rotate, { c.rotate() }, if (info.rotLocked) pal.accent else Color.White)
        }

        // Bottom area: no background, everything floats on the video
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.displayCutout)
                .padding(horizontal = 16.dp)
                .padding(bottom = 6.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                PlBtn(PlIc.Note, { c.audioOnly() }, if (info.audioOnly) pal.accent else Color.White)
                PlBtn(PlIc.CC, { c.sheet("tracks") })
                PlBtn(PlIc.Phones, { c.sheet("tracks") })
                PlBtn(PlIc.Pip, { act.pip() })
                PlBtn(PlIc.Speed, { c.sheet("more") })
                PlBtn(PlIc.Gear, { c.sheet("more") })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                val shown = if (info.dragging) info.dragVal.toLong() else info.pos
                T(fmt(shown), 12, Color.White)
                PlSeek(
                    value = if (info.dragging) info.dragVal else info.pos.toFloat(),
                    maxV = max(info.dur, 1L).toFloat(),
                    onChange = { c.drag(it) },
                    onEnd = { c.dragEnd() },
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                )
                T("-" + fmt((info.dur - shown).coerceAtLeast(0L)), 12, Color.White)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlBlue("−${prefs.skipLong}") { c.skip(-prefs.skipLong * 1000L) }
                PlBtn(PlIc.Lock, { c.lock() })
                PlSkip(false, prefs.skipShort) { c.skip(-prefs.skipShort * 1000L) }
                PlBtn(PlIc.Prev, { p.seekToPreviousMediaItem() })
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .clickable { c.toggle() },
                    contentAlignment = Alignment.Center
                ) {
                    PlGlyph(if (info.playing) PlIc.Pause else PlIc.Play, Color.White, 46.dp)
                }
                PlBtn(PlIc.Next, { p.seekToNextMediaItem() })
                PlSkip(true, prefs.skipShort) { c.skip(prefs.skipShort * 1000L) }
                PlBtn(PlIc.Aspect, { c.aspect() })
                PlBlue("+${prefs.skipLong}") { c.skip(prefs.skipLong * 1000L) }
            }
        }
    }
}

/** Thin Arc-style seek line with a small round handle. */
@Composable
internal fun PlSeek(
    value: Float,
    maxV: Float,
    onChange: (Float) -> Unit,
    onEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var w by remember { mutableIntStateOf(1) }
    val frac = (value / maxV).coerceIn(0f, 1f)
    Box(
        modifier
            .height(36.dp)
            .onSizeChanged { w = max(it.width, 1) }
            .pointerInput(maxV) {
                detectTapGestures { off ->
                    onChange((off.x / size.width * maxV).coerceIn(0f, maxV))
                    onEnd()
                }
            }
            .pointerInput(maxV) {
                detectHorizontalDragGestures(
                    onDragStart = { off -> onChange((off.x / size.width * maxV).coerceIn(0f, maxV)) },
                    onDragEnd = { onEnd() },
                    onDragCancel = { onEnd() }
                ) { change, _ ->
                    change.consume()
                    onChange((change.position.x / size.width * maxV).coerceIn(0f, maxV))
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0x66FFFFFF))
        )
        Box(
            Modifier
                .fillMaxWidth(frac)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White)
        )
        Box(
            Modifier
                .offset { IntOffset((w * frac - 6.dp.toPx()).toInt(), 0) }
                .size(12.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
