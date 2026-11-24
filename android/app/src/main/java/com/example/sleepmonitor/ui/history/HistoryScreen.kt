package com.example.sleepmonitor.ui.history
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.example.sleepmonitor.data.*
import com.example.sleepmonitor.ui.components.*

@Composable fun HistoryScreen(sessions:List<SleepSession>,detail:(String)->Unit,add:()->Unit){
    var query by remember{mutableStateOf("")};var quality by remember{mutableIntStateOf(0)};var filters by remember{mutableStateOf(false)}
    val rows=sessions.filter{!it.deleted && (quality==0||it.quality==quality) && (query.isBlank()||dateText(it.end?:it.start).contains(query)||it.notes.contains(query,true))}.sortedByDescending{it.end?:it.start}
    Page("Những đêm đã qua","Nhật ký theo ngày thức dậy"){
        LiquidToolbarSurface(Modifier.fillMaxWidth()){
            Row(Modifier.fillMaxWidth().heightIn(min=52.dp).padding(horizontal=8.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                TextButton({filters=!filters}){Text(if(quality==0)"Lọc cảm nhận" else qualities[quality])}
                TextButton(add){Text("+ Ghi giấc ngủ")}
            }
        }
        OutlinedTextField(query,{query=it},label={Text("Tìm ngày hoặc ghi chú")},modifier=Modifier.fillMaxWidth(),singleLine=true)
        AnimatedVisibility(filters){QualitySelector(quality){quality=if(quality==it)0 else it}}
        if(rows.isEmpty())Panel{SectionTitle(if(query.isBlank()&&quality==0)"Trang đầu của nhật ký" else "Chưa tìm thấy giấc ngủ");SmallLabel(if(query.isBlank()&&quality==0)"Ghi lại giờ ngủ và cảm nhận để bắt đầu." else "Thử một ngày hoặc ghi chú khác.")}
        rows.groupBy{it.date}.forEach{(date,day)->
            Row(Modifier.fillMaxWidth().padding(top=10.dp),horizontalArrangement=Arrangement.SpaceBetween){SmallLabel(dateText((day.first().end?:day.first().start)));SmallLabel(durationText(day.filter{it.end!=null}.sumOf{it.durationMinutes}))}
            day.forEach{s->key(s.id){Reveal{SleepHistoryItem(s){detail(s.id)}}}}
        }
    }
}
