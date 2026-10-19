package com.baiyue.medicine.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** A compact, hand-drawn glyph set so the app keeps one consistent visual language. */
enum class AppGlyph {
    Settings, Back, ChevronLeft, ChevronRight, ChevronDown, ChevronUp,
    Plus, Minus, Check, Clock, Pill, Calendar, Sparkle, Bell, Data, Info,
    Image, Edit, Trash, Close, Undo, Heart, Chart, Download, Upload, Shield,
    Battery, Language, Search, Star, Leaf, Sun, ArrowRight
}

@Composable
fun GlyphIcon(
    glyph: AppGlyph,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: Dp = 22.dp,
    contentDescription: String? = null
) {
    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }
    Canvas(modifier.size(size).then(semanticsModifier)) {
        drawGlyph(glyph, tint)
    }
}

private fun DrawScope.drawGlyph(glyph: AppGlyph, tint: Color) {
    val unit = size.minDimension / 24f
    val ox = (size.width - 24f * unit) / 2f
    val oy = (size.height - 24f * unit) / 2f
    val stroke = 1.75f * unit
    val thin = 1.45f * unit

    fun p(x: Float, y: Float) = Offset(ox + x * unit, oy + y * unit)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float, width: Float = stroke) {
        drawLine(tint, p(x1, y1), p(x2, y2), width, StrokeCap.Round)
    }
    fun circle(cx: Float, cy: Float, r: Float, fill: Boolean = false, width: Float = stroke) {
        drawCircle(tint, r * unit, p(cx, cy), style = if (fill) Fill else Stroke(width, cap = StrokeCap.Round))
    }
    fun roundRect(x: Float, y: Float, w: Float, h: Float, r: Float, fill: Boolean = false, width: Float = stroke) {
        drawRoundRect(
            color = tint,
            topLeft = p(x, y),
            size = Size(w * unit, h * unit),
            cornerRadius = CornerRadius(r * unit, r * unit),
            style = if (fill) Fill else Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
    fun path(block: Path.() -> Unit, fill: Boolean = false, width: Float = stroke) {
        val path = Path().apply(block)
        drawPath(path, tint, style = if (fill) Fill else Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    when (glyph) {
        AppGlyph.Settings -> {
            repeat(8) { index ->
                val angle = Math.toRadians((index * 45).toDouble())
                val inner = 7.3f
                val outer = 9.4f
                val cx = 12f + cos(angle).toFloat() * inner
                val cy = 12f + sin(angle).toFloat() * inner
                val ex = 12f + cos(angle).toFloat() * outer
                val ey = 12f + sin(angle).toFloat() * outer
                drawLine(tint, p(cx, cy), p(ex, ey), stroke, StrokeCap.Round)
            }
            circle(12f, 12f, 5.3f)
            circle(12f, 12f, 2.1f, fill = true)
        }

        AppGlyph.Back -> {
            line(15f, 4.5f, 7.5f, 12f)
            line(7.5f, 12f, 15f, 19.5f)
        }

        AppGlyph.ChevronLeft -> {
            line(14.5f, 5.5f, 8f, 12f)
            line(8f, 12f, 14.5f, 18.5f)
        }

        AppGlyph.ChevronRight -> {
            line(9.5f, 5.5f, 16f, 12f)
            line(16f, 12f, 9.5f, 18.5f)
        }

        AppGlyph.ChevronDown -> {
            line(5.5f, 9.5f, 12f, 16f)
            line(12f, 16f, 18.5f, 9.5f)
        }

        AppGlyph.ChevronUp -> {
            line(5.5f, 14.5f, 12f, 8f)
            line(12f, 8f, 18.5f, 14.5f)
        }

        AppGlyph.Plus -> {
            line(12f, 5f, 12f, 19f, width = stroke * 1.05f)
            line(5f, 12f, 19f, 12f, width = stroke * 1.05f)
        }

        AppGlyph.Minus -> line(5f, 12f, 19f, 12f, width = stroke * 1.05f)

        AppGlyph.Check -> {
            line(4.8f, 12.4f, 9.7f, 17.2f, width = stroke * 1.2f)
            line(9.7f, 17.2f, 19.2f, 6.8f, width = stroke * 1.2f)
        }

        AppGlyph.Clock -> {
            circle(12f, 12f, 8.2f)
            line(12f, 7.2f, 12f, 12.2f, width = thin)
            line(12f, 12.2f, 15.7f, 14.4f, width = thin)
        }

        AppGlyph.Pill -> {
            rotate(-42f, pivot = p(12f, 12f)) {
                drawRoundRect(
                    color = tint,
                    topLeft = p(3.4f, 8.1f),
                    size = Size(17.2f * unit, 7.8f * unit),
                    cornerRadius = CornerRadius(3.9f * unit, 3.9f * unit),
                    style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawLine(tint, p(12f, 8.2f), p(12f, 15.8f), thin, StrokeCap.Round)
                drawCircle(
                    tint,
                    3.7f * unit,
                    p(7f, 12f),
                    style = Stroke(thin, cap = StrokeCap.Round)
                )
            }
        }

        AppGlyph.Calendar -> {
            roundRect(3.2f, 5.2f, 17.6f, 15.4f, 3.2f)
            line(3.6f, 9.6f, 20.4f, 9.6f, width = thin)
            line(7.9f, 3f, 7.9f, 6.9f, width = thin)
            line(16.1f, 3f, 16.1f, 6.9f, width = thin)
            circle(8f, 14f, 1.15f, fill = true)
            circle(12f, 14f, 1.15f, fill = true)
            circle(16f, 14f, 1.15f, fill = true)
        }

        AppGlyph.Sparkle -> {
            path({
                moveTo(p(12f, 2.8f))
                lineTo(p(13.9f, 9.8f))
                lineTo(p(21.2f, 12f))
                lineTo(p(13.9f, 14.2f))
                lineTo(p(12f, 21.2f))
                lineTo(p(10.1f, 14.2f))
                lineTo(p(2.8f, 12f))
                lineTo(p(10.1f, 9.8f))
                close()
            }, fill = true)
        }

        AppGlyph.Bell -> {
            path({
                moveTo(p(5.6f, 17.2f))
                cubicTo(p(6.4f, 15.2f), p(6.5f, 14.2f), p(6.5f, 11.4f))
                cubicTo(p(6.5f, 7.2f), p(8.8f, 4.5f), p(12f, 4.5f))
                cubicTo(p(15.2f, 4.5f), p(17.5f, 7.2f), p(17.5f, 11.4f))
                cubicTo(p(17.5f, 14.2f), p(17.6f, 15.2f), p(18.4f, 17.2f))
                lineTo(p(5.6f, 17.2f))
                close()
            })
            line(9.9f, 20.1f, 14.1f, 20.1f, width = thin)
        }

        AppGlyph.Data -> {
            drawOval(tint, p(4.4f, 4.6f), Size(15.2f * unit, 4.4f * unit), style = Stroke(stroke))
            line(4.4f, 6.8f, 4.4f, 17.3f, width = thin)
            line(19.6f, 6.8f, 19.6f, 17.3f, width = thin)
            drawOval(tint, p(4.4f, 15.2f), Size(15.2f * unit, 4.4f * unit), style = Stroke(thin))
            drawArc(
                tint, 0f, 180f, false, p(4.4f, 8.5f), Size(15.2f * unit, 6.6f * unit),
                style = Stroke(thin)
            )
        }

        AppGlyph.Info -> {
            circle(12f, 12f, 8.4f)
            circle(12f, 8.1f, 1f, fill = true)
            line(12f, 11.4f, 12f, 16.6f, width = stroke * 1.05f)
        }

        AppGlyph.Image -> {
            roundRect(3.2f, 4.4f, 17.6f, 15.2f, 3f)
            circle(8.4f, 9.3f, 1.55f)
            path({
                moveTo(p(3.8f, 17.3f))
                lineTo(p(9.2f, 12.2f))
                lineTo(p(12.6f, 15.2f))
                lineTo(p(15.6f, 12.4f))
                lineTo(p(20.2f, 17.3f))
            }, width = thin)
        }

        AppGlyph.Edit -> {
            path({
                moveTo(p(4f, 20f))
                lineTo(p(5f, 15.4f))
                lineTo(p(16.2f, 4.2f))
                cubicTo(p(16.9f, 3.5f), p(18f, 3.5f), p(18.7f, 4.2f))
                lineTo(p(19.8f, 5.3f))
                cubicTo(p(20.5f, 6f), p(20.5f, 7.1f), p(19.8f, 7.8f))
                lineTo(p(8.6f, 19f))
                close()
            })
            line(14.8f, 5.8f, 18.2f, 9.2f, width = thin)
        }

        AppGlyph.Trash -> {
            line(4.6f, 6.8f, 19.4f, 6.8f)
            line(9f, 3.7f, 15f, 3.7f, width = thin)
            path({
                moveTo(p(6.4f, 7.2f))
                lineTo(p(7.4f, 19.6f))
                cubicTo(p(7.5f, 20.5f), p(8.2f, 21f), p(9f, 21f))
                lineTo(p(15f, 21f))
                cubicTo(p(15.8f, 21f), p(16.5f, 20.5f), p(16.6f, 19.6f))
                lineTo(p(17.6f, 7.2f))
            })
            line(10.4f, 10.4f, 10.9f, 17.5f, width = thin)
            line(13.6f, 10.4f, 13.1f, 17.5f, width = thin)
        }

        AppGlyph.Close -> {
            line(6f, 6f, 18f, 18f)
            line(18f, 6f, 6f, 18f)
        }

        AppGlyph.Undo -> {
            drawArc(
                tint, 150f, -250f, false, p(4.5f, 5.2f), Size(15f * unit, 15f * unit),
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            line(4.8f, 5.2f, 4.8f, 10.2f, width = thin)
            line(4.8f, 10.2f, 9.8f, 10.2f, width = thin)
        }

        AppGlyph.Heart -> {
            path({
                moveTo(p(12f, 20.2f))
                cubicTo(p(10.5f, 18.8f), p(3.2f, 14.1f), p(3.2f, 8.6f))
                cubicTo(p(3.2f, 5.6f), p(5.4f, 3.6f), p(8.1f, 3.6f))
                cubicTo(p(9.9f, 3.6f), p(11.4f, 4.7f), p(12f, 6.1f))
                cubicTo(p(12.6f, 4.7f), p(14.1f, 3.6f), p(15.9f, 3.6f))
                cubicTo(p(18.6f, 3.6f), p(20.8f, 5.6f), p(20.8f, 8.6f))
                cubicTo(p(20.8f, 14.1f), p(13.5f, 18.8f), p(12f, 20.2f))
                close()
            }, fill = true)
        }

        AppGlyph.Chart -> {
            line(4.4f, 19.6f, 20f, 19.6f, width = thin)
            line(7.4f, 19.2f, 7.4f, 12.8f, width = stroke * 1.2f)
            line(12f, 19.2f, 12f, 7.2f, width = stroke * 1.2f)
            line(16.6f, 19.2f, 16.6f, 10.4f, width = stroke * 1.2f)
        }

        AppGlyph.Download -> {
            line(12f, 3.8f, 12f, 15.4f)
            line(7.2f, 10.8f, 12f, 15.6f)
            line(16.8f, 10.8f, 12f, 15.6f)
            line(4.6f, 19.8f, 19.4f, 19.8f, width = thin)
        }

        AppGlyph.Upload -> {
            line(12f, 15.8f, 12f, 4.2f)
            line(7.2f, 8.8f, 12f, 4f)
            line(16.8f, 8.8f, 12f, 4f)
            line(4.6f, 19.8f, 19.4f, 19.8f, width = thin)
        }

        AppGlyph.Shield -> {
            path({
                moveTo(p(12f, 3f))
                lineTo(p(19.6f, 6.2f))
                lineTo(p(19.6f, 11.5f))
                cubicTo(p(19.6f, 16.4f), p(16.3f, 19.5f), p(12f, 21.2f))
                cubicTo(p(7.7f, 19.5f), p(4.4f, 16.4f), p(4.4f, 11.5f))
                lineTo(p(4.4f, 6.2f))
                close()
            })
            line(8.6f, 12f, 11.2f, 14.6f, width = thin)
            line(11.2f, 14.6f, 15.7f, 9.7f, width = thin)
        }

        AppGlyph.Battery -> {
            roundRect(3.2f, 7.4f, 16.4f, 9.2f, 2.6f)
            line(21f, 10.4f, 21f, 13.6f, width = 2.3f * unit)
            line(6.3f, 10.2f, 6.3f, 13.8f, width = thin)
            line(9.4f, 10.2f, 9.4f, 13.8f, width = thin)
            line(12.5f, 10.2f, 12.5f, 13.8f, width = thin)
        }

        AppGlyph.Language -> {
            circle(12f, 12f, 8.4f)
            drawOval(tint, p(7.4f, 4.2f), Size(9.2f * unit, 15.6f * unit), style = Stroke(thin))
            line(3.9f, 12f, 20.1f, 12f, width = thin)
            drawArc(tint, 200f, 140f, false, p(3.9f, 4.2f), Size(16.2f * unit, 15.6f * unit), style = Stroke(thin))
        }

        AppGlyph.Search -> {
            circle(10.7f, 10.7f, 6.5f)
            line(15.4f, 15.4f, 20.2f, 20.2f, width = stroke * 1.1f)
        }

        AppGlyph.Star -> {
            path({
                moveTo(p(12f, 3.2f))
                lineTo(p(14.7f, 8.9f))
                lineTo(p(21f, 9.8f))
                lineTo(p(16.4f, 14.2f))
                lineTo(p(17.6f, 20.4f))
                lineTo(p(12f, 17.4f))
                lineTo(p(6.4f, 20.4f))
                lineTo(p(7.6f, 14.2f))
                lineTo(p(3f, 9.8f))
                lineTo(p(9.3f, 8.9f))
                close()
            }, fill = true)
        }

        AppGlyph.Leaf -> {
            path({
                moveTo(p(4.2f, 19.8f))
                cubicTo(p(4.4f, 9.8f), p(10.2f, 4f), p(20f, 3.8f))
                cubicTo(p(19.8f, 13.6f), p(14f, 19.4f), p(4.2f, 19.8f))
                close()
            })
            line(4.6f, 19.4f, 15.2f, 8.8f, width = thin)
        }

        AppGlyph.Sun -> {
            circle(12f, 12f, 4.2f)
            repeat(8) { index ->
                val angle = Math.toRadians((index * 45).toDouble())
                val sx = 12f + cos(angle).toFloat() * 6.8f
                val sy = 12f + sin(angle).toFloat() * 6.8f
                val ex = 12f + cos(angle).toFloat() * 9.2f
                val ey = 12f + sin(angle).toFloat() * 9.2f
                drawLine(tint, p(sx, sy), p(ex, ey), thin, StrokeCap.Round)
            }
        }

        AppGlyph.ArrowRight -> {
            line(4.4f, 12f, 19.2f, 12f)
            line(13.6f, 6.4f, 19.2f, 12f)
            line(19.2f, 12f, 13.6f, 17.6f)
        }
    }
}

private fun Path.moveTo(offset: Offset) = moveTo(offset.x, offset.y)

private fun Path.lineTo(offset: Offset) = lineTo(offset.x, offset.y)

private fun Path.cubicTo(p1: Offset, p2: Offset, p3: Offset) =
    cubicTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)
