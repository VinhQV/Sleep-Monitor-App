package com.example.sleepmonitor.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import com.example.sleepmonitor.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.*

@Composable fun Reveal(delayMillis: Int = 0, content: @Composable () -> Unit) {
    val enabled = LocalMotionEnabled.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(enabled) { if (enabled && delayMillis > 0) delay(delayMillis.toLong()); visible = true }
    val fraction = animateFloatAsState(if (visible) 1f else 0f,
        tween(if (enabled) 650 else 0, easing = FastOutSlowInEasing), label = "soft-arrival")
    Box(Modifier.graphicsLayer {
        alpha = fraction.value
        translationY = (1 - fraction.value) * 32.dp.toPx()
        scaleX = .97f + .03f * fraction.value; scaleY = scaleX
    }) { content() }
}

/** Duration ring is stationary and data-driven; only the decorative satellite orbits rotate. */
@Composable fun MoonOrb(modifier: Modifier = Modifier, progress: Float = .8f, ai: Boolean = false) {
    if (!LocalDarkTheme.current && !ai) { SunOrb(modifier, progress); return }
    val Purple = Purple
    val Teal = Teal
    val phase = ambientPhase(14000)
    val enabled = LocalMotionEnabled.current
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val arc = animateFloatAsState(if (appeared) progress.coerceIn(0f, 1f) else 0f,
        tween(if (enabled) 1400 else 0, easing = FastOutSlowInEasing), label = "duration-ring")
    val glyph = remember(ai) { Path().apply {
        if (ai) {
            moveTo(0f, -.66f)
            quadraticTo(.14f, -.14f, .66f, 0f); quadraticTo(.14f, .14f, 0f, .66f)
            quadraticTo(-.14f, .14f, -.66f, 0f); quadraticTo(-.14f, -.14f, 0f, -.66f)
        } else {
            moveTo(.22f, -.66f)
            cubicTo(-.76f, -.79f, -.93f, .53f, -.13f, .69f)
            cubicTo(.30f, .80f, .71f, .51f, .74f, .13f)
            cubicTo(.02f, .42f, -.24f, -.26f, .22f, -.66f)
        }
        close()
    } }
    Canvas(modifier) {
        val t = phase.value; val r = size.minDimension * .32f
        val breathe = (sin(t * 2f) + 1f) * .5f
        val c = center + Offset(0f, sin(t) * 2.dp.toPx())
        glow(Purple, c, r * (1.65f + breathe * .08f), .28f + breathe * .09f)
        glow(Teal, c + Offset(r * .55f, r * .35f), r * 1.1f, .12f)
        drawCircle(Purple.copy(alpha = .09f), r * 1.23f, center, style = Stroke(6.dp.toPx()))
        drawCircle(Purple.copy(alpha = .18f), r * 1.23f, center, style = Stroke(1.dp.toPx()))
        val ring = Brush.sweepGradient(listOf(Purple, Color(0xFFF4DDFF), Teal, Purple), center)
        drawArc(ring, -90f, arc.value * 360f, false,
            center - Offset(r * 1.23f, r * 1.23f), Size(r * 2.46f, r * 2.46f), style = Stroke(2.3.dp.toPx(), cap = StrokeCap.Round))
        withTransform({ rotate(-24f, center) }) {
            drawOval(Brush.linearGradient(listOf(Teal.copy(alpha = .4f), Purple.copy(alpha = .04f), Purple.copy(alpha = .32f))),
                center - Offset(r * 1.47f, r * .66f), Size(r * 2.94f, r * 1.32f), style = Stroke(.65.dp.toPx()))
        }
        repeat(3) { i ->
            val a = t + i * (2 * PI / 3).toFloat()
            val position = center + Offset(cos(a) * r * 1.36f, sin(a) * r * 1.36f)
            val color = if (i == 1) Teal else Purple
            glow(color, position, 5.dp.toPx(), .45f)
            drawCircle(color.copy(alpha = .9f), if (i == 0) 1.6.dp.toPx() else 1.dp.toPx(), position)
        }
        withTransform({ translate(c.x, c.y); scale(r, r, Offset.Zero); rotate(sin(t) * if (ai) 8f else 3f, Offset.Zero) }) {
            drawPath(glyph, Brush.linearGradient(listOf(Color.White, Color(0xFFD9C9FF), Color(0xFF8C86EC)), Offset(-.6f, -.7f), Offset(.7f, .7f)))
            drawPath(glyph, Color.White.copy(alpha = .24f), style = Stroke(.018f))
        }
        val star = c + Offset(r * .60f, -r * .58f)
        glow(Teal, star, r * .26f, .22f)
        drawLine(Teal, star - Offset(3.dp.toPx(), 0f), star + Offset(3.dp.toPx(), 0f), .9.dp.toPx(), StrokeCap.Round)
        drawLine(Teal, star - Offset(0f, 3.dp.toPx()), star + Offset(0f, 3.dp.toPx()), .9.dp.toPx(), StrokeCap.Round)
    }
}

