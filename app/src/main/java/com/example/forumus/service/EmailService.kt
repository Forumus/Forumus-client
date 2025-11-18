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
            val otpHtml = createOTPEmailHTML(otpCode, recipientEmail)

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
     * Create OTP email HTML content
     */
    private fun createOTPEmailHTML(otpCode: String, recipientEmail: String): String {
        return """
        <!DOCTYPE html>
        <html>
          <body style="font-family: Arial, sans-serif; background:#ffffff; padding:20px;">
            <table width="100%" cellpadding="0" cellspacing="0" style="max-width: 600px; margin:auto; border:1px solid #e0e0e0; border-radius:8px;">
              <tr>
                <td style="background:#4a64d8; padding:20px; text-align:center; color:white; font-size:22px; border-radius:8px 8px 0 0;">
                  Forumus Email Verification
                </td>
              </tr>
              <tr>
                <td style="padding:25px; color:#333; font-size:15px;">
                  <p style="margin:0 0 12px 0;">Hi $recipientEmail,</p>
                  <p style="margin:0 0 12px 0;">Use the verification code below to continue:</p>
                  <p style="font-size:32px; margin:25px 0; text-align:center; font-weight:bold; color:#4a64d8;">
                    $otpCode
                  </p>
                  <p style="margin:0 0 12px 0;">This code expires in 5 minutes.</p>
                  <p style="margin:0;">If you didn’t request this code, you can ignore this email.</p>
                </td>
              </tr>
              <tr>
                <td style="background:#f5f5f5; padding:15px; text-align:center; font-size:13px; color:#666; border-radius:0 0 8px 8px;">
                  Forumus – Learning Community
                </td>
              </tr>
            </table>
          </body>
        </html>
        """.trimIndent()
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

            val welcomeHtml = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                        .content { background: #f8f9fa; padding: 30px; border-radius: 0 0 10px 10px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🎉 Welcome to Forumus!</h1>
                        </div>
                        <div class="content">
                            <p>Hi $userName,</p>
                            <p>Congratulations! Your email has been successfully verified and your Forumus account is now active.</p>
                            <p>You can now:</p>
                            <ul>
                                <li>Join discussions and forums</li>
                                <li>Ask questions and get answers</li>
                                <li>Connect with students and teachers</li>
                                <li>Share your knowledge with the community</li>
                            </ul>
                            <p>Welcome to the Forumus community!</p>
                            <p>Best regards,<br>The Forumus Team</p>
                        </div>
                    </div>
                </body>
                </html>
            """.trimIndent()

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