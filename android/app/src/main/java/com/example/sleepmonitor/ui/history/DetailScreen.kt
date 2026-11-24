package com.example.sleepmonitor.ui.history
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.sleepmonitor.data.*
import com.example.sleepmonitor.ui.components.*
import com.example.sleepmonitor.viewmodel.AppState

@Composable fun DetailScreen(id:String,state:AppState,edit:()->Unit,delete:()->Unit){
    val s=state.sessions.find{it.id==id&&!it.deleted};var confirm by remember{mutableStateOf(false)}
    val env=if(state.detailId==id)state.detail else Environment()
    Page("Chi tiết giấc ngủ",s?.let{dateText(it.end?:it.start)}?:""){
        if(s==null)Text("Phiên không còn tồn tại") else{
            if(s.end!=null)SleepSummaryCard(s,"PHIÊN ĐANG XEM")
            else StatusPill("Đang ngủ",MaterialTheme.colorScheme.primary)
            Panel{SectionTitle("Nhật ký");Text(s.notes.ifBlank{"Chưa có ghi chú"});SmallLabel(if(s.interrupted)"Có thức giấc giữa đêm" else "Không ghi nhận thức giấc giữa đêm")
                SmallLabel("Bắt đầu ${clockText(s.start)} ${dateText(s.start)}");s.end?.let{SmallLabel("Kết thúc ${clockText(it)} ${dateText(it)}")}}
            if(env.samples.isNotEmpty()){Panel{SectionTitle("Môi trường trong phiên")
                val adc=env.samples.any{it.lightAdc!=null}
                Text("${averageMetric(env.samples){it.temperature}} °C  ·  ${averageMetric(env.samples){it.humidity}} %  ·  ${averageMetric(env.samples){if(adc)it.lightAdc else it.light}} ${if(adc)"ADC" else "lux"}")
                SmallLabel("Trung bình ${env.samples.size} bản ghi trong khoảng thời gian này")
                if(env.samples.isEmpty())SmallLabel("Chưa có dữ liệu cảm biến cho phiên này.")}
            LineChart(env.samples,{it.temperature},"Diễn biến nhiệt độ")
            val adc=env.samples.any{it.lightAdc!=null}
            LineChart(env.samples,{if(adc)it.lightAdc else it.light},"Ánh sáng · ${if(adc)"ADC" else "lux"}")
            };if(env.events.isNotEmpty())Panel{SectionTitle("Dòng thời gian thiết bị")
                if(env.events.isEmpty())SmallLabel("Chưa có sự kiện được ghi nhận")
                env.events.takeLast(30).forEach{e->Text("${clockText(e.timestamp)} · ${eventLabel(e.kind)}");SmallLabel(e.value)}}
            if(s.end!=null)PrimaryAction("Sửa phiên ngủ",edit)
            TextButton({confirm=true}){Text("Xóa phiên ngủ")}
        }
    }
    if(confirm)AlertDialog(onDismissRequest={confirm=false},title={Text("Xóa phiên ngủ?")},text={Text("Phiên này sẽ được xóa khỏi nhật ký và đồng bộ khi có kết nối.")},confirmButton={TextButton({confirm=false;delete()}){Text("Xóa")}},dismissButton={TextButton({confirm=false}){Text("Hủy")}})
}
fun eventLabel(kind:String)=when(kind){"sleep_start"->"Bắt đầu ngủ bằng nút nhấn";"sleep_end"->"Thức dậy bằng nút nhấn";"light_change"->"Đèn đổi độ sáng";"environment_outside"->"Môi trường ngoài ngưỡng";"sensor_error"->"Kiểm tra cảm biến";"queue_full"->"Bộ nhớ đệm gần đầy";else->kind}

