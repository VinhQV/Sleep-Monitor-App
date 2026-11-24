package com.example.sleepmonitor
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sleepmonitor.data.*
import com.example.sleepmonitor.ui.sleep.SleepScreen
import com.example.sleepmonitor.ui.theme.SleepTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class DiaryRedesignTest {
 @get:Rule val ui=createComposeRule()
 @Test fun feelingAndSnoringAreSavedWithTimesInLightTheme(){
   val now=System.currentTimeMillis()-60000
   val existing=SleepSession("review",now-8*3600000,now)
   var saved:SleepSession?=null
   ui.setContent{SleepTheme(false,AppearanceMode.LIGHT){SleepScreen(existing){id,start,end,q,n,i->saved=SleepSession(id!!,start,end,q,n,i)}}}
   ui.onNodeWithText("Tốt",useUnmergedTree=true).performScrollTo().performClick()
   ui.onNodeWithText("Có ngáy").performScrollTo().performClick()
   ui.onNodeWithText("LƯU GIẤC NGỦ").performScrollTo().performClick()
   ui.runOnIdle{assertEquals(4,saved!!.quality);assertTrue(saved!!.notes.lines().contains("Có ngáy"));assertEquals(480L,saved!!.durationMinutes)}
 }
 @Test fun reversedTimesCannotBeSavedInDarkTheme(){
   val now=System.currentTimeMillis()-60000
   ui.setContent{SleepTheme(false,AppearanceMode.DARK){SleepScreen(SleepSession("invalid",now,now-3600000)){_,_,_,_,_,_->error("Invalid session was saved")}}}
   ui.onNodeWithText("Giờ thức dậy cần sau giờ đi ngủ.").assertExists()
   ui.onNodeWithText("LƯU GIẤC NGỦ").performScrollTo().assertIsNotEnabled()
 }
}
