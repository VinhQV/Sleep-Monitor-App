package com.example.sleepmonitor.reminders
import android.app.*
import android.content.*
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.sleepmonitor.MainActivity
import com.example.sleepmonitor.R
import com.example.sleepmonitor.data.*
import java.time.*

object SleepReminders {
    private const val CHANNEL="sleep_gentle_reminders"
    fun options(c:Context):ReminderOptions=c.getSharedPreferences("sleep-reminders",0).let{ReminderOptions(it.getBoolean("enabled",false),it.getInt("bed",1350),it.getInt("morning",450),it.getBoolean("adaptive",true))}
    fun save(c:Context,o:ReminderOptions){c.getSharedPreferences("sleep-reminders",0).edit().putBoolean("enabled",o.enabled).putInt("bed",o.bedtime).putInt("morning",o.morning).putBoolean("adaptive",o.adaptive).apply();schedule(c)}
    fun channel(c:Context){c.getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL,"Nhắc ngủ nhẹ nhàng",NotificationManager.IMPORTANCE_LOW).apply{description="Nhắc ghi nhật ký và đánh giá buổi sáng";setShowBadge(true)})}
    private fun pending(c:Context,kind:String)=PendingIntent.getBroadcast(c,if(kind=="bed")710 else 711,Intent(c,ReminderReceiver::class.java).setAction(kind),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    fun schedule(c:Context){
        channel(c);val o=options(c);if(!o.enabled){c.getSystemService(NotificationManager::class.java).cancel(710);c.getSystemService(NotificationManager::class.java).cancel(711)};val alarm=c.getSystemService(AlarmManager::class.java)
        val rows=LocalStore(c).use{it.sessions(it.connection().demo)}
        for(kind in listOf("bed","morning")){
            val pi=pending(c,kind);alarm.cancel(pi)
            if(o.enabled){val minute=if(kind=="morning")o.morning else if(o.adaptive)suggestedBedtime(rows,o.bedtime)else o.bedtime
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,nextReminderTime(minute,ZonedDateTime.now()).toInstant().toEpochMilli(),pi)}
        }
    }
    fun notify(c:Context,kind:String,test:Boolean=false){
        if(!test&&!options(c).enabled)return
        val rows=LocalStore(c).use{it.sessions(it.connection().demo)}
        val msg=if(test)ReminderMessage("Nhắc nhở đã sẵn sàng","Thông báo sẽ nhẹ nhàng nhắc bạn ghi ngủ, khi phù hợp.","home")else reminderMessage(kind,rows,System.currentTimeMillis())?:return
        val prefs=c.getSharedPreferences("sleep-reminders",0);val stamp=LocalDate.now().toString()
        if(!test&&prefs.getString("sent-$kind",null)==stamp)return
        if(!NotificationManagerCompat.from(c).areNotificationsEnabled())return
        channel(c)
        val intent=Intent(c,MainActivity::class.java).putExtra("reminderDestination",msg.destination).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pi=PendingIntent.getActivity(c,if(kind=="bed")712 else 713,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        try{NotificationManagerCompat.from(c).notify(if(kind=="bed")710 else 711,NotificationCompat.Builder(c,CHANNEL).setSmallIcon(R.drawable.ic_sleep_notification).setContentTitle(msg.title).setContentText(msg.body).setStyle(NotificationCompat.BigTextStyle().bigText(msg.body)).setContentIntent(pi).setAutoCancel(true).setSilent(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build())
            if(!test)prefs.edit().putString("sent-$kind",stamp).apply()
        }catch(_:SecurityException){}
    }
}
class ReminderReceiver:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){
    if(i.action in listOf("bed","morning"))SleepReminders.notify(c,i.action!!)
    SleepReminders.schedule(c)
}}
