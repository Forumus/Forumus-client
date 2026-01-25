package com.hcmus.forumus_client.ui.settings.guidelines

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.databinding.FragmentCommunityGuidelinesBinding

/**
 * Fragment displaying the community guidelines for the Forumus platform.
 * 
 * Shows expandable sections covering:
 * - Respectful behavior and civility
 * - Content posting guidelines
 * - Prohibited content
 * - Account security
 * - Intellectual property
 * - Moderation and consequences
 * - Reporting violations
 * - Privacy expectations
 */
class CommunityGuidelinesFragment : Fragment() {

    private lateinit var binding: FragmentCommunityGuidelinesBinding
    private val navController by lazy { findNavController() }
    private lateinit var guidelineAdapter: GuidelineAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCommunityGuidelinesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupBackButton()
        setupGuidelinesList()
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
     * Setup RecyclerView with guideline sections
     */
    private fun setupGuidelinesList() {
        val sections = getGuidelineSections()
        
        guidelineAdapter = GuidelineAdapter(sections)
        binding.rvGuidelines.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = guidelineAdapter
            setHasFixedSize(false)
        }
    }

    /**
     * Setup footer contact action
     */
    private fun setupFooterActions() {
        binding.tvContact.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Contact support - Coming Soon",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Get the list of guideline sections with content
     */
    private fun getGuidelineSections(): List<GuidelineSection> {
        return listOf(
            GuidelineSection(
                icon = R.drawable.ic_favorite,
                title = "Respectful Conduct and Civil Discourse",
                guidelines = listOf(
                    "All members are required to treat fellow participants with utmost respect, courtesy, and professionalism in all interactions within the platform",
                    "Harassment, intimidation, bullying, personal attacks, or any form of abusive behavior directed toward other members is strictly prohibited and will result in immediate action",
                    "When engaging in disagreements or debates, members must maintain a constructive and professional tone, focusing on ideas rather than individuals",
                    "The use of hate speech, discriminatory language, offensive slurs, or any form of communication that targets individuals based on race, ethnicity, gender, religion, sexual orientation, disability, or other protected characteristics is expressly forbidden",
                    "Members are expected to acknowledge and respect diverse perspectives, opinions, and backgrounds, fostering an inclusive environment conducive to academic growth and intellectual exchange",
                    "All communication should be conducted in a manner that upholds the dignity and academic integrity of the University of Science community"
                ),
                isExpanded = true // First section expanded by default
            ),
            GuidelineSection(
                icon = R.drawable.ic_article,
                title = "Content Submission Standards and Guidelines",
                guidelines = listOf(
                    "All posts and contributions must remain relevant to the designated topic category and adhere to the academic focus of the respective discussion area",
                    "Post titles must be clear, descriptive, and accurately reflect the content being shared, enabling efficient navigation and search functionality for all users",
                    "The posting of spam, unsolicited advertisements, excessive self-promotion, misleading clickbait, or repetitive content is strictly prohibited",
                    "Members are required to utilize appropriate topic tags and categories to ensure proper content organization and enhance discoverability for the community",
                    "Quality of contribution is prioritized over quantity; members are encouraged to thoughtfully compose their posts and comments before submission",
                    "Prior to creating a new discussion thread, members should utilize the search functionality to verify that similar topics have not already been addressed",
                    "All content should contribute meaningfully to academic discourse and align with the educational mission of the platform",
                    "Members must ensure that their contributions are well-researched, factually accurate, and presented in a clear, professional manner"
                )
            ),
            GuidelineSection(
                icon = R.drawable.ic_ban,
                title = "Prohibited Content and Activities",
                guidelines = listOf(
                    "The posting, sharing, or promotion of any illegal content, activities, or materials that violate local, national, or international laws is absolutely prohibited",
                    "Adult content, not safe for work (NSFW) materials, sexually explicit content, or any form of inappropriate material is strictly forbidden on this platform",
                    "Content depicting graphic violence, gore, disturbing imagery, or materials intended to shock or distress other members will not be tolerated",
                    "The sharing, soliciting, or distributing of personal identifying information (doxxing) of any individual, whether a member of this community or otherwise, is expressly prohibited",
                    "The deliberate spreading of misinformation, disinformation, false news, unverified claims, or conspiracy theories is not permitted",
                    "Plagiarism, academic dishonesty, cheating facilitation, sharing of examination materials, or any activities that compromise academic integrity are strictly forbidden and may result in reporting to university authorities",
                    "Content that promotes dangerous activities, self-harm, or poses a threat to the safety and well-being of community members will be removed immediately"
                )
            ),
            GuidelineSection(
                icon = R.drawable.ic_lock,
                title = "Account Security and Authentication Policies",
                guidelines = listOf(
                    "Each individual member is permitted to maintain only one active account on the platform; the creation of multiple accounts for the purpose of circumventing restrictions or manipulating content is prohibited",
                    "Impersonation of other individuals, including but not limited to students, faculty members, administrators, or public figures, is strictly forbidden",
                    "Members are responsible for maintaining the security and confidentiality of their login credentials, including passwords and authentication information",
                    "Any suspicious activity, unauthorized access attempts, security vulnerabilities, or potential breaches must be reported to the platform administrators immediately",
                    "The sharing of account credentials with other individuals or allowing unauthorized access to your account is not permitted",
                    "Members should utilize strong, unique passwords and enable all available security features to protect their accounts from unauthorized access"
                )
            ),
            GuidelineSection(
                icon = R.drawable.ic_copyright,
                title = "Intellectual Property Rights and Attribution",
                guidelines = listOf(
                    "All members must respect and adhere to copyright laws, intellectual property rights, and academic attribution standards when sharing content on the platform",
                    "Proper citation and attribution must be provided for all quoted material, referenced works, external sources, and intellectual contributions of others",
                    "Plagiarism in any form, including the unauthorized use of others' work without proper attribution, is strictly prohibited and constitutes a serious violation of academic integrity",
                    "Members may only post images, media files, documents, or other materials for which they possess appropriate rights, licenses, or permissions to share",
                    "When sharing copyrighted material, members must comply with fair use principles and applicable copyright exceptions for educational purposes",
                    "Original research, creative works, and intellectual contributions should be properly credited to their authors and creators",
                    "Any instances of copyright infringement or intellectual property violations should be reported to platform administrators for appropriate action"
                )
            ),
            GuidelineSection(
                icon = R.drawable.ic_gavel,
                title = "Moderation Procedures and Enforcement Actions",
                guidelines = listOf(
                    "Platform moderators and administrators reserve the right to review, edit, relocate, or remove any content that violates these community guidelines or disrupts the platform's academic mission",
                    "First-time violations of minor nature will typically result in an official warning and the removal of the offending content, with documentation maintained in the user's account record",
                    "Repeated violations of community guidelines or persistent disregard for platform rules will result in temporary account suspension, with duration determined by the severity and frequency of infractions",
                    "Serious violations, including but not limited to harassment, threats, doxxing, illegal content sharing, or severe academic dishonesty, may result in immediate and permanent account termination",
                    "Members who disagree with moderation decisions may submit formal appeals to the platform administration team for review and reconsideration",
                    "All enforcement actions are documented and may be shared with university administration in cases involving serious policy violations or potential threats to community safety",
                    "The moderation team operates independently and impartially, applying these guidelines consistently across all members regardless of status or affiliation"
                )
            ),
            GuidelineSection(
                icon = R.drawable.ic_flag,
                title = "Violation Reporting Mechanisms and Procedures",
                guidelines = listOf(
                    "Members who observe content or behavior that violates these community guidelines are encouraged to utilize the platform's reporting functionality to bring such matters to the attention of moderators",
                    "The reporting mechanism can be accessed via the report button or flag icon located on all posts, comments, and user profiles throughout the platform",
                    "When submitting a report, members should provide specific, detailed information regarding the nature of the violation, including relevant context and evidence when available",
                    "All reports are reviewed thoroughly by the moderation team in accordance with established procedures and timelines, with appropriate action taken based on the findings",
                    "The platform provides anonymous reporting options for members who wish to report violations without revealing their identity, ensuring protection for those who raise concerns",
                    "False or malicious reports submitted with the intent to harass other members or abuse the reporting system will be investigated and may result in disciplinary action against the reporting party",
                    "Members will receive confirmation when their reports are received and may be contacted if additional information is required for the investigation"
                )
            ),
            GuidelineSection(
                icon = R.drawable.ic_shield,
                title = "Privacy Expectations and Data Protection",
                guidelines = listOf(
                    "Members should be aware that profile information, including name, email address, role designation, and biography, is visible to other authenticated members of the platform",
                    "All posts, comments, and contributions made by members become part of the public record and are accessible to other community members indefinitely unless deleted",
                    "Private messaging functionality employs end-to-end encryption to ensure the confidentiality and security of direct communications between members",
                    "Members are strongly advised to review the platform's comprehensive Privacy Policy for detailed information regarding data collection, storage, processing, and protection practices",
                    "Profile visibility settings and privacy controls are available within account settings, allowing members to customize what information is shared with the broader community",
                    "The platform does not sell, share, or distribute personal member information to third parties without explicit consent, except as required by law or university policy",
                    "Members should exercise discretion when sharing personal information in public posts or discussions and are responsible for their own privacy management"
                )
            ),
            GuidelineSection(
                icon = R.drawable.ic_update,
                title = "Guideline Amendments and Policy Updates",
                guidelines = listOf(
                    "The platform administration reserves the right to modify, update, or amend these community guidelines at any time in response to evolving community needs, legal requirements, or operational considerations",
                    "All registered members will receive notification via email and in-platform announcements when significant changes to these guidelines are implemented",
                    "Continued use of the platform following the implementation of updated guidelines constitutes acceptance of and agreement to abide by the revised policies",
                    "Members are encouraged to review these community guidelines periodically to ensure familiarity with current standards and expectations",
                    "Questions, concerns, or suggestions regarding these guidelines may be directed to the platform administration team through official communication channels",
                    "The most current version of these guidelines will always be available through the Settings menu and will include the date of last revision for reference"
                )
            )
        )
    }
}
