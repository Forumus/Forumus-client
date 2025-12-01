package com.hcmus.forumus_client.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hcmus.forumus_client.data.model.User
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val usersCollection = firestore.collection("users")

    suspend fun getUserById(userId: String): User {
        return try {
            val document = usersCollection.document(userId).get().await()
            document.toObject(User::class.java) ?: User()
        } catch (e: Exception) {
            User()
        }
    }
}