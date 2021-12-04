package com.example.foodxypartner


import android.content.ContentValues.TAG
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.edit
import com.example.foodxypartner.core.Constants

import com.google.firebase.messaging.FirebaseMessagingService

class MyFirebaseInstanceIDService : FirebaseMessagingService() {

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