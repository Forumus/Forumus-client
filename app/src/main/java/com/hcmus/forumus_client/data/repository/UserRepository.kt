package com.hcmus.forumus_client.data.repository

import android.util.Log
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

    suspend fun searchUsers(query: String): List<User> {
        try {
            if (query.isBlank()) {
                return emptyList()
            }

            Log.i("UserRepository", "Search query: $query")
            val queryLower = query.lowercase()
            val currentUserId = auth.currentUser?.uid

            Log.i("UserRepository", "Current user ID: $currentUserId")
            
            // Get all users and filter client-side (since Firestore doesn't support case-insensitive queries well)
            val querySnapshot = usersCollection
                .limit(50) // Limit to avoid large data transfer
                .get()
                .await()

            Log.i("UserRepository", "Total users fetched: $querySnapshot, size: ${querySnapshot.size()}")

            val allUsers = querySnapshot.documents.mapNotNull { doc ->
                try {
                    Log.i("UserRepository", "Processing document: ${doc.id}")
                    Log.i("UserRepository", "Document data: ${doc.data}")
                    val user = doc.toObject(User::class.java)
                    Log.i("UserRepository", "Converted user: $user")
                    if (user != null) {
                        Log.i("UserRepository", "User converted successfully")
                        user.copy(uid = doc.id)
                    } else {
                        Log.w(
                            "UserRepository",
                            "Failed to convert document ${doc.id} to User object"
                        )
                        null
                    }
                } catch (e: Exception) {
                    Log.e(
                        "UserRepository",
                        "Exception converting document ${doc.id}: ${e.message}",
                        e
                    )
                    null
                }
            }

            Log.i("UserRepository", "All users with UIDs: $allUsers")
            
            val users = allUsers.filter { user ->
                val shouldInclude = user.uid != currentUserId
                Log.i("UserRepository", "User ${user.uid} (${user.fullName}) - should include: $shouldInclude")
                shouldInclude
            }

            Log.i("UserRepository", "Users after excluding current user: $users")

            // Client-side filtering for case-insensitive search
            return users.filter { user ->
                user.fullName.lowercase().contains(queryLower) ||
                user.email.lowercase().contains(queryLower)
            }.take(10) // Limit results to 10 users

        } catch (e: Exception) {
            return emptyList()
        }
    }
}