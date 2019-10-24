package br.com.tworemember.localizer.fcm

import android.content.Context
import android.util.Log
import com.google.firebase.iid.FirebaseInstanceId

class TokenProvider {

    companion object{
        fun sendToken(context: Context){
            FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener {
                if (!it.isSuccessful) {
                    Log.w("MyApplication", "getInstanceId failed", it.exception)
                    return@addOnCompleteListener
                }

                // Get new Instance ID token
                val token = it.result?.token

                token?.let { t -> WsToken(context).sendTokenToFunction(t) }
            }
        }
    }
}