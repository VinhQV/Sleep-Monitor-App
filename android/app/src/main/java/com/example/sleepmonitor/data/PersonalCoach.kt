package com.example.sleepmonitor.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToLong

enum class CoachFocus(val label:String) {
    SCHEDULE("Giờ ngủ"), ROOM("Phòng ngủ"), REST("Cảm nhận")
}

enum class ReplyStyle(val wire:String, val label:String) {
    BRIEF("brief","Ngắn gọn"), STEPS("steps","Từng bước")
}

data class CoachTip(val observation:String,val nextStep:String,val route:String,val action:String)

/** The result is rendered on the device and never added to cloud chat history. */
fun personalTip(focus:CoachFocus, sessions:List<SleepSession>, room:Environment,
                settings:Settings, now:Long, zone:ZoneId=ZoneId.systemDefault()):CoachTip {
    if(focus==CoachFocus.ROOM)return roomTip(room,settings,now)
    fun local(ts:Long)=Instant.ofEpochMilli(ts).atZone(zone)
    val today=local(now).toLocalDate()
    val week=sessions.filter { session ->
        val end=session.end ?: return@filter false
        !session.deleted && end>session.start && end<=now &&
            !local(end).toLocalDate().isBefore(today.minusDays(6))
    }
    if(week.isEmpty())return CoachTip("Trong 7 ngày qua chưa có phiên đã kết thúc.",
        "Ghi lại giờ đi ngủ và giờ thức dậy của một phiên để bắt đầu.","sleep","Mở Ghi ngủ")
    val days=week.groupBy { local(it.end!!).toLocalDate() }
    if(focus==CoachFocus.REST) {
        val rated=week.filter { it.quality in 1..5 }
        if(rated.isEmpty())return CoachTip("Bạn có ${week.size} phiên đã ghi trong 7 ngày, chưa có đánh giá khi thức dậy.",
            "Mở một phiên trong Lịch sử và ghi lại cảm nhận của bạn.","history","Mở Lịch sử")
        val last=rated.maxBy { it.end!! }
        val date=local(last.end!!).toLocalDate()
        return CoachTip("Ngày ${date.dayOfMonth}/${date.monthValue}, bạn chọn “${qualities[last.quality]}”. Có ${rated.size}/${week.size} phiên đã đánh giá trong 7 ngày.",
            if(last.quality<=2)"Mở phiên đó để xem lại giờ ngủ và các lần thức giấc bạn đã ghi."
            else "Xem các lần đánh giá khác trong tuần để so sánh cảm nhận khi thức dậy.",
            if(last.quality<=2)"history" else "stats",if(last.quality<=2)"Mở Lịch sử" else "Mở Thống kê")
    }
    val mean=days.values.map { rows->rows.sumOf { it.durationMinutes } }.average().roundToLong()
    val overview="Trong 7 ngày qua, trung bình mỗi ngày có ghi nhận là ${durationText(mean)} (${days.size}/7 ngày)."
    if(days.size<2)return CoachTip(overview,"Ghi thêm một ngày để so sánh giờ bắt đầu giữa hai ngày.","sleep","Mở Ghi ngủ")
    // Use the longest recorded session per wake date so a short nap isn't chosen over a full night.
    fun mainSession(date:LocalDate)=days.getValue(date).maxBy { it.durationMinutes }
    val recent=days.keys.sortedDescending().take(2).map { local(mainSession(it).start) }
    val gap=abs(recent[0].toLocalTime().toSecondOfDay()/60-recent[1].toLocalTime().toSecondOfDay()/60)
    val difference=minOf(gap,1440-gap)
    val change=if(difference==0)"cùng giờ" else "lệch $difference phút"
    return CoachTip(overview,"Ở hai ngày có ghi nhận gần nhất, phiên dài nhất bắt đầu $change. Mở Lịch sử để xem hai mốc giờ.","history","Mở Lịch sử")
}

private fun roomTip(room:Environment,settings:Settings,now:Long):CoachTip {
    fun tip(text:String,next:String)=CoachTip(text,next,"env","Mở Môi trường")
    val sample=room.current ?: return tip("Chưa có số đo phòng.","Kiểm tra kết nối của thiết bị trong Môi trường.")
    if(now-sample.timestamp !in 0L..30000L)return tip("Chưa có số đo mới trong 30 giây qua.","Kiểm tra thời điểm cập nhật trước khi chỉnh phòng.")
    val light=sample.light
    val temp=sample.temperature
    val humidity=sample.humidity
    if(light!=null && light>settings.lightMax)return tip("Ánh sáng ${light.roundToLong()} lux, cao hơn ngưỡng bạn đặt (${settings.lightMax.roundToLong()} lux).",
        "Nếu ánh sáng này đến từ đèn đầu giường, thử giảm độ sáng rồi xem lại số đo.")
    if(temp!=null && temp !in settings.temperatureMin..settings.temperatureMax)return tip("Nhiệt độ ${"%.1f".format(temp)} °C nằm ngoài khoảng ${settings.temperatureMin}–${settings.temperatureMax} °C bạn đặt.",
        "Xem biểu đồ nhiệt độ 24 giờ để biết số đo vừa thay đổi hay đã kéo dài.")
    if(humidity!=null && humidity !in settings.humidityMin..settings.humidityMax)return tip("Độ ẩm ${humidity.roundToLong()}% nằm ngoài khoảng ${settings.humidityMin}–${settings.humidityMax}% bạn đặt.",
        "Xem biểu đồ độ ẩm 24 giờ và kiểm tra các lần đo kế tiếp.")
    if(light==null || temp==null || humidity==null)return tip("Thiết bị mới gửi một phần số đo.","Kiểm tra nhiệt độ, độ ẩm và ánh sáng trong Môi trường.")
    return tip("Ba số đo mới nhất đều trong ngưỡng phòng bạn đã đặt.","Mở biểu đồ 24 giờ để xem nhiệt độ, độ ẩm hoặc ánh sáng thay đổi lúc nào.")
}
