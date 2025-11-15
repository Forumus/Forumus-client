package com.example.forumus.utils

object EmailConfig {
    // For Gmail SMTP
    const val SMTP_HOST = "smtp.gmail.com"
    const val SMTP_PORT = 465
    
    // Email credentials - In production, use BuildConfig or encrypted storage
    // For now, using placeholder values - you'll need to replace these
    const val EMAIL_FROM = "longto.xp@gmail.com"
    const val EMAIL_PASSWORD = "snftiqxpyagjnuos" // Use App Password, not regular password
    const val EMAIL_FROM_NAME = "Forumus App"
    
    // Email template settings
    const val SUBJECT_OTP = "Your Forumus Verification Code"
    
    /**
     * Generate HTML email template for OTP
     */
    fun getOTPEmailTemplate(otpCode: String, userEmail: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Forumus - Email Verification</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f8f9fa; padding: 30px; border-radius: 0 0 10px 10px; }
                    .otp-box { background: white; border: 2px dashed #667eea; padding: 20px; margin: 20px 0; text-align: center; border-radius: 10px; }
                    .otp-code { font-size: 36px; font-weight: bold; color: #667eea; letter-spacing: 8px; margin: 10px 0; }
                    .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #666; }
                    .warning { background: #fff3cd; border: 1px solid #ffeaa7; padding: 15px; border-radius: 5px; margin: 15px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎓 Forumus</h1>
                        <h2>Email Verification</h2>
                    </div>
                    <div class="content">
                        <p>Hello,</p>
                        <p>Thank you for registering with Forumus! To complete your registration, please verify your email address using the code below:</p>
                        
                        <div class="otp-box">
                            <p style="margin: 0; font-size: 14px;">Your verification code is:</p>
                            <div class="otp-code">$otpCode</div>
                            <p style="margin: 0; font-size: 12px; color: #666;">Enter this code in the app to continue</p>
                        </div>
                        
                        <div class="warning">
                            <p><strong>⚠️ Important:</strong></p>
                            <ul>
                                <li>This code will expire in <strong>5 minutes</strong></li>
                                <li>For security, do not share this code with anyone</li>
                                <li>If you didn't request this code, please ignore this email</li>
                            </ul>
                        </div>
                        
                        <p>If you have any questions or need help, feel free to contact our support team.</p>
                        
                        <p>Best regards,<br>
                        The Forumus Team</p>
                    </div>
                    <div class="footer">
                        <p>This is an automated message. Please do not reply to this email.</p>
                        <p>© 2025 Forumus. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
    
    /**
     * Generate plain text version for email clients that don't support HTML
     */
    fun getOTPEmailPlainText(otpCode: String): String {
        return """
            FORUMUS - EMAIL VERIFICATION
            
            Thank you for registering with Forumus!
            
            Your verification code is: $otpCode
            
            Please enter this code in the app to complete your registration.
            
            IMPORTANT:
            - This code will expire in 5 minutes
            - Do not share this code with anyone
            - If you didn't request this code, please ignore this email
            
            Best regards,
            The Forumus Team
            
            ---
            This is an automated message. Please do not reply to this email.
        """.trimIndent()
    }
}