package com.example.sleepmonitor.ui.environment

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sleepmonitor.data.*
import com.example.sleepmonitor.ui.components.*
import com.example.sleepmonitor.ui.theme.*
import com.example.sleepmonitor.viewmodel.AppState

@Composable fun EnvironmentScreen(state:AppState,lamp:(Int)->Unit){
    val env=state.environment
    val connected=state.connection.demo||(state.serverReachable&&env.status.online)
    val sample=env.current.takeIf{connected}
    val lightUnit=env.current?.lightUnit?:"ADC"
    var brightness by rememberSaveable{mutableFloatStateOf(env.status.brightness.toFloat())}
    var chart by rememberSaveable{mutableIntStateOf(0)}
    val status=if(!connected)"Mất kết nối thiết bị" else environmentStatus(sample,state.settings,state.now)
    Page("Phòng ngủ của bạn"){
        Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
            StatusPill(status,if(status=="Trong ngưỡng đã đặt")Teal else Amber)
            env.current?.let{SmallLabel("${if(connected)"Cập nhật" else "Số đo cuối"} ${clockText(it.timestamp)} · ${dateText(it.timestamp)}")}
        }
        Row(horizontalArrangement=Arrangement.spacedBy(9.dp)){
            SensorTile("Nhiệt độ",sample?.temperature,"°C",Icons.Outlined.Thermostat,Amber,Modifier.weight(1f))
            SensorTile("Độ ẩm",sample?.humidity,"%",Icons.Outlined.WaterDrop,Teal,Modifier.weight(1f))
            SensorTile("Ánh sáng",sample?.lightReading,lightUnit,Icons.Outlined.LightMode,Purple,Modifier.weight(1f))
        }
        sample?.lightMillivolts?.let{SmallLabel("AO ${it.toInt()} mV · ADC 12-bit, thang 0–4095")}
        if(env.status.lampOnline)Surface(shape=RoundedCornerShape(12.dp),color=Card,border=BorderStroke(1.dp,Amber.copy(alpha=.18f))){
            Column(Modifier.background(Brush.linearGradient(listOf(Amber.copy(alpha=.06f),Card))).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){
                    LampHalo(Modifier.size(52.dp),brightness/100f)
                    Column(Modifier.weight(1f).padding(start=8.dp)){
                        SectionTitle("Đèn đầu giường")
                        if(!state.connection.demo)SmallLabel(if(env.status.lampOnline)"Đã phản hồi · ${env.status.brightness}%" else "Chưa nhận phản hồi")
                    }
                    Text("${brightness.toInt()}%",color=Amber,fontWeight=FontWeight.SemiBold,fontSize=25.sp)
                }
                Slider(value=brightness,onValueChange={brightness=it},valueRange=0f..100f,colors=SliderDefaults.colors(thumbColor=Amber,activeTrackColor=Amber,inactiveTrackColor=Border))
                PrimaryAction("Đặt độ sáng",{lamp(brightness.toInt())},state.connection.demo||env.status.online,Icons.Outlined.Tune)
                if(state.command.isNotEmpty())Text(state.command,color=Teal,fontSize=12.sp)
                Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.WbTwilight,null,Modifier.size(18.dp),tint=Amber);Text(if(state.settings.alarmEnabled)"Bình minh lúc %02d:%02d".format(state.settings.alarmMinute/60,state.settings.alarmMinute%60) else "Đặt lịch đèn bình minh trong Cài đặt",fontSize=11.sp,color=Muted,modifier=Modifier.padding(start=8.dp))}
            }
        }
        Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
            SectionTitle("Nhịp thay đổi trong 24 giờ")
            LiquidSegmentedControl(listOf("Nhiệt độ","Độ ẩm","Ánh sáng"),chart,{chart=it})
        }
        key(chart,lightUnit){LineChart(env.samples,when(chart){0->{s->s.temperature};1->{s->s.humidity};else->{s->if(lightUnit=="ADC")s.lightAdc else s.light}},listOf("Nhiệt độ · °C","Độ ẩm · %","Ánh sáng · $lightUnit")[chart])}
        if(!state.connection.demo)Panel{
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){SmallLabel("Kết nối thiết bị");Text(if(connected)"Trực tuyến" else "Mất kết nối",color=if(connected)Teal else Amber,fontSize=12.sp)}
            SmallLabel("${env.status.queue} bản ghi chờ gửi · ${if(env.status.timeSynced)"Đồng hồ đã đồng bộ" else "Chưa xác nhận đồng hồ"}")
        }
    }
}

