package com.example.sleepmonitor.data

import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

data class SleepSession(val id:String, val start:Long, val end:Long?=null, val quality:Int=0,
    val notes:String="", val interrupted:Boolean=false, val version:Int=0, val deleted:Boolean=false, val dirty:Boolean=false) {
    val durationMinutes:Long get()=((end?:System.currentTimeMillis())-start).coerceAtLeast(0)/60000
    val date:LocalDate get()=Instant.ofEpochMilli(end?:start).atZone(ZoneId.systemDefault()).toLocalDate()
}
data class SensorSample(val id:String,val timestamp:Long,val temperature:Double?,val humidity:Double?,val light:Double?,val source:String,
    val lightAdc:Double?=null,val lightMillivolts:Double?=null) {
    val lightReading:Double? get()=lightAdc?:light
    val lightUnit:String get()=if(lightAdc!=null)"ADC" else "lux"
}
data class SleepEvent(val id:String,val timestamp:Long,val kind:String,val value:String,val source:String)
data class DeviceStatus(val online:Boolean=false,val lastSeen:Long=0,val lampOnline:Boolean=false,val brightness:Int=0,val source:String="device",val timeSynced:Boolean=false,val queue:Int=0,val sensorOnly:Boolean=false)
data class Environment(val current:SensorSample?=null,val samples:List<SensorSample> = emptyList(),val events:List<SleepEvent> = emptyList(),val status:DeviceStatus=DeviceStatus())
data class Settings(val temperatureMin:Double=18.0,val temperatureMax:Double=27.0,val humidityMin:Double=40.0,val humidityMax:Double=70.0,val lightMax:Double=150.0,
    val alarmEnabled:Boolean=false,val alarmMinute:Int=390,val fadeMinutes:Int=15,val autoLight:Boolean=true,val manualBrightness:Int=30,val timezoneOffset:Int=420)
data class Connection(val url:String="http://10.0.2.2:8080",val key:String="sleep-local-demo",val demo:Boolean=false)
val qualities=listOf("Chưa đánh giá","Rất kém","Kém","Trung bình","Tốt","Rất tốt")
fun durationText(minutes:Long)="${minutes/60}h ${minutes%60}m"
fun clockText(timestamp:Long)=Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
fun dateText(timestamp:Long)=Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
fun averageMinutes(rows:List<SleepSession>):Long=rows.filter{it.end!=null&&!it.deleted}.map{it.durationMinutes}.let{if(it.isEmpty())0 else it.average().roundToInt().toLong()}
fun environmentStatus(s:SensorSample?,settings:Settings,now:Long=System.currentTimeMillis()):String {
    if(s==null)return "Chưa có dữ liệu"
    if(now-s.timestamp>30000)return "Dữ liệu đã cũ"
    if(s.temperature==null||s.humidity==null||s.lightReading==null)return "Thiếu số đo cảm biến"
    if(s.temperature !in settings.temperatureMin..settings.temperatureMax || s.humidity !in settings.humidityMin..settings.humidityMax)return "Ngoài ngưỡng đã đặt"
    return if(s.lightAdc!=null)"Đang nhận dữ liệu cảm biến" else if(s.light!!<=settings.lightMax)"Trong ngưỡng đã đặt" else "Ngoài ngưỡng đã đặt"
}
fun completed(rows:List<SleepSession>)=rows.filter{!it.deleted&&it.end!=null}
fun periodStart(month:Boolean,today:LocalDate=LocalDate.now()):LocalDate=if(month)today.withDayOfMonth(1)else today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
fun periodEnd(month:Boolean,anchor:LocalDate):LocalDate=if(month)anchor.withDayOfMonth(anchor.lengthOfMonth())else periodStart(false,anchor).plusDays(6)
fun periodRows(rows:List<SleepSession>,month:Boolean,today:LocalDate=LocalDate.now()):List<SleepSession>{
    val from=periodStart(month,today)
    return completed(rows).filter{!it.date.isBefore(from)&&!it.date.isAfter(today)}
}
fun periodTotals(rows:List<SleepSession>,month:Boolean,today:LocalDate=LocalDate.now()):List<Pair<LocalDate,Long?>> {
    val totals=dailyMinutes(periodRows(rows,month,today));val from=periodStart(month,today)
    return (0..java.time.temporal.ChronoUnit.DAYS.between(from,today)).map{from.plusDays(it).let{day->day to totals[day]}}
}
fun dailyMinutes(rows:List<SleepSession>)=completed(rows).groupBy{it.date}.mapValues{(_,v)->v.sumOf{it.durationMinutes}}
fun dailyAverage(rows:List<SleepSession>):Long=dailyMinutes(rows).values.let{if(it.isEmpty())0 else it.average().roundToInt().toLong()}
fun averageMetric(samples:List<SensorSample>,value:(SensorSample)->Double?):String=samples.mapNotNull(value).let{if(it.isEmpty())"—" else String.format(java.util.Locale.US,"%.1f",it.average())}

