package com.hcmus.forumus_client.data.repository

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

    /**
     * Retrieves a user by their unique ID.
     *
     * @param userId The unique identifier of the user
     * @return The user object
     * @throws IllegalStateException if user is not found
     */
    suspend fun getUserById(userId: String): User {
        val doc = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .get()
            .await()

        return doc.toObject(User::class.java)
            ?: throw IllegalStateException("User not found: $userId")
    }

    /**
     * Retrieves a user by their email address.
     *
     * @param email The email address of the user
     * @return The user object, or null if not found
     */
    suspend fun getUserByEmail(email: String): User? {
        val result = firestore.collection(USERS_COLLECTION)
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .await()

        return if (result.isEmpty) null else result.documents[0].toObject(User::class.java)
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

        // Firestore whereIn supports maximum 10 items per query
        val chunkedIds = ids.chunked(10)
        val result = mutableListOf<User>()

        for (chunk in chunkedIds) {
            val query = firestore.collection(USERS_COLLECTION)
                .whereIn("uid", chunk)
                .get()
                .await()

            result += query.toObjects(User::class.java)
        }

        return result
    }

    /**
     * Saves or creates a user profile in Firestore.
     * Uses merge option to preserve existing data if document already exists.
     *
     * @param user The user object to save
     */
    suspend fun saveUser(user: User) {
        firestore.collection(USERS_COLLECTION)
            .document(user.uid)
            .set(user, SetOptions.merge())
            .await()
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
        val updates = mutableMapOf<String, Any>()

        fullName?.let { updates["fullName"] = it }
        profilePictureUrl?.let { updates["profilePictureUrl"] = it }

        if (updates.isNotEmpty()) {
            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .update(updates)
                .await()
        }
    }

    /**
     * Deletes a user document from Firestore.
     * Note: This only removes the user profile data, not the authentication account.
     *
     * @param uid The user ID
     */
    suspend fun deleteUser(uid: String) {
        firestore.collection(USERS_COLLECTION)
            .document(uid)
            .delete()
            .await()
    }
}
