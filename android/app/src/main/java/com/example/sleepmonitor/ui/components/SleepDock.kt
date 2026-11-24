package com.example.sleepmonitor.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sleepmonitor.ui.theme.*

/** Keep the centre assistant independent of the four diary destinations. */
@Composable fun SleepDock(route: String, navigate: (String) -> Unit, assistant: () -> Unit) {
    val dark = LocalDarkTheme.current
    val accent = Purple
    val motion = LocalMotionEnabled.current
    val selected = when (route) { "sleep" -> 1; "history" -> 3; "stats" -> 4; else -> 0 }
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 14.dp).padding(top = 12.dp, bottom = 8.dp)) {
        LiquidSelectionTrack(selected, 5, Modifier.fillMaxWidth().padding(top = 10.dp).height(66.dp)) {
            DockTab("home", "Tổng quan", Icons.Outlined.Home, route, navigate, Modifier.weight(1f))
            DockTab("sleep", "Ghi ngủ", Icons.Outlined.Bedtime, route, navigate, Modifier.weight(1f))
            Spacer(Modifier.weight(1f))
            DockTab("history", "Lịch sử", Icons.Outlined.History, route, navigate, Modifier.weight(1f))
            DockTab("stats", "Thống kê", Icons.Outlined.BarChart, route, navigate, Modifier.weight(1f))
        }
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val scale = animateFloatAsState(if (pressed) .90f else 1f, if (motion) spring(.52f, 420f) else snap(), label = "assistant-press")
        Column(Modifier.align(Alignment.TopCenter), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(Modifier.size(54.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value }
                .shadow(6.dp, CircleShape, ambientColor = accent.copy(alpha = .2f), spotColor = accent.copy(alpha = .2f))
                .clip(CircleShape).background(Brush.linearGradient(if (dark) listOf(Color(0xFFAF8DE1), Color(0xFF6242A3), Color(0xFF354F78))
                    else listOf(Color(0xFF8FD8EE), Color(0xFF3C9BCB), Color(0xFF247FB8))))
                .border(1.5.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = .96f), Color(0xFFFFE4A7).copy(alpha = .7f), Color.White.copy(alpha = .3f))), CircleShape)
                .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = assistant)
                .semantics { contentDescription = "Mở Sleep AI" }, contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(26.dp), tint = Color.White)
            }
            Text("Sleep AI", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = accent)
        }
    }
}

@Composable private fun DockTab(id: String, label: String, icon: ImageVector, route: String, navigate: (String) -> Unit, modifier: Modifier) {
    val chosen = route == id
    val motion = LocalMotionEnabled.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale = animateFloatAsState(if (pressed) .88f else 1f, if (motion) spring(.6f, 520f) else snap(), label = "dock-press")
    val color by animateColorAsState(if (chosen) Purple else Muted, tween(if (motion) 180 else 0), label = "dock-color")
    Column(modifier.fillMaxHeight().selectable(chosen, source, indication = null, role = Role.Tab, onClick = { navigate(id) }),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, null, Modifier.size(21.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value }, tint = color)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 9.sp, color = color, fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
    }
}
