package com.mrigank.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal enum class PlIc {
    Back, Queue, Cast, Vol, Mute, Rotate, Note, CC, Phones, Pip, Speed, Gear,
    Lock, Prev, Next, Play, Pause, Rew, Fwd, Aspect
}

@Composable
internal fun PlGlyph(ic: PlIc, tint: Color, boxSize: Dp = 24.dp) {
    Canvas(Modifier.size(boxSize)) {
        val w = size.width
        val h = size.height
        val sw = w * 0.085f
        val st = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (ic) {
            PlIc.Back -> {
                drawLine(tint, Offset(w * 0.2f, h * 0.5f), Offset(w * 0.82f, h * 0.5f), sw, StrokeCap.Round)
                val p = Path()
                p.moveTo(w * 0.46f, h * 0.24f)
                p.lineTo(w * 0.2f, h * 0.5f)
                p.lineTo(w * 0.46f, h * 0.76f)
                drawPath(p, tint, style = st)
            }
            PlIc.Queue -> {
                drawLine(tint, Offset(w * 0.2f, h * 0.28f), Offset(w * 0.8f, h * 0.28f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.2f, h * 0.48f), Offset(w * 0.8f, h * 0.48f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.2f, h * 0.68f), Offset(w * 0.46f, h * 0.68f), sw, StrokeCap.Round)
                val p = Path()
                p.moveTo(w * 0.6f, h * 0.58f)
                p.lineTo(w * 0.6f, h * 0.8f)
                p.lineTo(w * 0.82f, h * 0.69f)
                p.close()
                drawPath(p, tint)
            }
            PlIc.Cast -> {
                val p = Path()
                p.moveTo(w * 0.16f, h * 0.26f)
                p.lineTo(w * 0.84f, h * 0.26f)
                p.lineTo(w * 0.84f, h * 0.74f)
                p.lineTo(w * 0.56f, h * 0.74f)
                drawPath(p, tint, style = st)
                for (r in listOf(0.14f, 0.28f, 0.42f)) {
                    drawArc(
                        color = tint,
                        startAngle = 270f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(w * (0.16f - r), h * (0.74f - r)),
                        size = Size(w * 2f * r, h * 2f * r),
                        style = st
                    )
                }
                drawCircle(tint, w * 0.04f, Offset(w * 0.16f, h * 0.74f))
            }
            PlIc.Vol, PlIc.Mute -> {
                val p = Path()
                p.moveTo(w * 0.14f, h * 0.4f)
                p.lineTo(w * 0.3f, h * 0.4f)
                p.lineTo(w * 0.52f, h * 0.2f)
                p.lineTo(w * 0.52f, h * 0.8f)
                p.lineTo(w * 0.3f, h * 0.6f)
                p.lineTo(w * 0.14f, h * 0.6f)
                p.close()
                drawPath(p, tint)
                if (ic == PlIc.Vol) {
                    drawArc(tint, -50f, 100f, false, Offset(w * 0.4f, h * 0.28f), Size(w * 0.44f, h * 0.44f), style = st)
                } else {
                    drawLine(tint, Offset(w * 0.64f, h * 0.38f), Offset(w * 0.88f, h * 0.62f), sw, StrokeCap.Round)
                    drawLine(tint, Offset(w * 0.88f, h * 0.38f), Offset(w * 0.64f, h * 0.62f), sw, StrokeCap.Round)
                }
            }
            PlIc.Rotate -> {
                drawArc(tint, 200f, 260f, false, Offset(w * 0.15f, h * 0.15f), Size(w * 0.7f, h * 0.7f), style = st)
                drawRoundRect(tint, Offset(w * 0.38f, h * 0.32f), Size(w * 0.24f, h * 0.36f), CornerRadius(w * 0.04f), style = st)
            }
            PlIc.Note -> {
                drawCircle(tint, w * 0.13f, Offset(w * 0.36f, h * 0.72f))
                drawLine(tint, Offset(w * 0.49f, h * 0.72f), Offset(w * 0.49f, h * 0.2f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.49f, h * 0.2f), Offset(w * 0.78f, h * 0.34f), sw, StrokeCap.Round)
            }
            PlIc.CC -> {
                drawRoundRect(tint, Offset(w * 0.1f, h * 0.22f), Size(w * 0.8f, h * 0.56f), CornerRadius(w * 0.1f), style = st)
                drawArc(tint, 45f, 270f, false, Offset(w * 0.22f, h * 0.38f), Size(w * 0.24f, h * 0.24f), style = st)
                drawArc(tint, 45f, 270f, false, Offset(w * 0.54f, h * 0.38f), Size(w * 0.24f, h * 0.24f), style = st)
            }
            PlIc.Phones -> {
                drawArc(tint, 180f, 180f, false, Offset(w * 0.2f, h * 0.18f), Size(w * 0.6f, h * 0.6f), style = st)
                drawRoundRect(tint, Offset(w * 0.16f, h * 0.5f), Size(w * 0.14f, h * 0.28f), CornerRadius(w * 0.05f))
                drawRoundRect(tint, Offset(w * 0.7f, h * 0.5f), Size(w * 0.14f, h * 0.28f), CornerRadius(w * 0.05f))
            }
            PlIc.Pip -> {
                drawRoundRect(tint, Offset(w * 0.12f, h * 0.22f), Size(w * 0.76f, h * 0.56f), CornerRadius(w * 0.06f), style = st)
                drawRoundRect(tint, Offset(w * 0.5f, h * 0.46f), Size(w * 0.3f, h * 0.24f), CornerRadius(w * 0.03f))
            }
            PlIc.Speed -> {
                drawArc(tint, 150f, 240f, false, Offset(w * 0.12f, h * 0.14f), Size(w * 0.76f, h * 0.76f), style = st)
                drawLine(tint, Offset(w * 0.5f, h * 0.52f), Offset(w * 0.66f, h * 0.34f), sw, StrokeCap.Round)
                drawCircle(tint, w * 0.06f, Offset(w * 0.5f, h * 0.52f))
            }
            PlIc.Gear -> {
                val p = Path()
                p.moveTo(w * 0.12f, h * 0.25f)
                p.lineTo(w * 0.12f, h * 0.75f)
                p.lineTo(w * 0.5f, h * 0.5f)
                p.close()
                drawPath(p, tint, style = st)
                drawCircle(tint, w * 0.17f, Offset(w * 0.72f, h * 0.68f), style = st)
                drawCircle(tint, w * 0.05f, Offset(w * 0.72f, h * 0.68f))
            }
            PlIc.Lock -> {
                drawRoundRect(tint, Offset(w * 0.22f, h * 0.45f), Size(w * 0.56f, h * 0.4f), CornerRadius(w * 0.06f))
                drawArc(tint, 180f, 180f, false, Offset(w * 0.32f, h * 0.15f), Size(w * 0.36f, h * 0.5f), style = st)
            }
            PlIc.Prev -> {
                drawLine(tint, Offset(w * 0.28f, h * 0.25f), Offset(w * 0.28f, h * 0.75f), sw * 1.2f, StrokeCap.Round)
                val p = Path()
                p.moveTo(w * 0.8f, h * 0.25f)
                p.lineTo(w * 0.8f, h * 0.75f)
                p.lineTo(w * 0.4f, h * 0.5f)
                p.close()
                drawPath(p, tint)
            }
            PlIc.Next -> {
                drawLine(tint, Offset(w * 0.72f, h * 0.25f), Offset(w * 0.72f, h * 0.75f), sw * 1.2f, StrokeCap.Round)
                val p = Path()
                p.moveTo(w * 0.2f, h * 0.25f)
                p.lineTo(w * 0.2f, h * 0.75f)
                p.lineTo(w * 0.6f, h * 0.5f)
                p.close()
                drawPath(p, tint)
            }
            PlIc.Play -> {
                val p = Path()
                p.moveTo(w * 0.3f, h * 0.2f)
                p.lineTo(w * 0.3f, h * 0.8f)
                p.lineTo(w * 0.82f, h * 0.5f)
                p.close()
                drawPath(p, tint)
            }
            PlIc.Pause -> {
                drawRoundRect(tint, Offset(w * 0.24f, h * 0.2f), Size(w * 0.18f, h * 0.6f), CornerRadius(w * 0.04f))
                drawRoundRect(tint, Offset(w * 0.58f, h * 0.2f), Size(w * 0.18f, h * 0.6f), CornerRadius(w * 0.04f))
            }
            PlIc.Fwd -> {
                drawArc(tint, -60f, 300f, false, Offset(w * 0.14f, h * 0.18f), Size(w * 0.72f, h * 0.72f), style = st)
                val p = Path()
                p.moveTo(w * 0.3f, h * 0.07f)
                p.lineTo(w * 0.3f, h * 0.37f)
                p.lineTo(w * 0.5f, h * 0.22f)
                p.close()
                drawPath(p, tint)
            }
            PlIc.Rew -> {
                drawArc(tint, 240f, -300f, false, Offset(w * 0.14f, h * 0.18f), Size(w * 0.72f, h * 0.72f), style = st)
                val p = Path()
                p.moveTo(w * 0.7f, h * 0.07f)
                p.lineTo(w * 0.7f, h * 0.37f)
                p.lineTo(w * 0.5f, h * 0.22f)
                p.close()
                drawPath(p, tint)
            }
            PlIc.Aspect -> {
                drawRoundRect(tint, Offset(w * 0.1f, h * 0.26f), Size(w * 0.8f, h * 0.48f), CornerRadius(w * 0.06f), style = st)
                drawRoundRect(tint, Offset(w * 0.28f, h * 0.4f), Size(w * 0.44f, h * 0.2f), CornerRadius(w * 0.03f), style = st)
            }
        }
    }
}

@Composable
internal fun PlBtn(
    ic: PlIc,
    onClick: () -> Unit,
    tint: Color = Color.White,
    boxSize: Dp = 44.dp,
    iconSize: Dp = 24.dp
) {
    Box(
        Modifier.size(boxSize).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        PlGlyph(ic, tint, iconSize)
    }
}

@Composable
internal fun PlSkip(forward: Boolean, secs: Int, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        PlGlyph(if (forward) PlIc.Fwd else PlIc.Rew, Color.White, 32.dp)
        Text(secs.toString(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun PlBlue(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(LocalPal.current.accent.copy(alpha = 0.34f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
