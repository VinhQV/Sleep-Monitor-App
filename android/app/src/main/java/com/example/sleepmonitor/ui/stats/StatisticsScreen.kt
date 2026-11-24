package com.example.sleepmonitor.ui.stats

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sleepmonitor.data.*
import com.example.sleepmonitor.ui.components.*
import com.example.sleepmonitor.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable fun StatisticsScreen(sessions:List<SleepSession>){
    var month by rememberSaveable{mutableStateOf(false)}
    var anchorText by rememberSaveable{mutableStateOf(LocalDate.now().toString())}
    val anchor=LocalDate.parse(anchorText);val end=minOf(periodEnd(month,anchor),LocalDate.now())
    val rows=periodRows(sessions,month,end);val days=dailyMinutes(rows).toSortedMap();val average=dailyAverage(rows)
    Page("Thống kê giấc ngủ"){
        LiquidToolbarSurface(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(horizontal=4.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
            TextButton({anchorText=(if(month)anchor.minusMonths(1)else anchor.minusWeeks(1)).toString()}){Text("‹ Trước")}
            Text(if(month)anchor.format(DateTimeFormatter.ofPattern("MM/yyyy"))else periodStart(false,anchor).format(DateTimeFormatter.ofPattern("dd/MM"))+" – "+periodEnd(false,anchor).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),fontSize=12.sp)
            TextButton({anchorText=(if(month)anchor.plusMonths(1)else anchor.plusWeeks(1)).toString()},enabled=periodStart(month,anchor)<periodStart(month,LocalDate.now())){Text("Sau ›")}
        }}
        if(periodStart(month,anchor)!=periodStart(month,LocalDate.now()))TextButton({anchorText=LocalDate.now().toString()}){Text("Về hiện tại")}

        LiquidSegmentedControl(listOf("Theo tuần","Theo tháng"),if(month)1 else 0,{month=it==1})
        Reveal{Panel(color=Lavender){
            SmallLabel("Trung bình / ngày có ghi nhận")
            Row(verticalAlignment=Alignment.CenterVertically){
                Text(if(rows.isEmpty())"—" else durationText(average),modifier=Modifier.weight(1f),fontSize=34.sp,fontWeight=FontWeight.SemiBold)
                MoonOrb(Modifier.size(87.dp),average/480f)
            }
            HorizontalDivider(color=Border)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){SmallLabel("${days.size} ngày ghi nhận");SmallLabel("${rows.size} phiên ngủ")}
        }}
        Reveal(100){BarChart(periodTotals(sessions,month,end).map{it.first.format(DateTimeFormatter.ofPattern("dd/MM")) to it.second})}
        Panel{
            SectionTitle("Cảm nhận khi thức dậy")
            for(q in 5 downTo 1){
                val count=rows.count{it.quality==q}
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                    Text(qualities[q],fontSize=12.sp,color=Muted,modifier=Modifier.width(70.dp))
                    LinearProgressIndicator(progress={if(rows.isEmpty())0f else count.toFloat()/rows.size},modifier=Modifier.weight(1f).height(5.dp),color=if(q>=4)Teal else Purple,trackColor=Border)
                    Text("$count",fontSize=12.sp,modifier=Modifier.width(17.dp))
                }
            }
            SmallLabel("${rows.count{it.quality==0}} phiên chưa đánh giá")
        }
        Panel{SectionTitle("Về số liệu của bạn")
            SmallLabel(if(rows.isEmpty())"Ghi phiên ngủ đầu tiên để bắt đầu thấy nhịp sinh hoạt của bạn." else "Biểu đồ cộng các phiên theo ngày thức dậy. Trung bình chỉ tính những ngày có ghi nhận, bỏ qua ngày thiếu dữ liệu.")
            SmallLabel("Thời lượng được tính từ giờ đi ngủ đến giờ thức dậy trong nhật ký.")
        }
    }
}

