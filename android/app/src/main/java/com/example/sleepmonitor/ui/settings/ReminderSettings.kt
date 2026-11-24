package com.example.sleepmonitor.ui.settings
import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.example.sleepmonitor.reminders.*
import com.example.sleepmonitor.ui.components.*
import com.example.sleepmonitor.data.SleepSession

@Composable fun ReminderSettings(rows:List<SleepSession>){
    val c=LocalContext.current;var o by remember{mutableStateOf(SleepReminders.options(c))}
    var permissionNote by remember{mutableStateOf("")}
    fun save(v:ReminderOptions){o=v;SleepReminders.save(c,v)}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->save(o.copy(enabled=granted));permissionNote=if(granted)"Đã bật nhắc nhở" else "Bạn có thể cho phép thông báo trong Cài đặt điện thoại."}
    Panel{
        SectionTitle("Nhắc nhở giấc ngủ")
        Row(verticalAlignment=Alignment.CenterVertically){Text("Nhắc tôi ghi nhật ký",Modifier.weight(1f));Switch(o.enabled,{enabled->if(enabled&&Build.VERSION.SDK_INT>=33)permission.launch(Manifest.permission.POST_NOTIFICATIONS)else save(o.copy(enabled=enabled))})}
        SmallLabel("Nhắc nhẹ, không phát âm thanh. Khi đang ghi ngủ, app tạm ngưng nhắc để bạn nghỉ ngơi.")
        if(o.enabled){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                TextButton({TimePickerDialog(c,{_,h,m->save(o.copy(bedtime=h*60+m))},o.bedtime/60,o.bedtime%60,true).show()}){Text("Giờ ngủ %02d:%02d".format(o.bedtime/60,o.bedtime%60))}
                TextButton({TimePickerDialog(c,{_,h,m->save(o.copy(morning=h*60+m))},o.morning/60,o.morning%60,true).show()}){Text("Buổi sáng %02d:%02d".format(o.morning/60,o.morning%60))}
            }
            Row(verticalAlignment=Alignment.CenterVertically){Text("Theo thói quen gần đây",Modifier.weight(1f));Switch(o.adaptive,{save(o.copy(adaptive=it))})}
            val minute=if(o.adaptive)suggestedBedtime(rows,o.bedtime)else o.bedtime
            SmallLabel("Nhắc tối khoảng %02d:%02d. Sau ít nhất 3 đêm có nhịp ổn định, giờ nhắc có thể sớm hơn giờ ngủ thường lệ 15 phút. Điện thoại có thể giao thông báo trễ khi tiết kiệm pin.".format(minute/60,minute%60))
            TextButton({SleepReminders.notify(c,"bed",true)}){Text("Thử thông báo")}
        }
        if(permissionNote.isNotEmpty())SmallLabel(permissionNote)
        TextButton({c.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,c.packageName))}){Text("Quyền thông báo trên điện thoại")}
    }
}
@Composable fun DeviceSetupSettings(){
    val c=LocalContext.current
    Panel{
        SectionTitle("Thiết lập Wi-Fi thiết bị")
        SmallLabel("1. Bật XIAO. Giữ nút BOOT 3 giây khi board đã chạy để mở chế độ thiết lập.")
        SmallLabel("2. Kết nối Wi-Fi Sleep-Setup-XXXXXX. Mật khẩu: Sleep-XXXXXX (6 ký tự cuối đúng như tên mạng, chữ hoa).")
        SmallLabel("3. Mở trang thiết lập, chọn Wi-Fi 2.4 GHz và nhập mật khẩu. Thiết bị kiểm tra kết nối trước khi lưu.")
        OutlinedButton({c.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))},Modifier.fillMaxWidth()){Text("Mở cài đặt Wi-Fi")}
        PrimaryAction("Mở trang thiết lập",{c.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("http://192.168.4.1")))})
        SmallLabel("Khi đi xa, đổi sang Wi-Fi 2.4 GHz hoặc điểm phát điện thoại. Giữ địa chỉ HTTPS và khóa thiết bị để tiếp tục gửi số đo về máy tính ở nhà. Máy tính cần bật và có Internet.")
    }
}
