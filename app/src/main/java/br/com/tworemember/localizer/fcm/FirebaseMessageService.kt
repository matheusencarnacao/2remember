package br.com.tworemember.localizer.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import br.com.tworemember.localizer.R
import br.com.tworemember.localizer.activities.HomeActivity
import br.com.tworemember.localizer.providers.Preferences
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class FirebaseMessageService : FirebaseMessagingService() {

    private val tag = "FirebaseMessageService"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        val content = remoteMessage.data

        when(val type = content["type"]){
            "outOfRange" -> { outOfRange(content, type) }
            "panicButton" -> { panicButton(content, type) }
            "disconnectedBand" -> { disconnectedBand(content, type) }
            "lowBattery" -> { lowBattery(content, type) }
        }
    }

    private fun outOfRange(data: Map<String, String>, type: String) {
        val id = 1
        val title = "Fora da área segura"
        val text = "Corre! o véio fugiu!"
        val icon = R.drawable.ic_location_off_red_24dp

        val bundle = Bundle()
        bundle.putDouble("lat", (data["lat"] ?: error("")).toDouble())
        bundle.putDouble("lng", (data["lng"] ?: error("")).toDouble())

        showNotification(id, title, text, icon, bundle, type)
    }

    private fun panicButton(data: Map<String, String>, type: String){
        val id = 2
        val title = "Botão do pânico!"
        val text = "Ai brother o véio ta pedindo ajuda!"
        val icon = R.drawable.ic_warning_red_24dp

        val status = (data["panicButton"] ?: error("")).toBoolean()
        Preferences(this).setPanicButtonOn(status)

        showNotification(id, title, text, icon, Bundle(), type)
    }

    private fun disconnectedBand(data: Map<String, String>, type: String){
        val id = 3
        val title = "Pulseira desconectada"
        val text =  "Mano, poe a pulseira no seu veio"
        val icon = R.drawable.ic_person_off

        val status = (data["disconnectedBand"] ?: error("")).toBoolean()
        Preferences(this).setDisconnectedBand(status)

        showNotification(id, title, text, icon, Bundle(), type)
    }

    private fun lowBattery(data: Map<String, String>, type: String){
        val id = 4
        val title = "Pulseira com bateria fraca"
        val text =  "Ai doido, a bateria ta acabando"
        val icon = R.drawable.ic_battery_alert_red_24dp

        val status = (data["lowBattery"] ?: error("")).toBoolean()
        Preferences(this).setBatteryLow(status)

        showNotification(id, title, text, icon, Bundle(), type)
    }

    private fun showNotification(id: Int,
                                 title: String,
                                 text: String,
                                 icon: Int,
                                 data: Bundle,
                                 type: String){
        val intent = Intent(this, HomeActivity::class.java)
        intent.putExtra("data", data)
        intent.putExtra("type", type)
        val p = getPendingIntent(id, intent, this)

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(NotificationChannel(type, title, NotificationManager.IMPORTANCE_HIGH))
        }

        val notificacao = NotificationCompat.Builder(this, type)
        notificacao.setSmallIcon(icon)
        notificacao.setContentTitle(title)
        notificacao.setContentText(text)
        notificacao.setContentIntent(p)

        nm.notify(id, notificacao.build())
    }

    private fun getPendingIntent(id: Int, intent: Intent, context: Context): PendingIntent {
        val stackBuilder = TaskStackBuilder.create(context)
        stackBuilder.addParentStack(intent.component)
        stackBuilder.addNextIntent(intent)

        return stackBuilder.getPendingIntent(id, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onNewToken(token: String) {
        Log.d(tag, "Refreshed token: $token")
        WsToken(this).sendTokenToFunction(token)
    }
}
