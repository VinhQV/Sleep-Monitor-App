package com.example.sleepmonitor.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sleepmonitor.data.*
import com.example.sleepmonitor.viewmodel.AppState

@Composable
fun PersonalCoachDialog(app:AppState, focus:CoachFocus, style:ReplyStyle, useTopic:Boolean,
                        chooseFocus:(CoachFocus)->Unit, chooseStyle:(ReplyStyle)->Unit, setUseTopic:(Boolean)->Unit,
                        navigate:(String)->Unit, close:()->Unit){
    // The gateway may publish a fresh sample after the refresh loop captured app.now.
    val tip=personalTip(focus,app.sessions,app.environment,app.settings,System.currentTimeMillis())
    val source=when{
        app.connection.demo->"Dữ liệu trải nghiệm"
        focus!=CoachFocus.ROOM->"Nhật ký trên điện thoại"
        app.environment.current?.source=="simulator"->"Số đo giả lập"
        app.environment.current?.source=="device"->"Thiết bị trong phòng"
        else->"Chưa xác nhận nguồn đo"
    }
    AlertDialog(onDismissRequest=close,title={Text("Gợi ý cho tôi")},text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text("Bạn muốn theo dõi điều gì?",style=MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){
                CoachFocus.entries.forEach { goal->FilterChip(focus==goal,{chooseFocus(goal)},{Text(goal.label)}) }
            }
            Text(source,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
            Text(tip.observation)
            Text(tip.nextStep)
            HorizontalDivider()
            Text("Cách chatbot trả lời",style=MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                ReplyStyle.entries.forEach { option->FilterChip(style==option,{chooseStyle(option)},{Text(option.label)}) }
            }
            Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
                Text("Gửi chủ đề “${focus.label}” khi chat",Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium)
                Switch(checked=useTopic,onCheckedChange=setUseTopic)
            }
            Text("Gợi ý trên được tính trên điện thoại. Chat nhận kiểu trả lời và tên chủ đề nếu bạn bật.",style=MaterialTheme.typography.bodySmall)
        }
    },confirmButton={TextButton({close();navigate(tip.route)}){Text(tip.action)}},
        dismissButton={TextButton(close){Text("Đóng")}})
}
