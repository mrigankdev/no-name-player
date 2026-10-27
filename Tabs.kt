package com.mrigank.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
private fun TabIcon(kind: Int, tint: Color) {
    Canvas(Modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val sw = w * 0.09f
        val st = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (kind) {
            0 -> {
                val roof = Path()
                roof.moveTo(w * 0.14f, h * 0.50f)
                roof.lineTo(w * 0.50f, h * 0.18f)
                roof.lineTo(w * 0.86f, h * 0.50f)
                drawPath(roof, tint, style = st)
                val body = Path()
                body.moveTo(w * 0.24f, h * 0.44f)
                body.lineTo(w * 0.24f, h * 0.82f)
                body.lineTo(w * 0.76f, h * 0.82f)
                body.lineTo(w * 0.76f, h * 0.44f)
                drawPath(body, tint, style = st)
            }
            1 -> {
                drawRoundRect(tint, Offset(w * 0.14f, h * 0.20f), Size(w * 0.72f, h * 0.60f), CornerRadius(w * 0.08f), style = st)
                val p = Path()
                p.moveTo(w * 0.42f, h * 0.36f)
                p.lineTo(w * 0.42f, h * 0.64f)
                p.lineTo(w * 0.66f, h * 0.50f)
                p.close()
                drawPath(p, tint)
            }
            2 -> {
                drawCircle(tint, w * 0.36f, Offset(w * 0.5f, h * 0.5f), style = st)
                drawCircle(tint, w * 0.05f, Offset(w * 0.38f, h * 0.42f))
                drawCircle(tint, w * 0.05f, Offset(w * 0.56f, h * 0.36f))
                drawCircle(tint, w * 0.05f, Offset(w * 0.64f, h * 0.54f))
            }
            else -> {
                drawCircle(tint, w * 0.15f, Offset(w * 0.5f, h * 0.5f), style = st)
                for (i in 0 until 8) {
                    val a = (i * PI / 4).toFloat()
                    drawLine(
                        tint,
                        Offset(w * 0.5f + cos(a) * w * 0.26f, h * 0.5f + sin(a) * h * 0.26f),
                        Offset(w * 0.5f + cos(a) * w * 0.38f, h * 0.5f + sin(a) * h * 0.38f),
                        sw * 1.4f,
                        StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val pal = LocalPal.current
    val labels = listOf("Home", "Videos", "Look", "Settings")
    Row(
        Modifier
            .fillMaxWidth()
            .background(pal.surface)
            .navigationBarsPadding()
            .height(60.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in labels.indices) {
            val c = if (i == selected) pal.accent else pal.muted
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onSelect(i) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                TabIcon(i, c)
                Spacer(Modifier.height(3.dp))
                T(labels[i], 11, c, if (i == selected) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}

@Composable
fun VideosScreen(
    videos: List<Video>,
    tick: Int,
    prefs: Prefs,
    onPlay: (List<Video>, Int) -> Unit
) {
    val pal = LocalPal.current
    val list = remember(videos) { videos.sortedByDescending { it.dateAdded } }
    Column(Modifier.fillMaxSize().background(pal.bg)) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            T("Videos", 24, pal.onBg, FontWeight.Bold)
        }
        if (list.isEmpty()) {
            Row(Modifier.padding(20.dp)) {
                T("No videos found yet.", 16, pal.muted)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                itemsIndexed(list, key = { _, v -> v.id }) { i, v ->
                    EpisodeRow(v, tick, prefs, false) { onPlay(list, i) }
                }
            }
        }
    }
}
