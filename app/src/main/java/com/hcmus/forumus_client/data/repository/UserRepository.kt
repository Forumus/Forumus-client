package com.hcmus.forumus_client.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.firestore.SetOptions
import com.hcmus.forumus_client.data.model.User
import kotlinx.coroutines.tasks.await
import android.net.Uri

/** Result of saving a post operation. */
sealed class SavePostResult {
    object Success : SavePostResult()
    object AlreadySaved : SavePostResult()
    data class Error(val message: String) : SavePostResult()
}

/** Repository for managing user data with Firestore. */
class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    companion object {
        private const val USERS_COLLECTION = "users"
        private const val AVATAR_FOLDER = "avatars"
    }

    private val usersCollection = firestore.collection(USERS_COLLECTION)

    suspend fun searchUsersCandidates(): List<User> {
        return try {
            usersCollection
                .limit(100)
                .get()
                .await()
                .toObjects(User::class.java)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error searching user candidates", e)
            emptyList()
        }
    }

    /** Retrieves a user by their ID. */
    suspend fun getUserById(userId: String): User {
        return try {
            val document = usersCollection.document(userId).get().await()
            document.toObject(User::class.java) ?: User()
        } catch (e: Exception) {
            Log.e("UserRepositoryx", "Error fetching user by ID: $userId", e)
            User()
        }
    }

    /** Retrieves the currently authenticated user. */
    suspend fun getCurrentUser(): User? {
        val currentUser = auth.currentUser ?: return null
        return getUserById(currentUser.uid)
    }

    /** Retrieves multiple users by their IDs. */
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

    /** Searches for users by name or email. */
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
                (user.fullName.lowercase().contains(queryLower) ||
                user.email.lowercase().contains(queryLower)) &&
                !user.email.lowercase().endsWith("@admin.forumus.me")
            }.take(10) // Limit results to 10 users

        } catch (e: Exception) {
            Log.e("UserRepository", "Error searching users", e)
            emptyList()
        }
    }

    /** Saves or creates a user profile in Firestore. */
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

    /** Updates specific user profile fields. */
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

    /** Uploads avatar to Firebase Storage and returns download URL. */
    suspend fun uploadAvatar(uid: String, uri: Uri): String {
        return try {
            val ref = storage.reference.child("$AVATAR_FOLDER/$uid.jpg")

            // Upload file
            ref.putFile(uri).await()

            // Get download url
            val downloadUrl = ref.downloadUrl.await().toString()

            Log.i("UserRepository", "Avatar uploaded: $downloadUrl")
            downloadUrl
        } catch (e: Exception) {
            Log.e("UserRepository", "Error uploading avatar for user: $uid", e)
            throw e
        }
    }

    /** Deletes a user document from Firestore. */
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

    /** Saves a post to user's saved posts list. */
    suspend fun savePost(postId: String): SavePostResult {
        return try {
            val currentUserId = auth.currentUser?.uid
            if (currentUserId == null) {
                return SavePostResult.Error("User not authenticated")
            }
            
            val userDoc = usersCollection.document(currentUserId)
            
            var wasAlreadySaved = false
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userDoc)
                val currentSaved = snapshot.get("followedPostIds") as? List<*> ?: emptyList<String>()
                val savedList = currentSaved.mapNotNull { it as? String }.toMutableList()
                
                if (savedList.contains(postId)) {
                    wasAlreadySaved = true
                } else {
                    savedList.add(postId)
                    transaction.update(userDoc, "followedPostIds", savedList)
                }
            }.await()
            
            if (wasAlreadySaved) {
                Log.i("UserRepository", "Post $postId is already saved")
                SavePostResult.AlreadySaved
            } else {
                Log.i("UserRepository", "Post $postId saved successfully")
                SavePostResult.Success
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error saving post: $postId", e)
            SavePostResult.Error(e.message ?: "Unknown error")
        }
    }

    /** Removes a post from user's saved posts list. */
    suspend fun unsavePost(postId: String) {
        try {
            val currentUserId = auth.currentUser?.uid ?: return
            val userDoc = usersCollection.document(currentUserId)
            
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userDoc)
                val currentSaved = snapshot.get("savedPostIds") as? List<*> ?: emptyList<String>()
                val savedList = currentSaved.mapNotNull { it as? String }.toMutableList()
                
                if (savedList.contains(postId)) {
                    savedList.remove(postId)
                    transaction.update(userDoc, "savedPostIds", savedList)
                }
            }.await()
            
            Log.i("UserRepository", "Post $postId unsaved successfully")
        } catch (e: Exception) {
            Log.e("UserRepository", "Error unsaving post: $postId", e)
            throw e
        }
    }
}
