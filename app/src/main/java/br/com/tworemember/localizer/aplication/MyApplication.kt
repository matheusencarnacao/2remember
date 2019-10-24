package br.com.tworemember.localizer.aplication

import android.app.Application
import android.util.Log
import br.com.tworemember.localizer.fcm.TokenProvider
import br.com.tworemember.localizer.fcm.WsToken
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.google.firebase.iid.FirebaseInstanceId

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        FacebookSdk.sdkInitialize(applicationContext);
        AppEventsLogger.activateApp(this);

       TokenProvider.sendToken(this)
    }
}