package com.hcmus.forumus_client.data.model

class Report (
    val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val nameViolation: String = "",
    val descriptionViolation: Violation
)
