package com.example.sleepmonitor

import com.example.sleepmonitor.data.*
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.*
import org.junit.Test

class PersonalCoachTest {
    private fun ts(day:String)=Instant.parse("2026-11-${day}:00Z").toEpochMilli()
    private val now=ts("20T12:00")
    private fun session(start:String,end:String)=SleepSession(start,ts(start),ts(end))
    private fun tip(focus:CoachFocus,rows:List<SleepSession> = emptyList(),room:Environment=Environment(),settings:Settings=Settings())=
        personalTip(focus,rows,room,settings,now,ZoneOffset.UTC)

    @Test fun midnightComparisonSkipsNapsAndUnloggedDays(){
        val rows=listOf(session("17T23:50","18T05:50"),session("18T13:00","18T13:40"),
            session("20T00:10","20T07:10"),session("12T21:00","13T07:00"),
            session("18T21:00","19T07:00").copy(deleted=true),SleepSession("active",now-60000))
        val result=tip(CoachFocus.SCHEDULE,rows)
        assertTrue(result.observation.contains("6h 50m (2/7 ngày)"))
        assertTrue(result.nextStep.contains("lệch 20 phút"))
    }
    @Test fun restUsesReportedQualityWithoutReadingNotes(){
        val row=session("19T22:00","20T05:00").copy(notes="PRIVATE")
        assertTrue(tip(CoachFocus.REST,listOf(row)).observation.contains("chưa có đánh giá"))
        val result=tip(CoachFocus.REST,listOf(row.copy(quality=2)))
        assertTrue(result.observation.contains("Kém"))
        assertFalse(result.toString().contains("PRIVATE"))
        assertEquals("sleep",tip(CoachFocus.REST).route)
    }
    @Test fun roomChecksPersonalThresholdAndFreshnessBeforeRecommendations(){
        val sample=SensorSample("t",now,23.0,50.0,100.0,"simulator")
        fun check(s:SensorSample,limit:Double)=tip(CoachFocus.ROOM,room=Environment(current=s),settings=Settings(lightMax=limit)).observation
        assertTrue(check(sample,80.0).contains("cao hơn"))
        assertTrue(check(sample,110.0).contains("đều trong ngưỡng"))
        assertTrue(check(sample.copy(timestamp=now-31000),80.0).contains("Chưa có số đo mới"))
        assertTrue(check(sample.copy(timestamp=now+60000),80.0).contains("Chưa có số đo mới"))
        assertTrue(check(sample.copy(light=null),80.0).contains("một phần"))
    }
}
