package com.hcmus.forumus_client.utils

object ApiConstants {
    // Use port 3000 for local development
    // Use port 8081 for Docker container

    // Local backend server
//    const val BASE_URL = "http://10.0.2.2:3000/"

    // AWS EC2 backend server
    const val BASE_URL = "http://3.105.149.245:8081/"
    const val SECRET_KEY = "Sdxb44tssZ0qAgrSE2EBO9geqxwLNaUA"
    
    // API Endpoints
    const val RESET_PASSWORD = "api/auth/resetPassword"
    const val GET_SUGGESTED_TOPICS = "api/posts/getSuggestedTopics"
    const val SEND_OTP_EMAIL = "api/email/send-otp"
    const val SEND_WELCOME_EMAIL = "api/email/send-welcome"
    const val NOTIFICATIONS_TRIGGER = "api/notifications"
    const val VALIDATE_POST = "api/posts/validatePost"
}