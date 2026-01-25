package com.hcmus.forumus_client.ui.settings.helpcenter

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.databinding.FragmentHelpCenterBinding
import androidx.core.net.toUri

/**
 * Fragment displaying the help center with FAQs and support options.
 * 
 * Features:
 * - Frequently asked questions with expandable answers
 * - Quick action buttons for contact support and reporting issues
 * - Email support contact information
 * - Comprehensive help topics covering platform usage
 */
class HelpCenterFragment : Fragment() {

    private lateinit var binding: FragmentHelpCenterBinding
    private val navController by lazy { findNavController() }
    private lateinit var helpTopicAdapter: HelpTopicAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHelpCenterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupBackButton()
        setupQuickActions()
        setupHelpTopics()
        setupFooterActions()
    }

    /**
     * Setup back button to return to settings screen
     */
    private fun setupBackButton() {
        binding.ibBack.setOnClickListener {
            navController.popBackStack()
        }
    }

    /**
     * Setup quick action buttons for support and reporting
     */
    private fun setupQuickActions() {
        // Contact support button
        binding.cvContactSupport.setOnClickListener {
            openEmailClient("support@forumus.edu.vn", "Support Request")
        }

        // Report issue button
        binding.cvReportIssue.setOnClickListener {
            openEmailClient("support@forumus.edu.vn", "Issue Report")
        }
    }

    /**
     * Setup RecyclerView with help topics/FAQs
     */
    private fun setupHelpTopics() {
        val topics = getHelpTopics()
        
        helpTopicAdapter = HelpTopicAdapter(topics)
        binding.rvHelpTopics.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = helpTopicAdapter
            setHasFixedSize(false)
        }
    }

    /**
     * Setup footer email support link
     */
    private fun setupFooterActions() {
        binding.tvEmailSupport.setOnClickListener {
            openEmailClient("support@forumus.edu.vn", "Help Request")
        }
    }

    /**
     * Open email client with pre-filled recipient and subject
     */
    private fun openEmailClient(email: String, subject: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = "mailto:".toUri()
                putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                putExtra(Intent.EXTRA_SUBJECT, subject)
            }
            
            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(
                    requireContext(),
                    "No email app found. Please email: $email",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Error opening email client",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Get the list of help topics with detailed answers
     */
    private fun getHelpTopics(): List<HelpTopic> {
        return listOf(
            HelpTopic(
                title = "How do I create an account?",
                content = "To create a new account on Forumus, navigate to the registration page and provide your full name, valid university email address, phone number, and a secure password. You will receive a 6-digit verification code via email that must be entered to complete the registration process. After verification, you will select your role (Student or Teacher) and gain full access to the platform. Only users with valid University of Science email addresses are permitted to register.",
                isExpanded = true
            ),
            HelpTopic(
                title = "How do I create a post or discussion?",
                content = "To create a new post, tap the floating action button (+ icon) located at the bottom of the home screen. Select the appropriate topic category for your discussion, compose a clear and descriptive title, write your detailed question or content in the body section, and optionally attach images or documents to support your post. You may also add relevant topic tags to improve discoverability. Review your post for accuracy and clarity before tapping the 'Post' button to publish it to the community. Your post will immediately become visible to other members in the selected category."
            ),
            HelpTopic(
                title = "How do I comment on or reply to posts?",
                content = "To participate in a discussion, open any post by tapping on it from the feed. Scroll to the bottom of the post content where you will find the comment input field. Type your response, answer, or contribution in the text box. You may format your text and attach supporting materials if necessary. Tap the 'Send' or 'Comment' button to publish your response. Your comment will appear in the discussion thread and the original poster will receive a notification. You can also reply to specific comments by tapping the reply button beneath any existing comment, creating threaded discussions."
            ),
            HelpTopic(
                title = "What is the voting system and how does it work?",
                content = "The voting system allows community members to evaluate the quality and relevance of posts and comments. Users can upvote content they find helpful, informative, or well-written by tapping the upward arrow icon. Conversely, users may downvote content that is inaccurate, unhelpful, or violates community standards by tapping the downward arrow. The total vote score (upvotes minus downvotes) is displayed next to each post and comment. Highly upvoted content rises to greater visibility, while downvoted content may be minimized. This democratic system helps surface the most valuable contributions and maintains content quality across the platform."
            ),
            HelpTopic(
                title = "How do I manage my notifications?",
                content = "Notification preferences can be customized in the Settings menu accessible from your profile. Navigate to Settings and locate the notification section where you will find toggle switches for push notifications and email notifications. Enable push notifications to receive real-time alerts on your device when someone replies to your posts, comments on your contributions, mentions you in discussions, or when important platform announcements are made. Enable email notifications to receive periodic summaries or immediate alerts via your registered email address. You may disable either or both notification types at any time based on your preferences."
            ),
            HelpTopic(
                title = "How do I save posts for later reference?",
                content = "To save a post for future reference, open the post you wish to bookmark and locate the save icon (bookmark symbol) typically found in the top-right corner or within the post options menu. Tap this icon to add the post to your saved collection. The icon will change appearance to indicate the post has been saved. All saved posts can be accessed later through the 'Saved Posts' section in your Settings menu. This feature allows you to create a personal library of valuable discussions, important announcements, useful tutorials, or any content you wish to revisit. Saved posts remain accessible until you manually unsave them."
            ),
            HelpTopic(
                title = "How do I search for specific topics or discussions?",
                content = "The search functionality is accessible via the search icon in the top navigation bar or the dedicated Search tab in the bottom navigation menu. Tap the search field and enter keywords, topic names, user names, or specific phrases related to the content you seek. The search algorithm will scan post titles, content body, comments, tags, and user profiles to return relevant results. You may filter search results by category, date range, post type, or vote score using the filter options. Advanced search features allow you to search within specific topic categories, find posts by particular users, or locate discussions from specific time periods."
            ),
            HelpTopic(
                title = "How do I edit my profile information?",
                content = "To modify your profile details, navigate to the Settings screen from the home menu and select 'Edit Profile.' You will be able to update your profile picture by uploading a new image, modify your display name, update your biography or personal description, change your academic role designation, and adjust your profile visibility settings. After making your desired changes, tap the 'Save' button to apply the updates. Note that certain information such as your email address may require additional verification if changed. Your profile changes will be immediately visible to other community members unless you have restricted profile visibility in your privacy settings."
            ),
            HelpTopic(
                title = "How do I send private messages to other users?",
                content = "To initiate a private conversation with another member, navigate to their profile by tapping their username from any post or comment. On their profile page, locate and tap the 'Message' or envelope icon to open a private chat. Type your message in the input field at the bottom of the conversation screen and tap send. All private messages are end-to-end encrypted to ensure confidentiality. You can access all your ongoing conversations through the 'Chats' tab in the bottom navigation menu. Message notifications can be customized in your notification settings to alert you of new messages."
            ),
            HelpTopic(
                title = "How do I report inappropriate content or behavior?",
                content = "If you encounter content or user behavior that violates community guidelines, you can report it by tapping the three-dot menu (⋮) on any post or comment and selecting 'Report.' Choose the most appropriate violation category from the list provided (e.g., harassment, spam, misinformation, inappropriate content). You may optionally provide additional context or details to help moderators understand the issue. Your report will be submitted anonymously to the moderation team for review. The team will investigate the matter and take appropriate action according to platform policies. You will receive confirmation that your report was received, and serious violations will be addressed promptly."
            ),
            HelpTopic(
                title = "How do I reset my password or recover my account?",
                content = "If you have forgotten your password or are unable to access your account, navigate to the login screen and tap the 'Forgot Password?' link located below the password input field. Enter your registered email address and tap 'Submit.' You will receive an email containing a 6-digit verification code. Enter this code on the verification screen to confirm your identity. After successful verification, you will be prompted to create a new password. Ensure your new password meets the security requirements (minimum length, complexity). Once set, you can immediately log in using your new credentials. If you do not receive the verification email, check your spam folder or request a new code."
            ),
            HelpTopic(
                title = "How do I enable dark mode?",
                content = "Dark mode provides a darker color scheme that is easier on the eyes in low-light conditions and may help conserve battery life on devices with OLED screens. To enable dark mode, navigate to the Settings screen from the main menu and locate the 'Dark Mode' toggle switch in the preferences section. Tap the switch to enable dark mode, and the interface will immediately transition to the dark theme. All colors, backgrounds, and text will adjust to the darker palette while maintaining readability and visual hierarchy. You can disable dark mode at any time by toggling the same switch. Your dark mode preference is saved and will persist across app sessions."
            ),
            HelpTopic(
                title = "What are topic categories and how do I use them?",
                content = "Topic categories are organizational structures that group related discussions together, making it easier for users to find relevant content. When creating a new post, you must select the most appropriate category (e.g., Biology, Computer Science, Mathematics, Campus Events). This categorization helps other users discover your post when browsing specific subjects. You can explore posts within particular categories by using the category filter on the home screen or search page. Each category may have specific guidelines or posting requirements, so review the category description before posting. Proper categorization is essential for maintaining an organized and navigable community."
            ),
            HelpTopic(
                title = "What happens if I violate community guidelines?",
                content = "Violations of community guidelines are taken seriously and result in progressive enforcement actions. For minor first-time violations, you will receive an official warning, and the offending content will be removed. The warning will be documented in your account record. Repeated violations or disregard for platform rules will result in temporary account suspension, with the duration determined by the severity and frequency of infractions. During suspension, you will be unable to post, comment, or interact with the community, though you may still view content. Serious violations such as harassment, threats, sharing illegal content, or severe academic dishonesty may result in immediate permanent account termination. All enforcement decisions can be appealed to the moderation team by contacting support."
            ),
            HelpTopic(
                title = "What is Forumus and who can use it?",
                content = "Forumus is an internal academic discussion platform developed exclusively for members of the University of Science – Vietnam National University Ho Chi Minh City. The platform is designed to provide a secure, professional, and collaborative environment where students, faculty members, lecturers, and academic staff can engage in meaningful discussions, share knowledge, ask questions, and support each other's learning journey. Access to Forumus requires a valid university email address and is restricted to verified members of the university community. The platform facilitates academic exchange across all disciplines and departments while maintaining high standards of discourse and respect."
            )
        )
    }
}
