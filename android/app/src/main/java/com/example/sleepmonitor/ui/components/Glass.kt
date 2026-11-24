package com.example.sleepmonitor.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.sleepmonitor.ui.theme.*
import kotlin.math.*

@Composable fun AuroraPanel(modifier: Modifier = Modifier, accent: Color = Purple, content: @Composable ColumnScope.() -> Unit) {
    val dark = LocalDarkTheme.current
    val Teal = Teal
    val Card = Card
    val phase = ambientPhase(21000)
    LiquidGlass(modifier.fillMaxWidth(),corner=14,tint=Card) {
        Canvas(Modifier.matchParentSize().graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
            scaleX = 1.06f; scaleY = 1.06f
            translationX = sin(phase.value) * 4.dp.toPx()
            alpha = .9f + .1f * sin(phase.value)
        }) {
            val t = phase.value
            glow(accent, Offset(size.width * (.87f + .1f * sin(t)), size.height * .2f), size.width * .58f, if (dark) .17f else .06f)
            glow(Teal, Offset(size.width * (.5f + .18f * cos(t)), size.height * 1.1f), size.width * .6f, if (dark) .12f else .035f)
            val ribbon = Path().apply {
                moveTo(size.width * .28f, -size.height * .15f)
                cubicTo(size.width * .74f, size.height * (.12f + .07f * sin(t)), size.width * .53f, size.height * .6f, size.width * 1.1f, size.height * .83f)
            }
            drawPath(ribbon, Brush.verticalGradient(listOf(accent.copy(alpha = .03f), Teal.copy(alpha = .13f), Color.Transparent)), style = Stroke(if(dark)32.dp.toPx() else 9.dp.toPx()))
            drawPath(ribbon, Color.White.copy(alpha = .055f), style = Stroke(.75.dp.toPx()))
            repeat(9) { i ->
                val x = size.width * (.53f + ((i * 29) % 43) / 100f)
                val y = size.height * (((i * 37 + 11) % 97) / 100f)
                drawCircle(Color.White.copy(alpha = .10f + .12f * ((sin(t + i) + 1) * .5f)), .7.dp.toPx(), Offset(x, y))
            }
        }
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}


