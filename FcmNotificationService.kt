// FcmNotificationService.kt

package com.example.app

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FcmNotificationService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FcmNotificationService", "Message received from: ${remoteMessage.from}")
        
        // Display notification if the message contains notification data
        remoteMessage.notification?.let {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification: Notification = Notification.Builder(this)
                .setContentTitle(it.title)
                .setContentText(it.body)
                .setSmallIcon(R.drawable.notification_icon)
                .build()
            notificationManager.notify(0, notification)
        }
    }

    override fun onNewToken(token: String) {
        Log.d("FcmNotificationService", "Refreshed token: $token")
        // Update the FCM token in Firestore if needed
    }
}
