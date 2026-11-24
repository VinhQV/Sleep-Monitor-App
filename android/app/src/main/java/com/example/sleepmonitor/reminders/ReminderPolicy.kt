package com.example.sleepmonitor.reminders
import com.example.sleepmonitor.data.*
import java.time.*
import kotlin.math.roundToInt

data class ReminderOptions(val enabled:Boolean=false,val bedtime:Int=22*60+30,val morning:Int=7*60+30,val adaptive:Boolean=true)
data class ReminderMessage(val title:String,val body:String,val destination:String)
fun suggestedBedtime(rows:List<SleepSession>,fallback:Int,now:Long=System.currentTimeMillis()):Int {
    val recent=completed(rows).filter{it.start>=now-14*86400000L&&it.durationMinutes>=180}.take(14)
    if(recent.size<3)return fallback
    // Unwrap midnight before finding the median (23:55 and 00:05 are close).
    val minutes=recent.map{Instant.ofEpochMilli(it.start).atZone(ZoneId.systemDefault()).let{z->(z.hour*60+z.minute).let{if(it<720)it+1440 else it}}}.sorted()
    val median=minutes[minutes.size/2];val spread=minutes.last()-minutes.first()
    return if(spread>180)fallback else ((median-15+1440)%1440/5.0).roundToInt()*5%1440
}
fun reminderMessage(kind:String,rows:List<SleepSession>,now:Long):ReminderMessage? {
    val active=rows.firstOrNull{!it.deleted&&it.end==null}
    if(kind=="bed"){
        if(active!=null)return null
        val recentlyFinished=completed(rows).any{now-it.end!! in 0..2*3600000L}
        return if(recentlyFinished)null else ReminderMessage("Một chút thời gian cho giấc ngủ","Nếu bạn chuẩn bị nghỉ, hãy mở nhật ký và bắt đầu ghi giấc ngủ.","home")
    }
    if(active!=null)return null // Never wake someone merely because a clock says morning.
    val unrated=completed(rows).filter{it.quality==0&&now-it.end!! in 0..24*3600000L}.maxByOrNull{it.end!!}
    return if(unrated!=null)ReminderMessage("Đêm qua bạn ngủ thế nào?","Khi thuận tiện, ghi lại cảm nhận để hiểu nhịp ngủ của mình hơn.","rate:${unrated.id}")
    else if(completed(rows).none{now-it.end!! in 0..20*3600000L})ReminderMessage("Thêm một dòng vào nhật ký","Nếu tối qua quên bấm ghi ngủ, bạn có thể bổ sung giờ ngủ và thức dậy.","sleep") else null
}
fun nextReminderTime(minute:Int,now:ZonedDateTime):ZonedDateTime {
    val target=now.toLocalDate().atTime(minute/60,minute%60).atZone(now.zone)
    return if(target.isAfter(now))target else now.toLocalDate().plusDays(1).atTime(minute/60,minute%60).atZone(now.zone)
}
