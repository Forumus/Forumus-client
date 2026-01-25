package com.hcmus.forumus_client.ui.settings.guidelines

/**
 * Data model representing a community guideline section.
 * 
 * @property icon Resource ID of the icon drawable
 * @property title Section title displayed in the header
 * @property guidelines List of guideline points for this section
 * @property isExpanded Whether the section is currently expanded (default: false)
 */
data class GuidelineSection(
    val icon: Int,
    val title: String,
    val guidelines: List<String>,
    var isExpanded: Boolean = false
)
