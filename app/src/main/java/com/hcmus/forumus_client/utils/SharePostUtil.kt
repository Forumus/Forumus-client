package com.hcmus.forumus_client.utils

import android.util.Log
import com.hcmus.forumus_client.data.model.User
import com.hcmus.forumus_client.data.model.UserRole
import com.hcmus.forumus_client.data.model.UserStatus
import com.hcmus.forumus_client.data.repository.ChatRepository
import com.hcmus.forumus_client.data.repository.PostRepository
import com.hcmus.forumus_client.data.repository.UserRepository

object SharePostUtil {

    private const val TAG = "SharePostUtil"
    private const val SHARE_URL_PREFIX = "app/forumus/share/"
    private const val SHARE_MESSAGE_FORMAT = "Check out this post: %s"

    // PostId pattern: alphanumeric with underscores (e.g., POST_20241221_001, post_12345,
    // ABC123_xyz)
    private val VALID_POST_ID_PATTERN = "^[a-zA-Z0-9_-]+$".toRegex()

    /**
     * Generates a share URL for a post
     * @param postId The ID of the post to share
     * @return Share URL in format: app/forumus/share/<PostID>
     */
    fun generateShareUrl(postId: String): String {
        return "$SHARE_URL_PREFIX$postId"
    }

    /**
     * Extracts post ID from a share URL with strict validation
     * @param url The share URL
     * @return The post ID if valid, or null if the URL is not a valid share URL
     */
    fun extractPostIdFromUrl(url: String): String? {
        return if (url.startsWith(SHARE_URL_PREFIX)) {
            val postId = url.substringAfter(SHARE_URL_PREFIX)
            // Validate that extracted postId is not empty and matches pattern
            if (postId.isNotEmpty() && postId.matches(VALID_POST_ID_PATTERN)) {
                postId
            } else {
                null
            }
        } else {
            null
        }
    }

    /**
     * Checks if a string is a strictly valid share URL
     * - Must start with SHARE_URL_PREFIX
     * - Must have non-empty PostId after prefix
     * - PostId must match valid pattern
     * @param text The text to check
     * @return True if the text is a valid share URL (format-wise, not Firebase validation)
     */
    fun isShareUrl(text: String): Boolean {
        if (!text.startsWith(SHARE_URL_PREFIX)) return false

        val postId = text.substringAfter(SHARE_URL_PREFIX)
        return postId.isNotEmpty() && postId.matches(VALID_POST_ID_PATTERN)
    }

    /**
     * Validates a share URL by checking:
     * 1. Format is strictly: SHARE_URL_PREFIX + valid PostId
     * 2. The extracted PostId exists in Firebase
     *
     * @param url The share URL to validate
     * @param postRepository The PostRepository instance to check Firebase
     * @return Result<Pair<String, Post>> with (postId, post) on success, exception on failure
     */
    suspend fun validateShareUrl(
            url: String,
            postRepository: PostRepository = PostRepository()
    ): Result<com.hcmus.forumus_client.data.model.Post> {
        return try {
            // Step 1: Check format validity
            val postId = extractPostIdFromUrl(url)
            if (postId == null) {
                Log.w(TAG, "Invalid share URL format: $url")
                return Result.failure(
                        IllegalArgumentException(
                                "Invalid share URL format. Expected: ${SHARE_URL_PREFIX}<PostID>"
                        )
                )
            }

            Log.d(TAG, "Share URL format valid. Checking if post exists in Firebase: $postId")

            // Step 2: Check if post exists in Firebase
            val post = postRepository.getPostById(postId)
            if (post == null) {
                Log.w(TAG, "Post not found in Firebase: $postId")
                return Result.failure(
                        IllegalArgumentException("Post not found. This post may have been deleted.")
                )
            }

            Log.d(TAG, "Post found in Firebase: $postId - ${post.title}")
            Result.success(post)
        } catch (e: Exception) {
            Log.e(TAG, "Error validating share URL", e)
            Result.failure(e)
        }
    }

