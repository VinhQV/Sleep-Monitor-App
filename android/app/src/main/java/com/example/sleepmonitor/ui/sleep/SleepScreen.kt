package com.example.sleepmonitor.ui.sleep
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sleepmonitor.data.*
import com.example.sleepmonitor.ui.components.*
import com.example.sleepmonitor.ui.theme.*
import java.time.*

@Composable fun DateTimeField(label:String,value:Long,onChange:(Long)->Unit){
    val context=LocalContext.current;val t=Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault())
    val style=if(LocalDarkTheme.current)android.R.style.Theme_Material_Dialog else android.R.style.Theme_Material_Light_Dialog
    Panel{SmallLabel(label)
        TextButton(onClick={TimePickerDialog(context,style,{_,h,m->onChange(t.withHour(h).withMinute(m).withSecond(0).withNano(0).toInstant().toEpochMilli())},t.hour,t.minute,true).show()}){Text(clockText(value),fontSize=28.sp)}
        TextButton(onClick={DatePickerDialog(context,style,{_,y,m,d->onChange(LocalDate.of(y,m+1,d).atTime(t.toLocalTime()).atZone(t.zone).toInstant().toEpochMilli())},t.year,t.monthValue-1,t.dayOfMonth).show()}){Text(dateText(value),fontSize=12.sp)}
    }
}
@Composable private fun Feelings(notes:String,change:(String)->Unit){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){
        listOf("Ngủ ngon","Có ngáy","Khó vào giấc").forEach{label->
            val selected=notes.lines().contains(label)
            FilterChip(selected,{change(if(selected)notes.lines().filter{it!=label}.joinToString("\n") else listOf(notes.trim(),label).filter{it.isNotBlank()}.joinToString("\n"))},label={Text(label,fontSize=11.sp)})
        }
    }
}
@Composable fun SleepScreen(existing:SleepSession?=null,save:(String?,Long,Long,Int,String,Boolean)->Unit){
    var start by rememberSaveable(existing?.id){mutableLongStateOf(existing?.start?:System.currentTimeMillis()-8*3600000)}
    var end by rememberSaveable(existing?.id){mutableLongStateOf(existing?.end?:System.currentTimeMillis())}
    var quality by rememberSaveable(existing?.id){mutableIntStateOf(existing?.quality?:0)}
    var notes by rememberSaveable(existing?.id){mutableStateOf(existing?.notes?:"")}
    var interrupted by rememberSaveable(existing?.id){mutableStateOf(existing?.interrupted?:false)}
    val error=when{end<=start->"Giờ thức dậy cần sau giờ đi ngủ.";end-start>48*3600000L->"Phiên ngủ tối đa 48 giờ.";end>System.currentTimeMillis()+60000->"Giờ thức dậy không thể ở tương lai.";else->null}
    Page(if(existing==null)"Giấc ngủ của bạn" else "Chỉnh sửa giấc ngủ","Ghi lại một đêm, hiểu mình thêm một chút."){
        Reveal{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
            Box(Modifier.weight(1f)){DateTimeField("ĐI NGỦ",start){start=it}}
            Box(Modifier.weight(1f)){DateTimeField("THỨC DẬY",end){end=it}}
        }}
        Reveal(90){Panel(color=Lavender){Metric("Tổng thời gian ngủ",if(error==null)durationText((end-start)/60000)else "—");if(error!=null)Text(error,color=Rose)}}
        Reveal(150){Panel{
            SectionTitle("Bạn ngủ thế nào?");QualitySelector(quality){quality=it}
            Feelings(notes){notes=it}
            Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){Checkbox(interrupted,{interrupted=it});Text("Có thức giấc giữa đêm",fontSize=14.sp)}
            OutlinedTextField(notes,{notes=it.take(2000)},label={Text("Ghi chú thêm")},minLines=2,modifier=Modifier.fillMaxWidth())
        }}
        PrimaryAction("LƯU GIẤC NGỦ",{save(existing?.id,start,end,quality,notes,interrupted)},error==null)
    }
}
@Composable fun RatingDialog(s:SleepSession,close:()->Unit,save:(Int,String,Boolean)->Unit){
    var q by rememberSaveable(s.id){mutableIntStateOf(s.quality)};var notes by rememberSaveable(s.id){mutableStateOf(s.notes)};var interrupted by rememberSaveable(s.id){mutableStateOf(s.interrupted)}
    AlertDialog(onDismissRequest=close,title={Text("Bạn ngủ thế nào?")},text={Column(Modifier.verticalScroll(rememberScrollState())){
        Text(durationText(s.durationMinutes));QualitySelector(q){q=it};Feelings(notes){notes=it}
        OutlinedTextField(notes,{notes=it.take(2000)},label={Text("Ghi chú")});Row{Checkbox(interrupted,{interrupted=it});Text("Ngủ gián đoạn",Modifier.padding(top=12.dp))}
    }},confirmButton={TextButton(onClick={save(q,notes,interrupted)},enabled=q>0){Text("Lưu đánh giá")}},dismissButton={TextButton(onClick=close){Text("Để sau")}})
}
