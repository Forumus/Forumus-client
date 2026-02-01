package com.hcmus.forumus_client.utils

import android.util.Patterns

object ValidationUtils {

    /** Validates if the full name is valid. */
    fun isValidFullName(fullName: String): Boolean {
        return fullName.trim().length >= 2 && fullName.trim().contains(" ")
    }

    /** Validates if the email is in correct format. */
    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /** Validates if the email is a student/university email. */
    fun isValidStudentEmail(email: String): Boolean {
        if (!isValidEmail(email)) return false
        
        // Common university email domains
        val universityDomains = listOf(
            "student.forumus.me", "student.hcmus.edu.vn",
            "gmail.com", "outlook.com"
        )
        
        val domain = email.substringAfter("@").lowercase()
        return universityDomains.any { domain.endsWith(it) }
    }

    /** Validates if the password meets minimum requirements. */
    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
                && password.length <= 32
                && !password.contains(" ")
                && password.all { it.isLetterOrDigit() || "!@#$%^&*()-_=+[]{}|;:'\",.<>?/`~".contains(it) }
    }

    /** Validates if the password meets strong requirements. */
    fun isStrongPassword(password: String): Boolean {
        if (password.length < 8) return false
        
        val hasUpperCase = password.any { it.isUpperCase() }
        val hasLowerCase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }
        
        return hasUpperCase && hasLowerCase && hasDigit && hasSpecialChar
    }

    /** Validates if the phone number is in correct format. */
    fun isValidPhoneNumber(phoneNumber: String): Boolean {
        return phoneNumber.isNotBlank() && 
               phoneNumber.replace(" ", "").replace("+", "").replace("-", "").all { it.isDigit() } &&
               phoneNumber.length >= 10
    }
}