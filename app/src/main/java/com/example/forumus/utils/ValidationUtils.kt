package com.example.forumus.utils

import android.util.Patterns

object ValidationUtils {

    /**
     * Validates if the full name is valid
     * @param fullName The full name to validate
     * @return true if valid, false otherwise
     */
    fun isValidFullName(fullName: String): Boolean {
        return fullName.trim().length >= 2 && fullName.trim().contains(" ")
    }

    /**
     * Validates if the email address is in correct format
     * @param email The email to validate
     * @return true if valid, false otherwise
     */
    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Validates if the email is a student/university email
     * This is a simple check for university domains - can be extended
     * @param email The email to validate
     * @return true if valid university email, false otherwise
     */
    fun isValidStudentEmail(email: String): Boolean {
        if (!isValidEmail(email)) return false
        
        // Common university email domains
        val universityDomains = listOf(
            "edu", "ac.uk", "edu.au", "edu.sg", "edu.my", 
            "student.university.edu", "university.edu",
            "gmail.com", "outlook.com", "hotmail.com" // Allow common domains for demo
        )
        
        val domain = email.substringAfter("@").lowercase()
        return universityDomains.any { domain.endsWith(it) }
    }

    /**
     * Validates if the password meets minimum requirements
     * @param password The password to validate
     * @return true if valid, false otherwise
     */
    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    /**
     * Validates if the password meets strong requirements
     * @param password The password to validate
     * @return true if strong password, false otherwise
     */
    fun isStrongPassword(password: String): Boolean {
        if (password.length < 8) return false
        
        val hasUpperCase = password.any { it.isUpperCase() }
        val hasLowerCase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }
        
        return hasUpperCase && hasLowerCase && hasDigit && hasSpecialChar
    }

    /**
     * Validates if the phone number is in correct format
     * @param phoneNumber The phone number to validate
     * @return true if valid, false otherwise
     */
    fun isValidPhoneNumber(phoneNumber: String): Boolean {
        return phoneNumber.isNotBlank() && 
               phoneNumber.replace(" ", "").replace("+", "").replace("-", "").all { it.isDigit() } &&
               phoneNumber.length >= 10
    }
}