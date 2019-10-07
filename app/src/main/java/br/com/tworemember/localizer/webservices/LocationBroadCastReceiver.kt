package br.com.tworemember.localizer.webservices

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class LocationBroadCastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let { it.startService(Intent(it, LocationService::class.java)) }
    }
}