package com.example.sleepmonitor.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.sleepmonitor.data.*
import com.example.sleepmonitor.viewmodel.AppState
import com.example.sleepmonitor.ui.components.*
import com.example.sleepmonitor.ui.theme.*
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable fun HomeScreen(state: AppState, start: () -> Unit, wake: () -> Unit, environment: () -> Unit,
                          review: (String) -> Unit = {}, history: () -> Unit = {}, statistics: () -> Unit = {}) {
    val rows = state.sessions.filter { !it.deleted }
    val active = rows.firstOrNull { it.end == null }
    val latest = completed(rows).maxByOrNull { it.end!! }
    val week = periodRows(rows, false)
    val dark = LocalDarkTheme.current;val motion=LocalMotionEnabled.current
    var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(active?.id) { while(active != null) { clock=System.currentTimeMillis();delay(1000) } }
    val day = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM", Locale.forLanguageTag("vi-VN")))
    Page(if(dark)"Một đêm an yên" else "Chào ngày mới",day.replaceFirstChar{it.titlecase()}) {
        Reveal {
            AnimatedContent(targetState=active!=null,transitionSpec={fadeIn(tween(if(motion)300 else 0)) togetherWith fadeOut(tween(if(motion)180 else 0))},label="sleep-session-state") { sleeping ->
                if(sleeping && active!=null) AuroraPanel {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                        StatusPill("ĐANG NGHỈ NGƠI",Purple)
                        Icon(Icons.Outlined.Bedtime,null,tint=Purple,modifier=Modifier.size(18.dp))
                    }
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(9.dp)){
                            Text(durationText(((clock-active.start)/60000).coerceAtLeast(0)),fontSize=36.sp,fontWeight=FontWeight.SemiBold,letterSpacing=(-1).sp)
                            SmallLabel("Bắt đầu lúc ${clockText(active.start)}")
                            SmallLabel("Mọi thứ có thể chờ đến sáng.")
                        }
                        MoonOrb(Modifier.size(100.dp),((clock-active.start)/60000f/480f).coerceIn(0f,1f))
                    }
                } else SleepSummaryCard(latest)
            }
        }
        Reveal(90) {
            Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
                PrimaryAction(if(active==null)"BẮT ĐẦU NGỦ" else "THỨC DẬY",if(active==null)start else wake,
                    icon=if(active==null)Icons.Outlined.Bedtime else Icons.Outlined.WbSunny)
                if(active==null && latest?.quality==0)TextButton({review(latest.id)},modifier=Modifier.fillMaxWidth()){
                    Icon(Icons.Outlined.SentimentSatisfied,null,Modifier.size(17.dp));Spacer(Modifier.width(7.dp));Text("Đánh giá giấc ngủ vừa rồi")
                }
            }
        }
        Reveal(170){
            Panel(Modifier.clickable(onClick=statistics)){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                    SectionTitle("Nhịp ngủ tuần này")
                    Icon(Icons.Outlined.NorthEast,null,tint=Muted,modifier=Modifier.size(17.dp))
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(24.dp)){
                    Column(Modifier.weight(1f)){SmallLabel("Trung bình / ngày");Text(if(week.isEmpty())"—" else durationText(dailyAverage(week)),fontSize=24.sp,fontWeight=FontWeight.SemiBold,color=Purple)}
                    Column{SmallLabel("Đã ghi nhận");Text("${dailyMinutes(week).size} ngày",fontSize=24.sp,fontWeight=FontWeight.SemiBold,color=Ink)}
                }
                WeekStrip(week)
            }
        }
        Reveal(240){
            val connected=state.connection.demo||(state.serverReachable&&state.environment.status.online)
            val sample=state.environment.current.takeIf{connected}
            Panel(Modifier.clickable(onClick=environment)){
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                    IconTile(Icons.Outlined.Sensors,Teal,42)
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)){
                        SectionTitle("Phòng ngủ của bạn")
                        SmallLabel(if(sample!=null)"${sample.temperature?.let{String.format(Locale.US,"%.1f °C",it)}?:"—"}  ·  ${sample.humidity?.let{String.format(Locale.US,"%.0f%%",it)}?:"—"}"
                            else "${if(connected)"Đang chờ số đo" else "Thiết bị chưa kết nối"}")
                    }
                    Icon(Icons.Outlined.ChevronRight,"Xem phòng",Modifier.size(20.dp),tint=Muted)
                }
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)){
                    Box(Modifier.size(6.dp).clip(RoundedCornerShape(6.dp)).background(if(connected)Teal else Muted))
                    SmallLabel(if(connected)"Theo dõi nhiệt độ, độ ẩm & ánh sáng" else "Xem trạng thái thiết bị và dữ liệu gần nhất")
                }
            }
        }
    }
}

@Composable private fun WeekStrip(rows:List<SleepSession>){
    val today=LocalDate.now();val start=periodStart(false,today);val totals=dailyMinutes(rows)
    var entered by remember{mutableStateOf(false)}
    LaunchedEffect(Unit){entered=true}
    val fraction by animateFloatAsState(if(entered)1f else 0f,tween(if(LocalMotionEnabled.current)1000 else 0),label="week-bars")
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
        repeat(7){i->val date=start.plusDays(i.toLong());val minutes=totals[date]
            Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(7.dp)){
                Box(Modifier.fillMaxWidth().height(34.dp).clip(RoundedCornerShape(7.dp)).background(CardRaised),contentAlignment=Alignment.BottomCenter){
                    if(minutes!=null)Box(Modifier.fillMaxWidth().height((5+29*(minutes/600f).coerceIn(0f,1f)*fraction).dp).background(if(date==today)Teal.copy(alpha=.8f)else Purple.copy(alpha=.6f)))
                }
                Text(if(i==6)"CN" else "T${i+2}",fontSize=10.sp,color=if(date==today)Purple else Muted,fontWeight=if(date==today)FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}


