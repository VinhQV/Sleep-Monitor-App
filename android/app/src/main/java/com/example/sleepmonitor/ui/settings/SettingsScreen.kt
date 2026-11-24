package com.example.sleepmonitor.ui.settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.sleepmonitor.data.*
import com.example.sleepmonitor.ui.components.*
import com.example.sleepmonitor.viewmodel.AppState

@Composable fun SettingsScreen(state:AppState,connect:(Connection)->Unit,save:(Settings)->Unit,reload:()->Unit,motion:(Boolean)->Unit,appearance:(AppearanceMode)->Unit,environment:()->Unit,chat:()->Unit,personalizeAi:(Boolean)->Unit){
    var url by remember(state.connection){mutableStateOf(state.connection.url)};var key by remember(state.connection){mutableStateOf(state.connection.key)};var demo by remember(state.connection){mutableStateOf(state.connection.demo)}
    var pairing by remember{mutableStateOf("")};var pairingError by remember{mutableStateOf("")}
    var advanced by remember{mutableStateOf(false)};var limits by remember{mutableStateOf(false)};var confirm by remember{mutableStateOf(false)}
    var thresholds by remember(state.settings){mutableStateOf(listOf(state.settings.temperatureMin,state.settings.temperatureMax,state.settings.humidityMin,state.settings.humidityMax,state.settings.lightMax).map{it.toString()})}
    val parsed=thresholds.map{it.toDoubleOrNull()};val valid=parsed.all{it!=null&&it.isFinite()} && (parsed[0]?:0.0)<(parsed[1]?:0.0) && (parsed[2]?:0.0)<(parsed[3]?:0.0)
    Page("Dành riêng cho bạn"){
        Reveal{Panel{
            SectionTitle("Giao diện")
            LiquidSegmentedControl(AppearanceMode.entries.map{it.label},AppearanceMode.entries.indexOf(state.appearanceMode),{appearance(AppearanceMode.entries[it])})
            SmallLabel("Tự động: giao diện sáng từ 06:00 đến 18:00.")
            Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){Text("Hiệu ứng chuyển động",Modifier.weight(1f));Switch(state.motionEnabled,motion)}
        }}
        Panel{
            SectionTitle("Kết nối từ xa")
            SmallLabel(if(state.connection.url.startsWith("https://"))"${java.net.URI(state.connection.url).host} · ${if(state.serverReachable)"Đã kết nối" else "Đang kết nối"}" else "Dùng app và thiết bị ở bất kỳ mạng Internet nào khi máy chủ đang bật.")
            OutlinedTextField(pairing,{pairing=it;pairingError=""},label={Text("Dán mã kết nối điện thoại")},visualTransformation=PasswordVisualTransformation(),singleLine=true,modifier=Modifier.fillMaxWidth(),isError=pairingError.isNotEmpty())
            if(pairingError.isNotEmpty())SmallLabel(pairingError)
            OutlinedButton({runCatching{decodeRemoteConnection(pairing)}.onSuccess{connect(it);pairing=""}.onFailure{pairingError="Mã chưa đúng. Sao chép lại mã điện thoại từ máy chủ."}},enabled=pairing.isNotBlank(),modifier=Modifier.fillMaxWidth()){Text("Kết nối")}
        }
        ReminderSettings(state.sessions)
        DeviceSetupSettings()
        Panel{SectionTitle("Không gian ngủ");SmallLabel("Nhiệt độ, độ ẩm và ánh sáng từ thiết bị của bạn.");OutlinedButton(environment,Modifier.fillMaxWidth()){Text("Xem phòng ngủ")}}
        Panel{
            SectionTitle("Trợ lý giấc ngủ")
            Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){Text("Cá nhân hóa từ nhật ký",Modifier.weight(1f));Switch(state.aiPersonalization,personalizeAi)}
            SmallLabel("Dùng tóm tắt giấc ngủ 7 ngày và số đo phòng để Gemini gợi ý phù hợp với bạn. Không gửi ghi chú riêng.")
            TextButton(chat){Text("Mở Sleep AI")}
        }
        Panel{
            TextButton({advanced=!advanced}){Text(if(advanced)"Ẩn kết nối nâng cao" else "Kết nối thiết bị & máy chủ")}
            AnimatedVisibility(advanced){Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
                SmallLabel(if(state.serverReachable)"Máy chủ đã kết nối" else "Chưa kết nối · Nhật ký vẫn lưu trên máy")
                OutlinedTextField(url,{url=it},label={Text("Địa chỉ máy chủ")},singleLine=true,modifier=Modifier.fillMaxWidth())
                OutlinedTextField(key,{key=it},label={Text("Khóa API")},singleLine=true,visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
                Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){Checkbox(demo,{demo=it});Text("Dùng dữ liệu trải nghiệm")}
                PrimaryAction("Lưu kết nối",{connect(Connection(url.trim().trimEnd('/'),key,demo))})
                if(!state.connection.demo)TextButton({confirm=true}){Text("Tải lại dữ liệu máy chủ")}
            }}
            TextButton({limits=!limits}){Text("Ngưỡng cảnh báo môi trường")}
            AnimatedVisibility(limits){Column{
                listOf("Nhiệt độ thấp (°C)","Nhiệt độ cao (°C)","Độ ẩm thấp (%)","Độ ẩm cao (%)","Ánh sáng tối đa (lux)").forEachIndexed{i,label->
                    if(i<4||state.environment.current?.lightAdc==null)OutlinedTextField(thresholds[i],{v->thresholds=thresholds.toMutableList().apply{set(i,v)}},label={Text(label)},singleLine=true,modifier=Modifier.fillMaxWidth(),isError=parsed[i]==null)
                }
                PrimaryAction("Lưu ngưỡng",{save(state.settings.copy(temperatureMin=parsed[0]!!,temperatureMax=parsed[1]!!,humidityMin=parsed[2]!!,humidityMax=parsed[3]!!,lightMax=parsed[4]!!))},valid)
            }}
        }
    }
    if(confirm)AlertDialog(onDismissRequest={confirm=false},title={Text("Thay dữ liệu cục bộ?")},text={Text("Các phiên chưa đồng bộ sẽ được thay thế bằng dữ liệu trên máy chủ.")},confirmButton={TextButton({confirm=false;reload()}){Text("Tải lại")}},dismissButton={TextButton({confirm=false}){Text("Hủy")}})
}

