package com.hcmus.forumus_client.ui.settings.helpcenter

/**
 * Data model representing a help center topic/FAQ section.
 * 
 * @property icon Resource ID of the icon drawable
 * @property title Topic title displayed in the header
 * @property content Detailed explanation or answer for this topic
 * @property isExpanded Whether the section is currently expanded (default: false)
 */
data class HelpTopic(
    val title: String,
    val content: String,
    var isExpanded: Boolean = false
)
