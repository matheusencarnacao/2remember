package br.com.tworemember.localizer.webservices

import android.app.AlarmManager
import android.content.Context.ALARM_SERVICE
import androidx.core.content.ContextCompat.getSystemService
import android.app.PendingIntent
import android.content.Context
import com.facebook.FacebookSdk.getApplicationContext
import android.content.Intent


class LocationScheduler {

    companion object {
        fun startService(context: Context) {
            val intent = Intent(context, LocationBroadCastReceiver::class.java)
            val locationAlarmIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            val startTime = System.currentTimeMillis() //alarm starts immediately
            val backupAlarmMgr = context.getSystemService(ALARM_SERVICE) as AlarmManager
            backupAlarmMgr.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                startTime,
                15000,
                locationAlarmIntent
            ) // alarm will repeat after every 15 minutes
        }
    }
}