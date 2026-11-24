package com.example.sleepmonitor.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.sleepmonitor.ui.theme.*
import kotlin.math.*

/** Read this state in draw/layer blocks, so continuous artwork does not recompose the UI. */
@Composable internal fun ambientPhase(duration: Int = 24000): State<Float> {
    if (!LocalMotionEnabled.current) return rememberUpdatedState(.8f)
    val loop = rememberInfiniteTransition(label = "ambient-cycle")
    return loop.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(duration, easing = LinearEasing)), label = "ambient-phase")
}

internal fun DrawScope.glow(color: Color, center: Offset, radius: Float, strength: Float) {
    if (radius <= 0f) return
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = strength), color.copy(alpha = strength * .3f), Color.Transparent), center, radius), radius, center)
}

/** Abstract ambient lighting, drawn natively; no bitmap, WebView, blur layer or network asset. */
@Composable fun NightBackdrop(modifier: Modifier = Modifier) {
    if (!LocalDarkTheme.current) { DayBackdrop(modifier); return }
    val Background = Background
    val Teal = Teal
    val phase = ambientPhase(28000)
    val moving = LocalMotionEnabled.current
    Box(modifier.clipToBounds()) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF11112C), Background, Color(0xFF080E20))))
        }
        // Cache expensive gradients/paths as a layer; animate its transform, not every pixel's shader.
        Canvas(Modifier.matchParentSize().graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
            scaleX = 1.12f; scaleY = 1.08f
            translationX = sin(phase.value) * 13.dp.toPx()
            translationY = cos(phase.value) * 15.dp.toPx()
            alpha = .90f + .10f * sin(phase.value)
        }) {
        val t = .8f
        val w = size.width; val h = size.height
        glow(Color(0xFF873DFF), Offset(w * (.15f + .13f * sin(t)), h * (.14f + .06f * cos(t))), w * .96f, .28f)
        glow(Color(0xFF167B8C), Offset(w * (.94f + .13f * cos(t)), h * (.38f + .09f * sin(t))), w * .86f, .26f)
        glow(Color(0xFF6544D2), Offset(w * .38f, h * (.86f + .05f * cos(t))), w * .9f, .18f)

        // Layered translucent curtains have independent bends but a seamless common period.
        repeat(3) { layer ->
            val line = Path()
            val y = h * (.11f + layer * .22f)
            line.moveTo(-w * .22f, y + h * .13f)
            line.cubicTo(w * (.13f + .09f * sin(t + layer)), y - h * .17f,
                w * (.63f + .08f * cos(t + layer)), y + h * .19f, w * 1.2f, y - h * .16f)
            val tint = if (layer == 1) Color(0xFF59B6C8) else Color(0xFFAF86FF)
            val brush = Brush.horizontalGradient(listOf(Color.Transparent, tint.copy(alpha = .10f), tint.copy(alpha = .03f), Color.Transparent))
            drawPath(line, brush, style = Stroke((70 - layer * 10).dp.toPx(), cap = StrokeCap.Round))
            drawPath(line, brush, style = Stroke(22.dp.toPx(), cap = StrokeCap.Round))
            drawPath(line, tint.copy(alpha = .045f), style = Stroke(1.dp.toPx()))
        }
        }
        Canvas(Modifier.matchParentSize()) {
        val t = phase.value
        val w = size.width; val h = size.height
        repeat(46) { i ->
            val x = ((i * 73 + 17) % 101) / 100f * w + sin(t + i * .7f) * 3.dp.toPx()
            val y = ((i * 53 + 9) % 109) / 109f * h
            val pulse = ((sin(t * (1 + i % 2) + i * 1.83f) + 1f) * .5f).pow(3)
            val radius = (if (i % 9 == 0) 1.15f else .55f).dp.toPx()
            val color = if (i % 4 == 0) Teal else Color(0xFFE5DAFF)
            drawCircle(color.copy(alpha = .12f + pulse * .43f), radius, Offset(x, y))
            if (i % 9 == 0) {
                drawCircle(color.copy(alpha = .035f + pulse * .025f), 4.dp.toPx(), Offset(x, y))
                drawLine(color.copy(alpha = pulse * .3f), Offset(x - radius * 3, y), Offset(x + radius * 3, y), .5.dp.toPx())
                drawLine(color.copy(alpha = pulse * .3f), Offset(x, y - radius * 3), Offset(x, y + radius * 3), .5.dp.toPx())
            }
        }
        // One brief, quiet light trail each cycle; disabled entirely with reduced motion.
        val travel = ((t / (2 * PI).toFloat() - .16f) / .12f)
        if (moving && travel in 0f..1f) {
            val head = Offset(w * (1.05f - travel * .75f), h * (.08f + travel * .15f))
            val tail = head + Offset(w * .20f, -h * .04f)
            val alpha = sin(travel * PI).toFloat() * .38f
            drawLine(Brush.linearGradient(listOf(Color.White.copy(alpha = alpha), Color.Transparent), head, tail), head, tail, 1.dp.toPx(), StrokeCap.Round)
        }
    }
    }
}

@Composable private fun DayBackdrop(modifier: Modifier) {
    val phase = ambientPhase(32000)
    Box(modifier.clipToBounds()) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF0F8FE), Color(0xFFFAF7F3))))
        }
        Canvas(Modifier.matchParentSize().graphicsLayer {
            translationX = sin(phase.value) * 12.dp.toPx()
            translationY = cos(phase.value) * 10.dp.toPx()
            scaleX = 1.1f; scaleY = 1.1f
        }) {
            glow(Color(0xFFADD8F4), Offset(size.width * .04f, size.height * .3f), size.width * .9f, .24f)
            glow(Color(0xFFF4D5A5), Offset(size.width * .95f, size.height * .08f), size.width * .8f, .3f)
            glow(Color(0xFFBBE3F5), Offset(size.width * .96f, size.height * .75f), size.width * .7f, .16f)
            repeat(3) { i ->
                val r = size.width * (.5f + i * .14f)
                drawCircle(Color(0xFFCFB67F).copy(alpha = .07f), r,
                    Offset(size.width * 1.02f, size.height * .08f), style = Stroke(.7.dp.toPx()))
            }
        }
    }
}
