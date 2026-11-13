package com.mrigank.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Extra icons used by the new home screen, menus and bottom bar. */
enum class Nic { Video, Music, Gear, Sort, Grid, Check, Down, Folder, Share, Info, Trash, Edit, Move }

@Composable
fun Nglyph(ic: Nic, tint: Color, boxSize: Dp = 24.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(boxSize)) {
        val w = size.width
        val h = size.height
        val sw = w * 0.09f
        val st = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (ic) {
            Nic.Video -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.14f, h * 0.24f),
                    size = Size(w * 0.72f, h * 0.52f),
                    cornerRadius = CornerRadius(w * 0.08f),
                    style = st
                )
                val p = Path()
                p.moveTo(w * 0.44f, h * 0.38f)
                p.lineTo(w * 0.44f, h * 0.62f)
                p.lineTo(w * 0.64f, h * 0.50f)
                p.close()
                drawPath(p, tint)
            }
            Nic.Music -> {
                drawLine(tint, Offset(w * 0.62f, h * 0.22f), Offset(w * 0.62f, h * 0.68f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.62f, h * 0.22f), Offset(w * 0.82f, h * 0.32f), sw, StrokeCap.Round)
                drawCircle(tint, w * 0.13f, Offset(w * 0.48f, h * 0.72f))
            }
            Nic.Gear -> {
                val c = Offset(w * 0.5f, h * 0.5f)
                drawCircle(tint, w * 0.20f, c, style = st)
                for (i in 0 until 8) {
                    val a = i * (PI / 4).toFloat()
                    val x = cos(a)
                    val y = sin(a)
                    drawLine(
                        tint,
                        Offset(c.x + x * w * 0.30f, c.y + y * h * 0.30f),
                        Offset(c.x + x * w * 0.42f, c.y + y * h * 0.42f),
                        sw * 1.4f,
                        StrokeCap.Round
                    )
                }
            }
            Nic.Sort -> {
                drawLine(tint, Offset(w * 0.32f, h * 0.20f), Offset(w * 0.32f, h * 0.80f), sw, StrokeCap.Round)
                val a = Path()
                a.moveTo(w * 0.20f, h * 0.34f)
                a.lineTo(w * 0.32f, h * 0.20f)
                a.lineTo(w * 0.44f, h * 0.34f)
                drawPath(a, tint, style = st)
                drawLine(tint, Offset(w * 0.68f, h * 0.20f), Offset(w * 0.68f, h * 0.80f), sw, StrokeCap.Round)
                val b = Path()
                b.moveTo(w * 0.56f, h * 0.66f)
                b.lineTo(w * 0.68f, h * 0.80f)
                b.lineTo(w * 0.80f, h * 0.66f)
                drawPath(b, tint, style = st)
            }
            Nic.Grid -> {
                val s = Size(w * 0.28f, h * 0.28f)
                val r = CornerRadius(w * 0.05f)
                drawRoundRect(tint, Offset(w * 0.16f, h * 0.16f), s, r, st)
                drawRoundRect(tint, Offset(w * 0.56f, h * 0.16f), s, r, st)
                drawRoundRect(tint, Offset(w * 0.16f, h * 0.56f), s, r, st)
                drawRoundRect(tint, Offset(w * 0.56f, h * 0.56f), s, r, st)
            }
            Nic.Check -> {
                val p = Path()
                p.moveTo(w * 0.20f, h * 0.54f)
                p.lineTo(w * 0.42f, h * 0.74f)
                p.lineTo(w * 0.80f, h * 0.30f)
                drawPath(p, tint, style = Stroke(width = sw * 1.3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            Nic.Down -> {
                val p = Path()
                p.moveTo(w * 0.26f, h * 0.38f)
                p.lineTo(w * 0.50f, h * 0.62f)
                p.lineTo(w * 0.74f, h * 0.38f)
                drawPath(p, tint, style = Stroke(width = sw * 1.3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            Nic.Folder -> {
                val p = Path()
                p.moveTo(w * 0.12f, h * 0.28f)
                p.lineTo(w * 0.40f, h * 0.28f)
                p.lineTo(w * 0.48f, h * 0.38f)
                p.lineTo(w * 0.88f, h * 0.38f)
                p.lineTo(w * 0.88f, h * 0.76f)
                p.lineTo(w * 0.12f, h * 0.76f)
                p.close()
                drawPath(p, tint, style = st)
            }
            Nic.Share -> {
                drawLine(tint, Offset(w * 0.30f, h * 0.50f), Offset(w * 0.70f, h * 0.28f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.30f, h * 0.50f), Offset(w * 0.70f, h * 0.72f), sw, StrokeCap.Round)
                drawCircle(tint, w * 0.10f, Offset(w * 0.30f, h * 0.50f))
                drawCircle(tint, w * 0.10f, Offset(w * 0.70f, h * 0.28f))
                drawCircle(tint, w * 0.10f, Offset(w * 0.70f, h * 0.72f))
            }
            Nic.Info -> {
                drawCircle(tint, w * 0.36f, Offset(w * 0.5f, h * 0.5f), style = st)
                drawLine(tint, Offset(w * 0.5f, h * 0.46f), Offset(w * 0.5f, h * 0.68f), sw, StrokeCap.Round)
                drawCircle(tint, w * 0.045f, Offset(w * 0.5f, h * 0.33f))
            }
            Nic.Trash -> {
                drawLine(tint, Offset(w * 0.20f, h * 0.28f), Offset(w * 0.80f, h * 0.28f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.40f, h * 0.18f), Offset(w * 0.60f, h * 0.18f), sw, StrokeCap.Round)
                val p = Path()
                p.moveTo(w * 0.28f, h * 0.28f)
                p.lineTo(w * 0.33f, h * 0.82f)
                p.lineTo(w * 0.67f, h * 0.82f)
                p.lineTo(w * 0.72f, h * 0.28f)
                drawPath(p, tint, style = st)
            }
            Nic.Edit -> {
                drawLine(tint, Offset(w * 0.26f, h * 0.74f), Offset(w * 0.74f, h * 0.26f), sw * 2.2f, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.20f, h * 0.84f), Offset(w * 0.50f, h * 0.84f), sw, StrokeCap.Round)
            }
            Nic.Move -> {
                drawLine(tint, Offset(w * 0.20f, h * 0.50f), Offset(w * 0.80f, h * 0.50f), sw, StrokeCap.Round)
                val p = Path()
                p.moveTo(w * 0.58f, h * 0.30f)
                p.lineTo(w * 0.80f, h * 0.50f)
                p.lineTo(w * 0.58f, h * 0.70f)
                drawPath(p, tint, style = st)
            }
        }
    }
}
