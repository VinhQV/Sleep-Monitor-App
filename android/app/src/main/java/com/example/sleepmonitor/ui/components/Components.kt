package com.example.sleepmonitor.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.example.sleepmonitor.data.*
import com.example.sleepmonitor.ui.theme.*
import java.util.Locale

@Composable fun Page(title: String, subtitle: String = "", content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Spacer(Modifier.height(2.dp))
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = Ink)
            if (subtitle.isNotEmpty()) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Muted)
        }
        content()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable fun Panel(modifier: Modifier = Modifier, color: Color = Card, content: @Composable ColumnScope.() -> Unit) {
    LiquidGlass(modifier.fillMaxWidth(),corner=12,tint=color){
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
@Composable fun SectionTitle(text: String) { Text(text, style = MaterialTheme.typography.titleMedium, color = Ink) }
@Composable fun SmallLabel(text: String) { Text(text, color = Muted, fontSize = 12.sp, lineHeight = 18.sp) }
@Composable fun Metric(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SmallLabel(label)
        Text(value, fontSize = 25.sp, fontWeight = FontWeight.SemiBold, color = Ink)
    }
}
@Composable fun StatusPill(text: String, color: Color = Teal, modifier: Modifier = Modifier) {
    Row(modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = .10f)).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(5.dp).background(color, CircleShape))
        Text(text, fontSize = 10.sp, color = color, fontWeight = FontWeight.Medium)
    }
}
@Composable fun IconTile(icon: ImageVector, color: Color = Purple, size: Int = 38) {
    Box(Modifier.size(size.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size((size * .52f).dp), tint = color)
    }
}
@Composable fun PrimaryAction(label: String, onClick: () -> Unit, enabled: Boolean = true, icon: ImageVector? = null) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .975f else 1f, tween(if (LocalMotionEnabled.current) 120 else 0), label = "button-press")
    val phase = ambientPhase(10000)
    val shape = RoundedCornerShape(10.dp)
    val dark = LocalDarkTheme.current
    Button(onClick, enabled = enabled, interactionSource = interaction, shape = shape,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White, disabledContainerColor = CardRaised),
        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if(enabled)12.dp else 0.dp,shape,ambientColor=Purple.copy(alpha=.24f),spotColor=Purple.copy(alpha=.24f))
            .background(if (enabled) Brush.horizontalGradient(if(dark)listOf(Color(0xFF8755DC),Color(0xFF6766D7),Color(0xFF4E7CA8))else listOf(Color(0xFF287FB4),Color(0xFF3D9CC6),Color(0xFF6CB8D0))) else Brush.linearGradient(listOf(CardRaised, CardRaised)), shape)
            .clip(shape).border(1.dp,Color.White.copy(alpha=if(enabled).20f else .04f),shape)
            .drawWithContent {
                drawContent()
                if(enabled){val x=(kotlin.math.sin(phase.value)+1f)*.5f*size.width
                    drawRect(Brush.linearGradient(listOf(Color.Transparent,Color.White.copy(alpha=.07f),Color.Transparent),Offset(x-size.width*.4f,0f),Offset(x+size.width*.2f,size.height)))}
            }) {
        if (icon != null) { Icon(icon, null, Modifier.size(19.dp)); Spacer(Modifier.width(9.dp)) }
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}
@Composable fun QualitySelector(selected: Int, onSelect: (Int) -> Unit) {
    val icons = listOf(Icons.Outlined.SentimentVeryDissatisfied, Icons.Outlined.SentimentDissatisfied,
        Icons.Outlined.SentimentNeutral, Icons.Outlined.SentimentSatisfied, Icons.Outlined.SentimentVerySatisfied)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (1..5).forEach { i ->
            val chosen = selected == i
            val scale by animateFloatAsState(if(chosen)1.04f else 1f,if(LocalMotionEnabled.current)spring(dampingRatio=.65f,stiffness=450f)else snap(),label="feeling-pop")
            val color by androidx.compose.animation.animateColorAsState(if (chosen) Purple else Muted, tween(if(LocalMotionEnabled.current)180 else 0), label = "quality")
            Column(Modifier.weight(1f).graphicsLayer{scaleX=scale;scaleY=scale}.clip(RoundedCornerShape(8.dp))
                .background(if (chosen) Lavender else Card)
                .border(1.dp, if (chosen) Purple.copy(alpha = .6f) else Border, RoundedCornerShape(8.dp))
                .selectable(selected=chosen,role=Role.RadioButton,onClick={onSelect(i)}).padding(vertical = 12.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icons[i - 1], null, Modifier.size(25.dp), tint = color)
                Text(qualities[i], color = color, fontSize = 10.sp, lineHeight = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center, minLines = 2, maxLines = 2)
            }
        }
    }
}

