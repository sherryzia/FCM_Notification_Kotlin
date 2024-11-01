// NotificationUtils.kt

package com.example.app

import android.util.Log
import com.example.app.utils.FirebaseUtils
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

object NotificationUtils {

    fun sendNotification(message: String?, otherUserModel: UserModel, currentUserModel: UserModel) {
        Log.d("NotificationUtils", "sendNotification: ${otherUserModel.fcmToken}")
        
        try {
            val jsonObject = JSONObject()
            val messageObject = JSONObject()
            val notificationObject = JSONObject()
            val dataObject = JSONObject()

            // Setting notification title and body
            notificationObject.put("title", currentUserModel.name)
            notificationObject.put("body", message)

            // Adding user data
            dataObject.put("userId", currentUserModel.userId)

            // Constructing message with notification and data
            messageObject.put("notification", notificationObject)
            messageObject.put("data", dataObject)
            messageObject.put("token", otherUserModel.fcmToken)
            jsonObject.put("message", messageObject)

            // Sending the notification
            callApi(jsonObject)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun callApi(jsonObject: JSONObject) {
        val JSON: MediaType = "application/json; charset=utf-8".toMediaTypeOrNull() ?: return
        val client = OkHttpClient()
        val url = "https://fcm.googleapis.com/v1/projects/YOUR_PROJECT_ID/messages:send"
        val body: RequestBody = jsonObject.toString().toRequestBody(JSON)

        runBlocking {
            launch(Dispatchers.IO) {
                val accessToken = AccessToken().getAccessToken() ?: return@launch

                Log.d("NotificationUtils", "Retrieved OAuth token: $accessToken")

                val request: Request = Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Content-Type", "application/json")
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        e.printStackTrace()
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            Log.d("FCM", "Notification sent successfully: ${response.body?.string()}")
                        } else {
                            Log.e("FCM", "Error sending notification: ${response.body?.string()}")
                        }
                    }
                })
            }
        }
    }

    fun getFcmToken(currentUid: String) {
        FirebaseUtils.collectionUserDetails
            .whereEqualTo("userId", currentUid)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val userDocument = querySnapshot.documents[0]
                    val uid = userDocument.getString("phone").toString()

                    FirebaseMessaging.getInstance().getToken().addOnSuccessListener {
                        if (it != null) {
                            val token = it
                            FirebaseUtils.collectionUserDetails.document(uid).update("fcmToken", token)
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("NotificationUtils", "Failed to fetch user details", exception)
            }
    }
}
