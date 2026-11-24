package com.example.sleepmonitor
import com.example.sleepmonitor.reminders.*
import com.example.sleepmonitor.data.*
import java.time.*
import org.junit.Assert.*
import org.junit.Test
class ReminderPolicyTest {
 private val now=ZonedDateTime.of(2026,11,22,8,0,0,0,ZoneId.systemDefault()).toInstant().toEpochMilli()
 @Test fun neverNudgeWhileRecordingSleep(){val rows=listOf(SleepSession("active",now-8*3600000));assertNull(reminderMessage("bed",rows,now));assertNull(reminderMessage("morning",rows,now))}
 @Test fun recentWakeSkipsBedtimeAndTargetsUnratedSession(){val rows=listOf(SleepSession("night",now-8*3600000,now-60000));assertNull(reminderMessage("bed",rows,now));assertEquals("rate:night",reminderMessage("morning",rows,now)!!.destination);assertNull(reminderMessage("morning",rows.map{it.copy(quality=4)},now))}
 @Test fun adaptiveTimeUnderstandsMidnightAndRequiresEnoughHistory(){val rows=(1..4).map{n->val d=ZonedDateTime.of(2026,11,22-n,if(n%2==0)23 else 0,if(n%2==0)55 else 5,0,0,ZoneId.systemDefault()).toInstant().toEpochMilli();SleepSession("n$n",d,d+8*3600000)};val m=suggestedBedtime(rows,1350,now);assertTrue(m>=23*60+30);assertEquals(1350,suggestedBedtime(rows.take(2),1350,now))}
 @Test fun nextDayFollowsLocalClockAcrossDst(){val n=ZonedDateTime.of(2026,3,7,23,0,0,0,ZoneId.of("America/New_York"));val t=nextReminderTime(450,n);assertEquals(7,t.hour);assertEquals(30,t.minute);assertEquals(LocalDate.of(2026,3,8),t.toLocalDate())}
 @Test fun oldPeriodEndsIncludeWholeWeekAndLeapMonth(){assertEquals(LocalDate.of(2024,2,29),periodEnd(true,LocalDate.of(2024,2,12)));assertEquals(LocalDate.of(2026,11,15),periodEnd(false,LocalDate.of(2026,11,11)))}
}
