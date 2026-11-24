package com.example.sleepmonitor.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sleepmonitor.ui.theme.*
import kotlin.math.abs

/** One small backdrop buffer per toolbar; never capture the whole screen recursively. */
@Composable fun LiquidToolbarSurface(
    modifier: Modifier = Modifier,
    corner: Int = 32,
    content: @Composable BoxScope.() -> Unit
) {
    val dark = LocalDarkTheme.current
    val accent = Purple
    val scene = LocalGlassScene.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    val shape = RoundedCornerShape(corner.dp)
    Box(modifier.shadow(7.dp, shape, ambientColor = Color(0xFF264A68).copy(alpha = .09f),
        spotColor = Color(0xFF163350).copy(alpha = .12f))
        .onGloballyPositioned { origin = it.positionInRoot() }.clip(shape)) {
        if (scene != null) Canvas(Modifier.matchParentSize().graphicsLayer {
            if (Build.VERSION.SDK_INT >= 31) renderEffect = BlurEffect(12.dp.toPx(), 12.dp.toPx(), TileMode.Clamp)
        }) {
            translate(scene.origin.x - origin.x, scene.origin.y - origin.y) { drawLayer(scene.layer) }
        }
        Canvas(Modifier.matchParentSize()) {
            drawRect(Brush.linearGradient(if (dark) listOf(Color(0xFF25253F).copy(alpha = .88f),
                Color(0xFF17253A).copy(alpha = .82f)) else listOf(Color.White.copy(alpha = .90f),
                Color(0xFFE8F5FF).copy(alpha = .70f), Color(0xFFFFFAEF).copy(alpha = .82f))))
            val inset = .7.dp.toPx()
            drawRoundRect(Brush.linearGradient(listOf(Color.White.copy(alpha = if (dark) .35f else .98f),
                accent.copy(alpha = .12f), Color.White.copy(alpha = if (dark) .12f else .8f))),
                Offset(inset, inset), Size(size.width - inset * 2, size.height - inset * 2),
                CornerRadius(corner.dp.toPx()), style = Stroke(1.dp.toPx()))
        }
        content()
    }
}

/** Fixed semantic/touch slots; only the optical lens moves. Rapid taps retarget the same spring. */
@Composable internal fun LiquidSelectionTrack(
    selectedIndex: Int,
    slots: Int,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val motion = LocalMotionEnabled.current
    val dark = LocalDarkTheme.current
    val accent = Purple
    val position = remember { Animatable(selectedIndex.toFloat()) }
    LaunchedEffect(selectedIndex, motion) {
        if (motion) position.animateTo(selectedIndex.toFloat(), spring(dampingRatio = .72f, stiffness = 380f))
        else position.snapTo(selectedIndex.toFloat())
    }
    val inkLayer = rememberGraphicsLayer()
    LiquidToolbarSurface(modifier) {
        Box(Modifier.fillMaxSize().padding(5.dp)) {
            fun DrawScope.lensBounds(): Rect {
                val slot = size.width / slots
                val stretch = if (motion) (abs(position.velocity) * .014f).coerceAtMost(.24f) else 0f
                val width = (slot - 2.dp.toPx()) * (1f + stretch)
                val height = size.height * (1f - stretch * .17f)
                val center = ((position.value + .5f) * slot).coerceIn(width / 2, size.width - width / 2)
                return Rect(center - width / 2, (size.height - height) / 2, center + width / 2, (size.height + height) / 2)
            }
            Canvas(Modifier.matchParentSize()) {
                val r = lensBounds()
                val radius = CornerRadius(r.height / 2)
                drawRoundRect(Color.Black.copy(alpha = if (dark) .14f else .035f),
                    r.topLeft + Offset(0f, 1.5.dp.toPx()), r.size, radius)
                drawRoundRect(Brush.linearGradient(if (dark) listOf(Color(0xFF65608B).copy(alpha = .66f),
                    Color(0xFF343F62).copy(alpha = .76f), Color(0xFF554B77).copy(alpha = .60f)) else listOf(
                    Color.White.copy(alpha = .98f), Color(0xFFD2EBFA).copy(alpha = .88f), Color(0xFFFFF6DC).copy(alpha = .78f)),
                    r.topLeft, r.bottomRight), r.topLeft, r.size, radius)
                drawRoundRect(Brush.linearGradient(listOf(Color.White.copy(alpha = .95f), accent.copy(alpha = .24f),
                    Color(0xFFA0E8FF).copy(alpha = .6f), Color.White.copy(alpha = .70f)), r.topLeft, r.bottomRight),
                    r.topLeft, r.size, radius, style = Stroke(1.dp.toPx()))
                // Fine opposing reflections move with the lens, rather than an idle shimmer loop.
                val reflection = Rect(r.left + 2.dp.toPx(), r.top + 1.dp.toPx(), r.right - 2.dp.toPx(), r.bottom - 1.dp.toPx())
                drawArc(Color.White.copy(alpha = if (dark) .45f else .9f), 205f, 115f, false,
                    reflection.topLeft, reflection.size, style = Stroke(.8.dp.toPx(), cap = StrokeCap.Round))
            }
            Row(Modifier.fillMaxSize().selectableGroup().drawWithContent {
                inkLayer.record { this@drawWithContent.drawContent() }
                val r = lensBounds()
                val lens = Path().apply { addRoundRect(RoundRect(r, CornerRadius(r.height / 2))) }
                // Refract the real icons/labels locally. One semantics tree, no duplicated controls.
                clipPath(lens, ClipOp.Difference) { drawLayer(inkLayer) }
                clipPath(lens) { scale(1.045f, 1.045f, r.center) { drawLayer(inkLayer) } }
            }, verticalAlignment = Alignment.CenterVertically, content = content)
        }
    }
}

@Composable fun LiquidSegmentedControl(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    LiquidSelectionTrack(selected, labels.size, modifier.fillMaxWidth().height(58.dp)) {
        labels.forEachIndexed { index, label ->
            val source = remember { MutableInteractionSource() }
            val pressed by source.collectIsPressedAsState()
            val motion = LocalMotionEnabled.current
            val scale = animateFloatAsState(if (pressed) .94f else 1f, if (motion) spring(stiffness = 500f) else snap(), label = "segment-press")
            val color by animateColorAsState(if (selected == index) Purple else Muted, tween(if (motion) 180 else 0), label = "segment-color")
            Box(Modifier.weight(1f).fillMaxHeight()
                .selectable(selected == index, source, indication = null, role = Role.Tab) { onSelect(index) }, contentAlignment = Alignment.Center) {
                Text(label, Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value },
                    color = color, fontSize = 12.sp, fontWeight = if (selected == index) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
            }
        }
    }
}

@Composable fun LiquidIconButton(icon: ImageVector, description: String, onClick: () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true, prominent: Boolean = false) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val motion = LocalMotionEnabled.current
    val scale = animateFloatAsState(if (pressed) .88f else 1f, if (motion) spring(.55f, 500f) else snap(), label = "tool-press")
    val accent = Purple
    val dark = LocalDarkTheme.current
    Box(modifier.size(48.dp).clip(CircleShape)
        .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Box(Modifier.size(40.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .background(if (prominent) accent.copy(alpha = if (enabled) 1f else .18f) else accent.copy(alpha = if (pressed) .14f else .045f), CircleShape)
            .border(.7.dp, Color.White.copy(alpha = if (dark) .12f else .78f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, description, Modifier.size(21.dp), tint = if (!enabled) Muted.copy(alpha = .4f) else if (prominent) Color.White else accent)
        }
    }
}
