package com.hcmus.forumus_client.utils

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
    fun createOTPEmailHTML(otpCode: String, recipientEmail: String): String {
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

    fun createWelcomeEmailHTML(userName: String): String {
        return """
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
    }
}