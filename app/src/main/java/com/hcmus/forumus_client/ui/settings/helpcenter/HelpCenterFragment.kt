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

/** Help center with FAQs and support options. */
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

    private fun setupBackButton() {
        binding.ibBack.setOnClickListener {
            navController.popBackStack()
        }
    }

    private fun setupQuickActions() {
        // Contact support button
        binding.cvContactSupport.setOnClickListener {
            openEmailClient("support@forumus.edu.vn", getString(R.string.email_subject_support))
        }

        // Report issue button
        binding.cvReportIssue.setOnClickListener {
            openEmailClient("support@forumus.edu.vn", getString(R.string.email_subject_issue))
        }
    }

    private fun setupHelpTopics() {
        val topics = getHelpTopics()
        
        helpTopicAdapter = HelpTopicAdapter(topics)
        binding.rvHelpTopics.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = helpTopicAdapter
            setHasFixedSize(false)
        }
    }

    private fun setupFooterActions() {
        binding.tvEmailSupport.setOnClickListener {
            openEmailClient("support@forumus.edu.vn", getString(R.string.email_subject_general))
        }
    }

    // Opens email app with pre-filled recipient and subject
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
                    getString(R.string.error_no_email_app, email),
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                getString(R.string.error_email_client),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun getHelpTopics(): List<HelpTopic> {
        return listOf(
            HelpTopic(
                title = getString(R.string.help_topic_account_title),
                content = getString(R.string.help_topic_account_content),
                isExpanded = true
            ),
            HelpTopic(
                title = getString(R.string.help_topic_post_title),
                content = getString(R.string.help_topic_post_content)
            ),
            HelpTopic(
                title = getString(R.string.help_topic_comment_title),
                content = getString(R.string.help_topic_comment_content)
            ),
            HelpTopic(
                title = getString(R.string.help_topic_voting_title),
                content = getString(R.string.help_topic_voting_content)
            ),
            HelpTopic(
                title = getString(R.string.help_topic_notif_title),
                content = getString(R.string.help_topic_notif_content)
            ),
            HelpTopic(
                title = getString(R.string.help_topic_save_title),
                content = getString(R.string.help_topic_save_content)
            ),
            HelpTopic(
                title = getString(R.string.help_topic_search_title),
                content = getString(R.string.help_topic_search_content)
            ),
            HelpTopic(
                title = getString(R.string.help_topic_edit_profile_title),
                content = getString(R.string.help_topic_edit_profile_content)
            ),
            HelpTopic(
                title = getString(R.string.help_topic_message_title),
                content = getString(R.string.help_topic_message_content)
            ),
            HelpTopic(
                title = getString(R.string.help_topic_report_title),
                content = getString(R.string.help_topic_report_content)
            ),
            HelpTopic(
                title = getString(R.string.help_topic_reset_pass_title),
                content = getString(R.string.help_topic_reset_pass_content)
            ),
            HelpTopic(
                title = getString(R.string.help_topic_dark_mode_title),
                content = getString(R.string.help_topic_dark_mode_content)
            ),
            HelpTopic(
                title = getString(R.string.help_topic_categories_title),
                content = getString(R.string.help_topic_categories_content)
            ),
            HelpTopic(
                title = getString(R.string.help_topic_violation_title),
                content = getString(R.string.help_topic_violation_content)
            ),
            HelpTopic(
                title = getString(R.string.help_topic_about_title),
                content = getString(R.string.help_topic_about_content)
            )
        )
    }
}
