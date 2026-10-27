package com.mrigank.player

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

fun fmt(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val s = ms / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) {
        String.format("%d:%02d:%02d", h, m, sec)
    } else {
        String.format("%d:%02d", m, sec)
    }
}

@Composable
fun T(
    text: String,
    size: Int = 15,
    color: Color = LocalPal.current.onBg,
    weight: FontWeight = FontWeight.Normal,
    maxLines: Int = 1,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size.sp,
        fontWeight = weight,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

enum class Ic { Play, Pause, Back, Search, More, Close, Lock, List, Pip }

@Composable
fun Glyph(ic: Ic, tint: Color, boxSize: Dp = 24.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(boxSize)) {
        val w = size.width
        val h = size.height
        val sw = w * 0.09f
        when (ic) {
            Ic.Play -> {
                val p = Path()
                p.moveTo(w * 0.30f, h * 0.20f)
                p.lineTo(w * 0.30f, h * 0.80f)
                p.lineTo(w * 0.82f, h * 0.50f)
                p.close()
                drawPath(p, tint)
            }
            Ic.Pause -> {
                drawRoundRect(tint, Offset(w * 0.24f, h * 0.20f), Size(w * 0.18f, h * 0.60f), CornerRadius(w * 0.04f))
                drawRoundRect(tint, Offset(w * 0.58f, h * 0.20f), Size(w * 0.18f, h * 0.60f), CornerRadius(w * 0.04f))
            }
            Ic.Back -> {
                val p = Path()
                p.moveTo(w * 0.62f, h * 0.20f)
                p.lineTo(w * 0.30f, h * 0.50f)
                p.lineTo(w * 0.62f, h * 0.80f)
                drawPath(p, tint, style = Stroke(width = sw * 1.3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            Ic.Search -> {
                drawCircle(tint, w * 0.28f, Offset(w * 0.43f, h * 0.43f), style = Stroke(width = sw))
                drawLine(tint, Offset(w * 0.64f, h * 0.64f), Offset(w * 0.85f, h * 0.85f), sw, StrokeCap.Round)
            }
            Ic.More -> {
                drawCircle(tint, w * 0.07f, Offset(w * 0.5f, h * 0.2f))
                drawCircle(tint, w * 0.07f, Offset(w * 0.5f, h * 0.5f))
                drawCircle(tint, w * 0.07f, Offset(w * 0.5f, h * 0.8f))
            }
            Ic.Close -> {
                drawLine(tint, Offset(w * 0.25f, h * 0.25f), Offset(w * 0.75f, h * 0.75f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.75f, h * 0.25f), Offset(w * 0.25f, h * 0.75f), sw, StrokeCap.Round)
            }
            Ic.Lock -> {
                drawRoundRect(tint, Offset(w * 0.22f, h * 0.45f), Size(w * 0.56f, h * 0.40f), CornerRadius(w * 0.06f))
                drawArc(
                    color = tint,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.32f, h * 0.15f),
                    size = Size(w * 0.36f, h * 0.50f),
                    style = Stroke(width = sw)
                )
            }
            Ic.List -> {
                drawLine(tint, Offset(w * 0.20f, h * 0.28f), Offset(w * 0.80f, h * 0.28f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.20f, h * 0.50f), Offset(w * 0.80f, h * 0.50f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.20f, h * 0.72f), Offset(w * 0.80f, h * 0.72f), sw, StrokeCap.Round)
            }
            Ic.Pip -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.12f, h * 0.22f),
                    size = Size(w * 0.76f, h * 0.56f),
                    cornerRadius = CornerRadius(w * 0.06f),
                    style = Stroke(width = sw)
                )
                drawRoundRect(tint, Offset(w * 0.50f, h * 0.46f), Size(w * 0.30f, h * 0.24f), CornerRadius(w * 0.03f))
            }
        }
    }
}

@Composable
fun IconBtn(ic: Ic, tint: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Glyph(ic, tint, 22.dp)
    }
}

@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val pal = LocalPal.current
    Box(
        Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) pal.accent else pal.onBg.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        T(label, 14, if (selected) pal.onAccent else pal.onBg, FontWeight.SemiBold)
    }
}

/** Loads small preview pictures from the videos themselves. */
object Thumbs {
    private val cache = LruCache<Long, Bitmap>(60)
    private val gate = Semaphore(2)

    suspend fun get(context: Context, v: Video): Bitmap? {
        val hit = cache.get(v.id)
        if (hit != null) return hit
        return gate.withPermit {
            val again = cache.get(v.id)
            if (again != null) {
                again
            } else {
                val loaded = withContext(Dispatchers.IO) { load(context, v) }
                if (loaded != null) cache.put(v.id, loaded)
                loaded
            }
        }
    }

    private fun load(context: Context, v: Video): Bitmap? {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(context, v.uri)
            val atMicros = (v.durationMs / 10).coerceAtMost(90_000L) * 1000L
            val raw = r.getFrameAtTime(atMicros, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: return null
            val w = 320
            val h = (raw.height * w / raw.width.toFloat()).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(raw, w, h, true)
            if (scaled !== raw) raw.recycle()
            return scaled
        } catch (e: Exception) {
            return null
        } finally {
            try {
                r.release()
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}

@Composable
fun Thumb(v: Video, modifier: Modifier = Modifier) {
    val pal = LocalPal.current
    val context = LocalContext.current
    val bmp by produceState<Bitmap?>(null, v.id) {
        value = Thumbs.get(context.applicationContext, v)
    }
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(pal.surface)) {
        val b = bmp
        if (b != null) {
            Image(
                bitmap = b.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}
