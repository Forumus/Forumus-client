package com.hcmus.forumus_client.ui.settings.guidelines

/**
 * Model for a guideline section with icon, title, and expandable content.
 */
data class GuidelineSection(
    val icon: Int,
    val title: String,
    val guidelines: List<String>,
    var isExpanded: Boolean = false
)
