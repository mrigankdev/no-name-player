package com.mrigank.player

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlFlow(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

@Composable
private fun PlTitle(text: String) {
    T(text.uppercase(), 12, LocalPal.current.muted, FontWeight.SemiBold)
}

@Composable
private fun PlRow(label: String, selected: Boolean, onClick: () -> Unit) {
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
private fun PlSheet(onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val pal = LocalPal.current
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0x66000000))
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
internal fun MoreSheet(
    p: ExoPlayer,
    fx: AudioFx,
    speed: Float,
    onSpeed: (Float) -> Unit,
    sleepMin: Int,
    sleepEnd: Boolean,
    onSleep: (Int, Boolean) -> Unit,
    aPos: Long,
    bPos: Long,
    onAb: (Long, Long) -> Unit,
    onClose: () -> Unit
) {
    val pal = LocalPal.current
    PlSheet(onClose) {
        PlTitle("Speed")
        PlFlow {
            for (s in listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)) {
                Chip(plSpeedLabel(s), abs(speed - s) < 0.01f) { onSpeed(s) }
            }
        }

        PlTitle("Sleep timer")
        PlFlow {
            for (m in listOf(0, 10, 15, 30, 45, 60)) {
                Chip(if (m == 0) "Off" else "$m min", sleepMin == m && !sleepEnd) { onSleep(m, false) }
            }
            Chip("End of video", sleepEnd) { onSleep(0, true) }
        }

        PlTitle("A-B repeat")
        PlFlow {
            Chip(if (aPos >= 0L) "A  " + fmt(aPos) else "Set A", aPos >= 0L) {
                val a = p.currentPosition
                onAb(a, if (bPos <= a) -1L else bPos)
            }
            Chip(if (bPos >= 0L) "B  " + fmt(bPos) else "Set B", bPos >= 0L) {
                if (aPos >= 0L && p.currentPosition > aPos) onAb(aPos, p.currentPosition)
            }
            Chip("Clear", false) { onAb(-1L, -1L) }
        }

        Row(Modifier.fillMaxWidth()) {
            T("VOLUME BOOST", 12, pal.muted, FontWeight.SemiBold, 1, Modifier.weight(1f))
            T("${fx.boostPercent}%", 14, pal.onBg, FontWeight.SemiBold)
        }
        Slider(
            value = fx.boostPercent.toFloat(),
            onValueChange = { fx.setBoost(it.toInt()) },
            valueRange = 100f..200f,
            colors = SliderDefaults.colors(
                thumbColor = pal.accent,
                activeTrackColor = pal.accent,
                inactiveTrackColor = pal.onBg.copy(alpha = 0.2f)
            )
        )

        PlTitle("Equalizer")
        val names = fx.eqNames()
        if (names.isEmpty()) {
            T("Not available on this device", 14, pal.muted)
        } else {
            PlFlow {
                Chip("Off", fx.eqPreset < 0) { fx.setEq(-1) }
                for (i in names.indices) {
                    Chip(names[i], fx.eqPreset == i) { fx.setEq(i) }
                }
            }
        }
    }
}

@Composable
internal fun TracksSheet(
    p: ExoPlayer,
    prefs: Prefs,
    tracks: Tracks,
    onPickSub: () -> Unit,
    onClose: () -> Unit
) {
    val pal = LocalPal.current
    PlSheet(onClose) {
        val textGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
        val textOff = p.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
        PlTitle("Subtitles")
        PlRow("Off", textOff || textGroups.none { it.isSelected }) {
            p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
            prefs.subOff = true
            onClose()
        }
        var n = 0
        for (g in textGroups) {
            for (i in 0 until g.length) {
                n++
                val label = plTrackLabel(g.getTrackFormat(i), n)
                PlRow(label, g.isTrackSelected(i) && !textOff) {
                    p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                        .setOverrideForType(TrackSelectionOverride(g.mediaTrackGroup, i))
                        .build()
                    prefs.subOff = false
                    prefs.subLang = g.getTrackFormat(i).language
                    onClose()
                }
            }
        }
        PlRow("Load a subtitle file", false) {
            onClose()
            onPickSub()
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
            PlTitle("Audio")
            var m = 0
            for (g in audioGroups) {
                for (i in 0 until g.length) {
                    m++
                    val label = plTrackLabel(g.getTrackFormat(i), m)
                    PlRow(label, g.isTrackSelected(i)) {
                        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                            .setOverrideForType(TrackSelectionOverride(g.mediaTrackGroup, i))
                            .build()
                        prefs.audioLang = g.getTrackFormat(i).language
                        onClose()
                    }
                }
            }
        }
    }
}

@Composable
internal fun QueueSheet(p: ExoPlayer, index: Int, onClose: () -> Unit) {
    val queue = PlayQueue.videos
    PlSheet(onClose) {
        PlTitle("Playing queue")
        for (i in queue.indices) {
            PlRow(queue[i].name, i == index) {
                p.seekToDefaultPosition(i)
                onClose()
            }
        }
    }
}
