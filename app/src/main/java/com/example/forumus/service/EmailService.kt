package com.example.forumus.service

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
            // Validate email format
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(recipientEmail).matches()) {
                return@withContext Resource.Error("Invalid email address format")
            }
            
            // Create email session
            val session = createEmailSession()
            
            // Create message
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(EmailConfig.EMAIL_FROM, EmailConfig.EMAIL_FROM_NAME))
                addRecipient(Message.RecipientType.TO, InternetAddress(recipientEmail))
                subject = EmailConfig.SUBJECT_OTP
                
                // Create multipart message (HTML + plain text)
                val multipart = MimeMultipart("alternative")
                
                // Plain text part
                val textPart = MimeBodyPart().apply {
                    setContent(EmailConfig.getOTPEmailPlainText(otpCode), "text/plain; charset=utf-8")
                }
                
                // HTML part
                val htmlPart = MimeBodyPart().apply {
                    setContent(
                        EmailConfig.getOTPEmailTemplate(otpCode, recipientEmail), 
                        "text/html; charset=utf-8"
                    )
                }
                
                multipart.addBodyPart(textPart)
                multipart.addBodyPart(htmlPart)
                setContent(multipart)
            }
            
            // Send email
            Transport.send(message)
            
            Resource.Success(true)
            
        } catch (e: AuthenticationFailedException) {
            Resource.Error("Email authentication failed. Please check email credentials.")
        } catch (e: MessagingException) {
            Resource.Error("Failed to send email: ${e.message}")
        } catch (e: Exception) {
            Resource.Error("Unexpected error occurred: ${e.message}")
        }
    }
    
    /**
     * Create email session with SMTP configuration
     */
    private fun createEmailSession(): Session {
        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", EmailConfig.SMTP_HOST)
            put("mail.smtp.port", EmailConfig.SMTP_PORT.toString())
            put("mail.smtp.ssl.protocols", "TLSv1.2")
            
            // Additional security settings
            put("mail.smtp.ssl.trust", EmailConfig.SMTP_HOST)
            put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
            put("mail.smtp.socketFactory.fallback", "false")
        }
        
        return Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(EmailConfig.EMAIL_FROM, EmailConfig.EMAIL_PASSWORD)
            }
        })
    }
    
    /**
     * Test email configuration without sending actual email
     */
    suspend fun testEmailConfiguration(): Resource<Boolean> = withContext(Dispatchers.IO) {
        try {
            val session = createEmailSession()
            
            // Try to connect to SMTP server
            val transport = session.getTransport("smtp")
            transport.connect(
                EmailConfig.SMTP_HOST,
                EmailConfig.SMTP_PORT,
                EmailConfig.EMAIL_FROM,
                EmailConfig.EMAIL_PASSWORD
            )
            transport.close()
            
            Resource.Success(true)
            
        } catch (e: AuthenticationFailedException) {
            Resource.Error("Email authentication failed. Please check your email credentials.")
        } catch (e: MessagingException) {
            Resource.Error("SMTP connection failed: ${e.message}")
        } catch (e: Exception) {
            Resource.Error("Email configuration test failed: ${e.message}")
        }
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