@Composable fun SleepSummaryCard(s: SleepSession?, label: String = "GIẤC NGỦ GẦN NHẤT") {
    AuroraPanel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
            Icon(Icons.Outlined.NightsStay, null, Modifier.size(17.dp), tint = Purple)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                if (s == null) {
                    Text("Một giấc ngủ\nđáng để chăm sóc", fontSize = 25.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold)
                    SmallLabel("Bắt đầu nhật ký của bạn hôm nay")
                } else {
                    Text(durationText(s.durationMinutes), fontSize = 36.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp)
                    StatusPill(qualities[s.quality], if (s.quality >= 4) Teal else Purple)
                    SmallLabel(dateText(s.end ?: s.start))
                }
            }
            MoonOrb(Modifier.size(100.dp), (s?.durationMinutes ?: 0) / 480f)
        }
        if (s?.end != null) {
            HorizontalDivider(color = Border)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TimeMetric(Icons.Outlined.Bedtime, "Đi ngủ", clockText(s.start))
                TimeMetric(Icons.Outlined.WbSunny, "Thức dậy", clockText(s.end), Amber)
            }
        }
    }
}
@Composable private fun TimeMetric(icon: ImageVector, label: String, value: String, color: Color = Purple) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(18.dp), tint = color)
        Column { SmallLabel(label); Text(value, fontSize = 19.sp, fontWeight = FontWeight.Medium) }
    }
}
@Composable fun SensorTile(label: String, value: Double?, unit: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(10.dp)).background(Brush.verticalGradient(listOf(color.copy(alpha=.13f),Card.copy(alpha=.86f))))
        .border(1.dp,Brush.linearGradient(listOf(color.copy(alpha=.28f),Border.copy(alpha=.35f))), RoundedCornerShape(10.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(19.dp), tint = color)
            Text(unit,color=Muted,fontSize=10.sp)
        }
        Text(if (value == null) "—" else String.format(Locale.US, if (unit == "lux" || unit == "ADC") "%.0f" else "%.1f", value), fontSize = 23.sp, fontWeight = FontWeight.SemiBold,maxLines=1)
        Text(label, color = Muted, fontSize = 11.sp, maxLines = 1)
    }
}
@Composable fun SensorCard(label: String, value: Double?, unit: String) {
    Panel { SmallLabel(label); Text(if (value == null) "—" else String.format(Locale.US, "%.1f %s", value, unit), fontSize = 28.sp, fontWeight = FontWeight.SemiBold) }
}
@Composable fun SleepHistoryItem(s: SleepSession, onClick: () -> Unit) {
    Panel(Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(Icons.Outlined.Bedtime, if (s.end == null) Amber else Purple)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(dateText(s.end ?: s.start), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                SmallLabel("${clockText(s.start)} → ${s.end?.let { clockText(it) } ?: "…"}")
            }
            Text(if (s.end == null) "Đang ngủ" else durationText(s.durationMinutes), color = Purple, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Icon(Icons.Outlined.ChevronRight, null, tint = Muted, modifier = Modifier.size(16.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(qualities[s.quality], fontSize = 11.sp, color = if (s.quality >= 4) Teal else Muted)
            if (s.dirty) Text("Chờ đồng bộ", fontSize = 11.sp, color = Amber)
        }
        if (s.notes.isNotBlank()) Text(s.notes, fontSize = 12.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable fun LineChart(samples: List<SensorSample>, value: (SensorSample) -> Double?, label: String) {
    val Border = Border
    val Purple = Purple
    val Teal = Teal
    val vals = samples.map(value)
    val gapLimit = ((samples.zipWithNext { a, b -> b.timestamp - a.timestamp }.filter { it > 0 }.minOrNull() ?: 60000) * 3).coerceAtLeast(120000)
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val reveal by animateFloatAsState(if (entered) 1f else 0f, tween(if (LocalMotionEnabled.current) 700 else 0), label = "line-reveal")
    Panel {
        SectionTitle(label)
        if (vals.count { it != null } < 2) SmallLabel("Chưa đủ dữ liệu để vẽ biểu đồ") else {
            val valid = vals.filterNotNull()
            SmallLabel("${String.format(Locale.US, "%.1f", valid.min())} — ${String.format(Locale.US, "%.1f", valid.max())}")
            Canvas(Modifier.fillMaxWidth().height(120.dp).semantics { contentDescription = "$label, ${valid.size} bản ghi" }) {
                repeat(3) { i -> val y = size.height * i / 2; drawLine(Border.copy(alpha = .6f), Offset(0f, y), Offset(size.width, y), 1f) }
                val min = valid.min(); val span = (valid.max() - min).coerceAtLeast(1.0)
                val path = Path(); var pen = false
                vals.forEachIndexed { i, v ->
                    if (v == null) pen = false else {
                        val x = (samples[i].timestamp - samples.first().timestamp).toFloat() / (samples.last().timestamp - samples.first().timestamp).coerceAtLeast(1) * size.width
                        val y = size.height - ((v - min) / span).toFloat() * (size.height - 20) - 10
                        if (x <= size.width * reveal) {
                            if (pen && (i == 0 || samples[i].timestamp - samples[i - 1].timestamp <= gapLimit)) path.lineTo(x, y) else path.moveTo(x, y)
                        }
                        pen = true
                    }
                }
                drawPath(path, Purple.copy(alpha = .1f), style = Stroke(10.dp.toPx(), cap = StrokeCap.Round))
                drawPath(path, Brush.horizontalGradient(listOf(Purple, Teal)), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SmallLabel(clockText(samples.first().timestamp)); SmallLabel(clockText(samples.last().timestamp))
            }
        }
    }
}
@Composable fun BarChart(values: List<Pair<String, Long?>>) {
    val Border = Border
    val Purple = Purple
    val Teal = Teal
    var selected by remember(values) { mutableIntStateOf((values.size - 1).coerceAtLeast(0)) }
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val rise by animateFloatAsState(if (entered) 1f else 0f, tween(if (LocalMotionEnabled.current) 800 else 0, easing = FastOutSlowInEasing), label = "bar-rise")
    Panel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SectionTitle("Nhịp ngủ của bạn")
            Text("Giờ ngủ / ngày", color = Muted, fontSize = 11.sp)
        }
        if (values.isEmpty()) SmallLabel("Chưa có phiên ngủ trong kỳ này") else {
            SmallLabel("${values[selected].first} · ${values[selected].second?.let{durationText(it)} ?: "Chưa ghi nhận"}")
            Canvas(Modifier.fillMaxWidth().height(150.dp).pointerInput(values) { detectTapGestures { selected = (it.x / size.width * values.size).toInt().coerceIn(values.indices) } }
                .semantics { contentDescription = values.joinToString { "${it.first}: ${it.second?.let{m->durationText(m)} ?: "Chưa ghi nhận"}" }
                    customActions=values.mapIndexed { index,v -> CustomAccessibilityAction("Xem ngày ${v.first}"){selected=index;true} } }) {
                val max = values.maxOf { it.second ?: 0L }.coerceAtLeast(600).toFloat(); val w = size.width / values.size
                repeat(4) { i -> val y = size.height * i / 3; drawLine(Border.copy(alpha = .6f), Offset(0f, y), Offset(size.width, y), 1f) }
                values.forEachIndexed { i, v ->
                    val h = ((v.second ?: 0L) / max * size.height * rise).coerceAtLeast(3.dp.toPx())
                    drawRoundRect(Brush.verticalGradient(if(v.second==null)listOf(Border,Border) else if (i == selected) listOf(Teal, Teal.copy(alpha=.45f)) else listOf(Purple, Purple.copy(alpha=.35f))),
                        Offset(i * w + w * .22f, size.height - h), Size(w * .56f, h), CornerRadius(5.dp.toPx(), 5.dp.toPx()))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { SmallLabel(values.first().first); SmallLabel(values.last().first) }
        }
    }
}

