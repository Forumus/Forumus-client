package com.hcmus.forumus_client.ui.settings.helpcenter

/** Model for a help topic. */
data class HelpTopic(
    val title: String,
    val content: String,
    var isExpanded: Boolean = false
)
