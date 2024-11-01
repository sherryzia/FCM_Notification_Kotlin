// FirebaseUtils.kt

package com.example.app.utils

import com.google.firebase.firestore.FirebaseFirestore

object FirebaseUtils {
    val db = FirebaseFirestore.getInstance()
    val collectionUserDetails = db.collection("users")
}
