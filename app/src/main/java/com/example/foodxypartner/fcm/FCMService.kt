package com.example.foodxypartner.fcm

import android.content.ContentValues.TAG
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FCMService : FirebaseMessagingService() {

    override fun onNewToken(p0: String) {
        Log.d(TAG, "Refreshed token: $p0")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(p0)
    }

    private fun sendRegistrationToServer(p0: String?) {
        // TODO: Implement this method to send token to your app server.
    }


}