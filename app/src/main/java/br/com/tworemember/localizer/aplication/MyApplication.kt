package br.com.tworemember.localizer.aplication

import android.app.Application
import android.util.Log
import br.com.tworemember.localizer.fcm.WsToken
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.google.firebase.iid.FirebaseInstanceId

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        FacebookSdk.sdkInitialize(applicationContext);
        AppEventsLogger.activateApp(this);

        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener {
            if (!it.isSuccessful) {
                Log.w("MyApplication", "getInstanceId failed", it.exception)
                return@addOnCompleteListener
            }

            // Get new Instance ID token
            val token = it.result?.token

            token?.let { t -> WsToken(this).sendTokenToFunction(t) }
        }
    }
}