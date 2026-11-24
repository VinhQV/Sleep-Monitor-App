package com.example.sleepmonitor
import android.content.Intent
import android.app.NotificationManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import com.example.sleepmonitor.reminders.*
@RunWith(AndroidJUnit4::class)
class ReminderDeliveryTest {
 @Test fun previewPostsSilentNotificationAndDeepLink(){
  val c=InstrumentationRegistry.getInstrumentation().targetContext
  SleepReminders.notify(c,"bed",true)
  val manager=c.getSystemService(NotificationManager::class.java)
  val notification=manager.activeNotifications.firstOrNull{it.id==710}?.notification
  assertNotNull(notification);assertNotNull(notification!!.contentIntent)
  assertEquals(NotificationManager.IMPORTANCE_LOW,manager.getNotificationChannel(notification.channelId).importance)
  notification.contentIntent.send();Thread.sleep(800)
  manager.cancel(710)
 }
}
