package br.com.tworemember.localizer

import android.util.Log
import br.com.tworemember.localizer.model.User
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessageService : FirebaseMessagingService() {

    private val tag = "FirebaseMessageService"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage?.let {

            // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
            Log.d(tag, "From: " + it.from);

            // Check if message contains a data payload.
            if (it.data.isNotEmpty()) {
                Log.d(tag, "Message data payload: " + it.data);

                if (/* Check if data needs to be processed by lng running job */ true) {
                    // For lng-running tasks (10 seconds or more) use Firebase Job Dispatcher.
                    //scheduleJob();
                } else {
                    // Handle message within 10 seconds
                    //handleNow();
                }

            }

            // Check if message contains a notification payload.
            if (it.notification != null) {
                Log.d(tag, "Message Notification Body: " + it.notification!!.body);
            }

            // Also if you intend on generating your own notifications as a result of a received FCM
            // message, here is where that should be initiated. See sendNotification method below.
        }
    }

    override fun onNewToken(token: String) {
        Log.d(tag, "Refreshed token: $token");
        sendTokenToFunction(token)
    }

    private fun sendTokenToFunction(token: String){
        val user = Preferences(this).getUser()
        user?.let { callTokenFunction(token, it) }
    }

    private fun callTokenFunction(token: String, user: User){
        val body = TokenRequest(user.uuid, token)
        val functions = FirebaseFunctions.getInstance()
        functions.getHttpsCallable("newToken")
            .call(body)
            .addOnCompleteListener {
                if (it.isSuccessful){
                    Log.d("Token", "Token registrado com sucesso")
                }
            }
    }
}