    /**
     * Gets recipients for share dialog from the database.
     * Fetches users from Firestore via UserRepository, excluding the current user.
     * @param userRepository The UserRepository instance to use for fetching users
     * @return List of users to share with (excluding current user)
     */
    suspend fun getRecipients(userRepository: UserRepository = UserRepository()): List<User> {
        return try {
            Log.d(TAG, "Fetching recipients from database")
            val allUsers = userRepository.searchUsersCandidates()
            
            // Get current user to exclude from recipients
            val currentUser = userRepository.getCurrentUser()
            val currentUserId = currentUser?.uid
            
            // Filter out current user and ADMIN users from recipients
            val recipients = allUsers.filter { user ->
                user.uid != currentUserId && user.role != UserRole.ADMIN
            }
            
            Log.d(TAG, "Retrieved ${recipients.size} recipients from database (excluding current user and admins)")
            recipients
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching recipients", e)
            emptyList()
        }
    }

    /**
     * Gets mock recipients for share dialog. In a real implementation, this would fetch the user's
     * contacts or chat list.
     * @return List of mock users to share with
     * @deprecated Use getRecipients() instead to fetch real users from database
     */
    @Deprecated("Use getRecipients() instead", ReplaceWith("getRecipients()"))
    fun getMockRecipients(): List<User> {
        return listOf(
                User(
                        uid = "BOfXQRfVdONENpbVR6oVtgPGKyh2",
                        email = "longto.xp@gmail.com",
                        fullName = "Long To 2",
                        role = UserRole.STUDENT,
                        profilePictureUrl = null,
                        createdAt = 1764947972943,
                        emailVerified = true,
                        followedPostIds = emptyList(),
                        reportCount = 0,
                        status = UserStatus.NORMAL
                ),
                User(
                        uid = "CnRTtVpEpHMaVNXL0tZssJUD6i23",
                        email = "23120096@student.hcmus.edu.vn",
                        fullName = "toan toan",
                        role = UserRole.STUDENT,
                        profilePictureUrl = null,
                        createdAt = 1763998135145,
                        emailVerified = true,
                        followedPostIds = emptyList(),
                        reportCount = 36,
                        status = UserStatus.REMINDED
                ),
                User(
                        uid = "xiIrXzEqLBRR6yAQTQCwxew3iga2",
                        email = "23120097@student.forumus.me",
                        fullName = "Tat Toan",
                        role = UserRole.STUDENT,
                        profilePictureUrl = null,
                        createdAt = 1764399442639,
                        emailVerified = true,
                        followedPostIds = emptyList(),
                        reportCount = 0,
                        status = UserStatus.NORMAL
                ),
                User(
                        uid = "Wg0Yx4jYbpOgmIvsAjS7neSc1RA2",
                        email = "23120143@student.forumus.me",
                        fullName = "Long To",
                        role = UserRole.STUDENT,
                        profilePictureUrl = null,
                        createdAt = 1764321845842,
                        emailVerified = true,
                        followedPostIds = emptyList(),
                        reportCount = 0,
                        status = UserStatus.NORMAL
                )
        )
    }

    /**
     * Sends a shared post to a recipient via the ChatRepository
     * @param recipientId The ID of the recipient user
     * @param postId The ID of the post being shared
     * @param chatRepository The ChatRepository instance to use for sending
     * @return Result with message ID on success, exception on failure
     */
    suspend fun sendShareMessage(
            recipientId: String,
            postId: String,
            chatRepository: ChatRepository = ChatRepository()
    ): Result<String> {
        return try {
            // Generate share URL
            val shareUrl = generateShareUrl(postId)

            Log.d(TAG, "Sending share message: postId=$postId, recipientId=$recipientId")

            // Get or create chat with recipient
            val chatItem = chatRepository.getOrCreateChat(recipientId)

            // Send the share URL as message content
            val result =
                    chatRepository.sendMessage(
                            chatId = chatItem.id,
                            content = shareUrl,
                            type = com.hcmus.forumus_client.data.model.MessageType.TEXT,
                            imageUrls = mutableListOf()
                    )

            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send share message", e)
            Result.failure(e)
        }
    }
}
