package br.com.tworemember.localizer.webservices

import android.app.IntentService
import android.content.Intent
import br.com.tworemember.localizer.providers.Preferences

class LocationService : IntentService(LocationService::class.java.simpleName) {

    override fun onHandleIntent(intent: Intent?) {
        val macaddress = Preferences(this).getMacAddress()
        macaddress?.let { FunctionTrigger.callLastPositionFunction(it) }
    }
}