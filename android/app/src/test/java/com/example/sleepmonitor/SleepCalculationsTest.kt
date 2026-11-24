package com.example.sleepmonitor
import com.example.sleepmonitor.data.*
import java.time.*
import org.junit.Assert.*
import org.junit.Test
class SleepCalculationsTest {
    private fun at(date:String,time:String)=LocalDateTime.parse(date+"T"+time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    @Test fun overnightUsesWakeDateAndActualElapsedTime(){
        val s=SleepSession("night",at("2026-11-18","23:15"),at("2026-11-19","06:45"))
        assertEquals(450,s.durationMinutes);assertEquals(LocalDate.parse("2026-11-19"),s.date)
    }
    @Test fun multipleSessionsSumPerDayAndExcludeMissingDays(){
        val a=SleepSession("night",at("2026-11-18","23:00"),at("2026-11-19","06:00"))
        val b=SleepSession("nap",at("2026-11-19","13:00"),at("2026-11-19","14:00"))
        assertEquals(480,dailyAverage(listOf(a,b)))
        assertEquals(1,dailyMinutes(listOf(a,b)).size)
    }
    @Test fun deletedAndActiveSessionsDoNotAffectStatistics(){
        val start=System.currentTimeMillis()-3600000
        val rows=listOf(SleepSession("active",start),SleepSession("deleted",start,start+60000,deleted=true))
        assertTrue(completed(rows).isEmpty());assertEquals(0,dailyAverage(completed(rows)))
    }
    @Test fun staleOrMissingSensorValuesCannotBeMarkedComfortable(){
        val now=System.currentTimeMillis();val sample=SensorSample("one",now,25.0,60.0,20.0,"device")
        assertEquals("Trong ngưỡng đã đặt",environmentStatus(sample,Settings(),now))
        assertEquals("Dữ liệu đã cũ",environmentStatus(sample.copy(timestamp=now-31000),Settings(),now))
        assertEquals("Thiếu số đo cảm biến",environmentStatus(sample.copy(light=null),Settings(),now))
    }
}
