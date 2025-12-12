package com.hcmus.forumus_client.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.hcmus.forumus_client.data.model.User
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing user data operations with Firestore.
 * Handles CRUD operations for user profiles and authentication-related data.
 */
class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        private const val USERS_COLLECTION = "users"
    }

    private val usersCollection = firestore.collection(USERS_COLLECTION)

    /**
     * Retrieves a user by their unique ID.
     *
     * @param userId The unique identifier of the user
     * @return The user object, or empty User if not found
     */
    suspend fun getUserById(userId: String): User {
        return try {
            val document = usersCollection.document(userId).get().await()
            document.toObject(User::class.java) ?: User()
        } catch (e: Exception) {
            Log.e("UserRepositoryx", "Error fetching user by ID: $userId", e)
            User()
        }
    }

    /**
     * Retrieves the currently authenticated user.
     *
     * @return The current user object, or null if no user is logged in
     */
    suspend fun getCurrentUser(): User? {
        val currentUser = auth.currentUser ?: return null
        return getUserById(currentUser.uid)
    }

    /**
     * Retrieves multiple users by their IDs.
     * Uses Firestore whereIn query which is limited to 10 items per query.
     * Chunks large ID lists into multiple queries if needed.
     *
     * @param ids List of user IDs to retrieve
     * @return List of user objects found
     */
    suspend fun getUsersByIds(ids: List<String>): List<User> {
        if (ids.isEmpty()) return emptyList()

        return try {
            // Firestore whereIn supports maximum 10 items per query
            val chunkedIds = ids.chunked(10)
            val result = mutableListOf<User>()

            for (chunk in chunkedIds) {
                val query = usersCollection
                    .whereIn("uid", chunk)
                    .get()
                    .await()

                result += query.toObjects(User::class.java)
            }

            result
        } catch (e: Exception) {
            Log.e("UserRepository", "Error fetching multiple users", e)
            emptyList()
        }
    }

    /**
     * Searches for users by full name or email (case-insensitive).
     * Excludes the current authenticated user from results.
     *
     * @param query The search query string
     * @return List of matching users (up to 10 results)
     */
    suspend fun searchUsers(query: String): List<User> {
        return try {
            if (query.isBlank()) {
                return emptyList()
            }

            Log.i("UserRepository", "Search query: $query")
            val queryLower = query.lowercase()
            val currentUserId = auth.currentUser?.uid

            Log.i("UserRepository", "Current user ID: $currentUserId")
            
            // Get all users and filter client-side (Firestore doesn't support case-insensitive queries well)
            val querySnapshot = usersCollection
                .limit(50) // Limit to avoid large data transfer
                .get()
                .await()

            Log.i("UserRepository", "Total users fetched: ${querySnapshot.size()}")

            val allUsers = querySnapshot.documents.mapNotNull { doc ->
                try {
                    Log.i("UserRepository", "Processing document: ${doc.id}")
                    val user = doc.toObject(User::class.java)
                    
                    if (user != null) {
                        Log.i("UserRepository", "User converted successfully")
                        user.copy(uid = doc.id)
                    } else {
                        Log.w("UserRepository", "Failed to convert document ${doc.id} to User object")
                        null
                    }
                } catch (e: Exception) {
                    Log.e("UserRepository", "Exception converting document ${doc.id}: ${e.message}", e)
                    null
                }
            }

            Log.i("UserRepository", "All users with UIDs: ${allUsers.size} total")
            
            // Filter out current user and search results
            val filteredUsers = allUsers.filter { user ->
                user.uid != currentUserId
            }

            Log.i("UserRepository", "Users after excluding current user: ${filteredUsers.size}")

            // Client-side filtering for case-insensitive search
            return filteredUsers.filter { user ->
                user.fullName.lowercase().contains(queryLower) ||
                user.email.lowercase().contains(queryLower)
            }.take(10) // Limit results to 10 users

        } catch (e: Exception) {
            Log.e("UserRepository", "Error searching users", e)
            emptyList()
        }
    }

    /**
     * Saves or creates a user profile in Firestore.
     * Uses merge option to preserve existing data if document already exists.
     *
     * @param user The user object to save
     */
    suspend fun saveUser(user: User) {
        try {
            usersCollection
                .document(user.uid)
                .set(user, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error saving user: ${user.uid}", e)
            throw e
        }
    }

    /**
     * Updates specific user profile fields (fullName, profilePictureUrl).
     * Only updates provided fields, leaving others unchanged.
     *
     * @param uid The user ID
     * @param fullName The user's full name (optional)
     * @param profilePictureUrl The user's profile picture URL (optional)
     */
    suspend fun updateProfile(
        uid: String,
        fullName: String? = null,
        profilePictureUrl: String? = null,
    ) {
        try {
            val updates = mutableMapOf<String, Any>()

            fullName?.let { updates["fullName"] = it }
            profilePictureUrl?.let { updates["profilePictureUrl"] = it }

            if (updates.isNotEmpty()) {
                usersCollection
                    .document(uid)
                    .update(updates)
                    .await()
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating profile for user: $uid", e)
            throw e
        }
    }

    /**
     * Deletes a user document from Firestore.
     * Note: This only removes the user profile data, not the authentication account.
     *
     * @param uid The user ID
     */
    suspend fun deleteUser(uid: String) {
        try {
            usersCollection
                .document(uid)
                .delete()
                .await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error deleting user: $uid", e)
            throw e
        }
    }
}
