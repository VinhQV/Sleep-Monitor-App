package com.example.sleepmonitor
import com.example.sleepmonitor.data.*
import java.time.*
import org.junit.Assert.*
import org.junit.Test
class AppearanceAndPeriodTest {
 @Test fun automaticAppearanceChangesAtSixAndEighteen(){
   assertTrue(usesDarkTheme(AppearanceMode.AUTOMATIC,5));assertFalse(usesDarkTheme(AppearanceMode.AUTOMATIC,6))
   assertFalse(usesDarkTheme(AppearanceMode.AUTOMATIC,17));assertTrue(usesDarkTheme(AppearanceMode.AUTOMATIC,18))
   assertFalse(usesDarkTheme(AppearanceMode.LIGHT,23));assertTrue(usesDarkTheme(AppearanceMode.DARK,12))
   assertEquals(AppearanceMode.AUTOMATIC,AppearanceMode.fromStored("invalid"))
 }
 @Test fun calendarPreservesMissingDaysAndSumsSleepByWakeDate(){
   fun at(d:String)=LocalDateTime.parse(d).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
   val rows=listOf(SleepSession("night",at("2026-11-17T23:00"),at("2026-11-18T06:00")),SleepSession("nap",at("2026-11-18T13:00"),at("2026-11-18T14:00")),SleepSession("deleted",at("2026-11-19T01:00"),at("2026-11-19T08:00"),deleted=true))
   val totals=periodTotals(rows,false,LocalDate.parse("2026-11-20"))
   assertEquals(5,totals.size);assertNull(totals[0].second);assertEquals(480L,totals[2].second);assertNull(totals[3].second)
   assertEquals(480L,dailyAverage(rows))
   assertEquals(20,periodTotals(rows,true,LocalDate.parse("2026-11-20")).size)
 }
}
