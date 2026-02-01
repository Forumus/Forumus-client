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

/** Displays community guidelines in expandable sections. */
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

    private fun setupBackButton() {
        binding.ibBack.setOnClickListener {
            navController.popBackStack()
        }
    }

    private fun setupGuidelinesList() {
        val sections = getGuidelineSections()
        
        guidelineAdapter = GuidelineAdapter(sections)
        binding.rvGuidelines.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = guidelineAdapter
            setHasFixedSize(false)
        }
    }

    private fun setupFooterActions() {
        // Set last updated text
        binding.tvLastUpdated.text = getString(R.string.footer_last_updated, "January 2026")

        binding.tvContact.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.toast_contact_support_soon),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun getGuidelineSections(): List<GuidelineSection> {
        return listOf(
            GuidelineSection(
                icon = R.drawable.ic_favorite,
                title = getString(R.string.guideline_respect_title),
                guidelines = listOf(
                    getString(R.string.guideline_respect_1),
                    getString(R.string.guideline_respect_2),
                    getString(R.string.guideline_respect_3),
                    getString(R.string.guideline_respect_4),
                    getString(R.string.guideline_respect_5),
                    getString(R.string.guideline_respect_6)
                ),
                isExpanded = true // First section expanded by default
            ),
            GuidelineSection(
                icon = R.drawable.ic_article,
                title = getString(R.string.guideline_content_title),
                guidelines = listOf(
                    getString(R.string.guideline_content_1),
                    getString(R.string.guideline_content_2),
                    getString(R.string.guideline_content_3),
                    getString(R.string.guideline_content_4),
                    getString(R.string.guideline_content_5),
                    getString(R.string.guideline_content_6),
                    getString(R.string.guideline_content_7),
                    getString(R.string.guideline_content_8)
                )
            ),
            GuidelineSection(
                icon = R.drawable.ic_ban,
                title = getString(R.string.guideline_prohibited_title),
                guidelines = listOf(
                    getString(R.string.guideline_prohibited_1),
                    getString(R.string.guideline_prohibited_2),
                    getString(R.string.guideline_prohibited_3),
                    getString(R.string.guideline_prohibited_4),
                    getString(R.string.guideline_prohibited_5),
                    getString(R.string.guideline_prohibited_6),
                    getString(R.string.guideline_prohibited_7)
                )
            ),
            GuidelineSection(
                icon = R.drawable.ic_lock,
                title = getString(R.string.guideline_security_title),
                guidelines = listOf(
                    getString(R.string.guideline_security_1),
                    getString(R.string.guideline_security_2),
                    getString(R.string.guideline_security_3),
                    getString(R.string.guideline_security_4),
                    getString(R.string.guideline_security_5),
                    getString(R.string.guideline_security_6)
                )
            ),
            GuidelineSection(
                icon = R.drawable.ic_copyright,
                title = getString(R.string.guideline_ip_title),
                guidelines = listOf(
                    getString(R.string.guideline_ip_1),
                    getString(R.string.guideline_ip_2),
                    getString(R.string.guideline_ip_3),
                    getString(R.string.guideline_ip_4),
                    getString(R.string.guideline_ip_5),
                    getString(R.string.guideline_ip_6),
                    getString(R.string.guideline_ip_7)
                )
            ),
            GuidelineSection(
                icon = R.drawable.ic_gavel,
                title = getString(R.string.guideline_moderation_title),
                guidelines = listOf(
                    getString(R.string.guideline_moderation_1),
                    getString(R.string.guideline_moderation_2),
                    getString(R.string.guideline_moderation_3),
                    getString(R.string.guideline_moderation_4),
                    getString(R.string.guideline_moderation_5),
                    getString(R.string.guideline_moderation_6),
                    getString(R.string.guideline_moderation_7)
                )
            ),
            GuidelineSection(
                icon = R.drawable.ic_flag,
                title = getString(R.string.guideline_reporting_title),
                guidelines = listOf(
                    getString(R.string.guideline_reporting_1),
                    getString(R.string.guideline_reporting_2),
                    getString(R.string.guideline_reporting_3),
                    getString(R.string.guideline_reporting_4),
                    getString(R.string.guideline_reporting_5),
                    getString(R.string.guideline_reporting_6),
                    getString(R.string.guideline_reporting_7)
                )
            ),
            GuidelineSection(
                icon = R.drawable.ic_shield,
                title = getString(R.string.guideline_privacy_title),
                guidelines = listOf(
                    getString(R.string.guideline_privacy_1),
                    getString(R.string.guideline_privacy_2),
                    getString(R.string.guideline_privacy_3),
                    getString(R.string.guideline_privacy_4),
                    getString(R.string.guideline_privacy_5),
                    getString(R.string.guideline_privacy_6),
                    getString(R.string.guideline_privacy_7)
                )
            ),
            GuidelineSection(
                icon = R.drawable.ic_update,
                title = getString(R.string.guideline_updates_title),
                guidelines = listOf(
                    getString(R.string.guideline_updates_1),
                    getString(R.string.guideline_updates_2),
                    getString(R.string.guideline_updates_3),
                    getString(R.string.guideline_updates_4),
                    getString(R.string.guideline_updates_5),
                    getString(R.string.guideline_updates_6)
                )
            )
        )
    }
}
