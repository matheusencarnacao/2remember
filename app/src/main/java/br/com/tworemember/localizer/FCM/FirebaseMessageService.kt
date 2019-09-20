package br.com.tworemember.localizer.FCM

import android.util.Log
import br.com.tworemember.localizer.providers.Preferences
import br.com.tworemember.localizer.webservices.model.TokenRequest
import br.com.tworemember.localizer.model.User
import br.com.tworemember.localizer.webservices.Functions
import br.com.tworemember.localizer.webservices.RetrofitClient
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
        val functions = RetrofitClient.getInstance().create(Functions::class.java)
        val body = TokenRequest(user.uuid, token)

        val tokenCall = functions.newToken(body)
        tokenCall.enqueue(object: Callback<Void> {
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("Token: ", "Erro ao enviar o token: E: ${t.message}")
            }

            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                Log.d("Token: ", "token enviado com sucesso!")
            }

        })

    }
}