@Composable fun LampHalo(modifier: Modifier = Modifier, brightness: Float) {
    val Amber = Amber
    val light = animateFloatAsState(brightness.coerceIn(0f, 1f), tween(if (LocalMotionEnabled.current) 600 else 0), label = "lamp-preview")
    val phase = ambientPhase(9000)
    Canvas(modifier) {
        val r = size.minDimension * .18f
        val strength = .08f + light.value * .42f
        val c = center
        glow(Amber, c, size.minDimension * (.42f + sin(phase.value) * .02f), strength)
        drawCircle(Amber.copy(alpha = .08f), r * 1.9f, c, style = Stroke(.7.dp.toPx()))
        drawCircle(Amber.copy(alpha = .16f + light.value * .18f), r * 1.4f, c, style = Stroke(1.dp.toPx()))
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF2D8), Amber.copy(alpha = .55f)), c, r), r, c)
    }
}

@Composable fun TypingDots() {
    val Purple = Purple
    val phase = ambientPhase(1800)
    Canvas(Modifier.size(46.dp, 22.dp)) {
        repeat(3) { i ->
            val wave = (sin(phase.value - i * .8f) + 1f) * .5f
            drawCircle(Purple.copy(alpha = .35f + .65f * wave), 3.dp.toPx(), Offset((8 + i * 14).dp.toPx(), center.y - wave * 3.dp.toPx()))
        }
    }
}

@Composable private fun SunOrb(modifier: Modifier, progress: Float) {
    val phase = ambientPhase(18000)
    val accent = Purple
    val moving = LocalMotionEnabled.current
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val arc = animateFloatAsState(if (entered) progress.coerceIn(0f, 1f) else 0f,
        tween(if (moving) 1400 else 0, easing = FastOutSlowInEasing), label = "sun-duration")
    Canvas(modifier) {
        val r = size.minDimension * .23f
        val t = phase.value
        val c = center + Offset(0f, sin(t) * 2.dp.toPx())
        glow(Color(0xFFE7B969), c, r * 2.2f, .25f + .05f * sin(t))
        drawCircle(accent.copy(alpha = .10f), r * 1.68f, center, style = Stroke(1.5.dp.toPx()))
        drawArc(Brush.sweepGradient(listOf(accent, Color(0xFFC49B57), accent), center), -90f, arc.value * 360f, false,
            center - Offset(r * 1.68f, r * 1.68f), Size(r * 3.36f, r * 3.36f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        repeat(12) { i ->
            val a = i * PI.toFloat() / 6f + t * .12f
            val direction = Offset(cos(a), sin(a))
            drawLine(Color(0xFFD3A561).copy(alpha = .5f), c + direction * r * 1.2f,
                c + direction * r * (1.38f + .05f * sin(t + i)), 1.5.dp.toPx(), StrokeCap.Round)
        }
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFFBE8), Color(0xFFF1D4A3), Color(0xFFDDAE71)), c - Offset(r * .3f, r * .3f), r * 1.6f), r, c)
        drawCircle(Color.White.copy(alpha = .7f), r, c, style = Stroke(.7.dp.toPx()))
        val satellite = center + Offset(cos(t) * r * 1.68f, sin(t) * r * 1.68f)
        drawCircle(accent.copy(alpha = .65f), 2.dp.toPx(), satellite)
    }
}
