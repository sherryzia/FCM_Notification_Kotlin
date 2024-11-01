# Firebase Cloud Messaging (FCM) Notifications with Kotlin

This guide details how to set up and send push notifications with Firebase Cloud Messaging (FCM) in a Kotlin Android app. Each section covers one of the essential functions needed to build a notification system for messaging.

## Overview
Our FCM notification system consists of the following core components:

`sendNotification`: Constructs and sends a notification with a JSON payload.

`callApi`: Performs an API call to FCM to send the notification.

`getFcmToken`: Retrieves and updates the FCM token for the user.

`AccessToken`: Generates an OAuth token for accessing FCM APIs.

## Prerequisites
Ensure the following steps are completed before diving into the code:

### Set up Firebase in your Android project.
Download the JSON file for your Firebase service account credentials:
Go to Firebase Console > Project Settings > Service accounts and Generate a new private key.

## Code Implementation
### 1. Sending Notifications with sendNotification
The sendNotification function constructs a JSON payload containing the notification details (title, body, and additional data) and initiates the notification send request using the callApi function.

```kotlin
fun sendNotification(message: String?) {
    Log.d("otherUserID", "sendNotification: ${otherUserModel.fcmToken}")
    currentUserDetails { userRef ->
        userRef?.get()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val currentUser = task.result.toObject(UserModel::class.java)
                currentUser?.let {
                    try {
                        // Build JSON payload
                        val jsonObject = JSONObject()
                        val messageObject = JSONObject()
                        val notificationObject = JSONObject()
                        val dataObject = JSONObject()

                        // Set notification title and body
                        notificationObject.put("title", currentUser.name)
                        notificationObject.put("body", message)

                        // Add user data
                        dataObject.put("userId", currentUser.userId)

                        // Construct message with notification and data
                        messageObject.put("notification", notificationObject)
                        messageObject.put("data", dataObject)
                        messageObject.put("token", otherUserModel.fcmToken)
                        jsonObject.put("message", messageObject)

                        // Call FCM API to send notification
                        callApi(jsonObject)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                Log.e("sendNotification", "Failed to fetch current user details.")
            }
        }?.addOnFailureListener { exception ->
            Log.e("sendNotification", "Error retrieving user data", exception)
        }
    }
}
```
### 2. Making API Calls with callApi
The callApi function performs the HTTP request to FCM's API endpoint using OkHttpClient. This includes setting up headers with the OAuth token generated from AccessToken.

```kotlin
fun callApi(jsonObject: JSONObject) {
    val JSON: MediaType = "application/json; charset=utf-8".toMediaTypeOrNull() ?: return
    val client = OkHttpClient()
    val url = "https://fcm.googleapis.com/v1/projects/chatappfirebase-3ebb8/messages:send"
    val body: RequestBody = jsonObject.toString().toRequestBody(JSON)

    lifecycleScope.launch(Dispatchers.IO) {
        val accessToken = AccessToken().accessToken

        Log.d("xyz", "Retrieved OAuth token: $accessToken")

        // Create request with the OAuth token
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

```
### 3. Retrieving the FCM Token with getFcmToken
This function retrieves the device's FCM token and updates the user’s document in Firestore with this token, ensuring notifications can be targeted correctly.

```kotlin
fun getFcmToken() {
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
            Log.e("fetchUserDetails", "Failed to fetch user details", exception)
        }
}
```
### 4. Generating Access Tokens with AccessToken
The AccessToken class retrieves an OAuth token required to authorize FCM API requests. Replace jsonString with the JSON credentials from your Firebase service account.

```java
public class AccessToken {
    private static final String firebaseMessagingScope = "https://www.googleapis.com/auth/firebase.messaging";

    public String getAccessToken() {
        try {
            String jsonString = "{ /* JSON credentials */ }";
            InputStream stream = new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8));
            GoogleCredentials googleCredentials = GoogleCredentials.fromStream(stream).createScoped(firebaseMessagingScope);
            googleCredentials.refresh();
            return googleCredentials.getAccessToken().getTokenValue();
        } catch (Exception e) {
            Log.e("AccessToken", "getAccessToken: " + e.getLocalizedMessage());
            return null;
        }
    }
}
```
### 5. Configuring FcmNotificationService in AndroidManifest.xml
Finally, add the Firebase Messaging service to your Android manifest file to handle incoming messages.

```xml
<service
    android:name=".FcmNotificationService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```
