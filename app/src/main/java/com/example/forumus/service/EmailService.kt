package com.example.forumus.service

import android.util.Log
import com.example.forumus.utils.EmailConfig
import com.example.forumus.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

class EmailService {

    /**
     * Send OTP email to user
     */
    suspend fun sendOTPEmail(
        recipientEmail: String,
        otpCode: String
    ): Resource<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d("EmailService", "Starting OTP email send to: $recipientEmail")

            // Validate email format
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(recipientEmail).matches()) {
                Log.e("EmailService", "Invalid email format: $recipientEmail")
                return@withContext Resource.Error("Invalid email address format")
            }

            // Create email session
            val session = createEmailSession()

            val multipart = MimeMultipart("alternative")

            // Create simple HTML content (similar to working welcome email)
            val otpHtml = EmailConfig.createOTPEmailHTML(otpCode, recipientEmail)

            val textPart = MimeBodyPart()
            textPart.setText("Your Forumus verification code is: $otpCode")

            val htmlPart = MimeBodyPart()
            htmlPart.setContent(otpHtml, "text/html; charset=utf-8")

            multipart.addBodyPart(textPart)
            multipart.addBodyPart(htmlPart)

            // Create message - simplified like the working welcome email
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(EmailConfig.EMAIL_FROM, EmailConfig.EMAIL_FROM_NAME))
                addRecipient(Message.RecipientType.TO, InternetAddress(recipientEmail))
                subject = "Your Forumus Verification Code"
                setContent(otpHtml, "text/html; charset=utf-8")
            }

            message.setContent(multipart)

            // Send email
            Transport.send(message)
            Log.d("EmailService", "Email sent successfully to $recipientEmail!")

            Resource.Success(true)

        } catch (e: AuthenticationFailedException) {
            Log.e("EmailService", "AuthenticationFailedException: ${e.message}", e)
            Resource.Error("Email authentication failed. Please check email credentials.")
        } catch (e: MessagingException) {
            Log.e("EmailService", "MessagingException: ${e.message}", e)
            Resource.Error("Failed to send email: ${e.message}")
        } catch (e: Exception) {
            Log.e("EmailService", "Exception: ${e.message}", e)
            Resource.Error("Unexpected error occurred: ${e.message}")
        }
    }


    /**
     * Create email session with SMTP configuration
     */
    private fun createEmailSession(): Session {
        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.ssl.enable", "true")
            put("mail.smtp.host", EmailConfig.SMTP_HOST)
            put("mail.smtp.port", EmailConfig.SMTP_PORT.toString())
            put("mail.smtp.ssl.protocols", "TLSv1.2")
        }

        return Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(EmailConfig.EMAIL_FROM, EmailConfig.EMAIL_PASSWORD)
            }
        })
    }

    /**
     * Send welcome email after successful verification (optional)
     */
    suspend fun sendWelcomeEmail(
        recipientEmail: String,
        userName: String
    ): Resource<Boolean> = withContext(Dispatchers.IO) {
        try {
            val session = createEmailSession()

            val welcomeHtml = EmailConfig.createWelcomeEmailHTML(userName)


            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(EmailConfig.EMAIL_FROM, EmailConfig.EMAIL_FROM_NAME))
                addRecipient(Message.RecipientType.TO, InternetAddress(recipientEmail))
                subject = "Welcome to Forumus! 🎉"
                setContent(welcomeHtml, "text/html; charset=utf-8")
            }

            Transport.send(message)
            Resource.Success(true)

        } catch (e: Exception) {
            Resource.Error("Failed to send welcome email: ${e.message}")
        }
    }
}