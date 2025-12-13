package com.hcmus.forumus_client.data.model

data class User (
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val role: UserRole = UserRole.STUDENT,
    val profilePictureUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val emailVerified: Boolean = false,
    val followedPostIds: List<String> = emptyList(),
    val reportCount: Int = 0,
    val status: UserStatus = UserStatus.NORMAL,
    val fcmToken: String? = null
)

enum class UserRole {
    STUDENT,
    TEACHER,
    ADMIN
}

enum class UserStatus {
    NORMAL,
    REMINDED,
    WARNED,
    BANNED
}