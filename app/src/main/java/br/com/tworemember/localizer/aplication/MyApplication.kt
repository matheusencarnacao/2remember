package br.com.tworemember.localizer.aplication

import android.util.Log
import androidx.multidex.MultiDexApplication
import br.com.tworemember.localizer.fcm.WsToken
import com.google.firebase.iid.FirebaseInstanceId

class MyApplication : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()

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