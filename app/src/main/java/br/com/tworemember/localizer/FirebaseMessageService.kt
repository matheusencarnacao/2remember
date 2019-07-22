package br.com.tworemember.localizer

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessageService : FirebaseMessagingService() {

    private val tag = "FirebaseMessageService"

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        remoteMessage?.let {

            // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
            Log.d(tag, "From: " + it.from);

            // Check if message contains a data payload.
            if (it.data.isNotEmpty()) {
                Log.d(tag, "Message data payload: " + it.data);

                if (/* Check if data needs to be processed by long running job */ true) {
                    // For long-running tasks (10 seconds or more) use Firebase Job Dispatcher.
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

    override fun onNewToken(token: String?) {
        Log.d(tag, "Refreshed token: $token");

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        //sendRegistrationToServer(token);
    }
}
