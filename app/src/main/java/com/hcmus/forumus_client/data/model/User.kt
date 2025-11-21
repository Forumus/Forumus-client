package com.hcmus.forumus_client.data.model

data class User (
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val role: UserRole = UserRole.STUDENT,
    val profilePictureUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val emailVerified: Boolean = false
)

enum class UserRole {
    STUDENT,
    TEACHER,
    ADMIN
}