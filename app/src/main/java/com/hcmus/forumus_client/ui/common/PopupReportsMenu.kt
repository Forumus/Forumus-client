package com.hcmus.forumus_client.ui.common

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.data.model.Violation

/**
 * A popup menu for displaying violation/report options.
 * Shows a list of violations that users can select to report a post.
 * Allows toggling violation description visibility.
 */
class PopupReportsMenu(
    private val activity: AppCompatActivity
) {
    // Callback when a violation is selected
    var onViolationSelected: ((Violation) -> Unit)? = null

    // List of violation options
    private val violations: List<Violation>
        get() = listOf(
            Violation(
                id = "vio_001",
                name = activity.getString(R.string.violation_scam_name),
                description = activity.getString(R.string.violation_scam_desc)
            ),
            Violation(
                id = "vio_002",
                name = activity.getString(R.string.violation_violence_name),
                description = activity.getString(R.string.violation_violence_desc)
            ),
            Violation(
                id = "vio_003",
                name = activity.getString(R.string.violation_inappropriate_name),
                description = activity.getString(R.string.violation_inappropriate_desc)
            ),
            Violation(
                id = "vio_004",
                name = activity.getString(R.string.violation_misinfo_name),
                description = activity.getString(R.string.violation_misinfo_desc)
            ),
            Violation(
                id = "vio_005",
                name = activity.getString(R.string.violation_illegal_name),
                description = activity.getString(R.string.violation_illegal_desc)
            ),
            Violation(
                id = "vio_006",
                name = activity.getString(R.string.violation_harassment_name),
                description = activity.getString(R.string.violation_harassment_desc)
            ),
            Violation(
                id = "vio_007",
                name = activity.getString(R.string.violation_privacy_name),
                description = activity.getString(R.string.violation_privacy_desc)
            ),
            Violation(
                id = "vio_008",
                name = activity.getString(R.string.violation_copyright_name),
                description = activity.getString(R.string.violation_copyright_desc)
            )
        )

    /**
     * Displays the reports menu at the specified anchor view position.
     *
     * @param anchor The view to anchor the popup menu to
     */
    fun show(anchor: View) {
        // Inflate the popup menu layout from XML
        val inflater = LayoutInflater.from(activity)
        val popupView = inflater.inflate(R.layout.popup_reports_menu, null)

        // Create the popup window
        val popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            setBackgroundDrawable(activity.getDrawable(R.drawable.popup_menu_background))
            elevation = 8f
        }

        // Get the violation list container
        val violationListContainer = popupView.findViewById<LinearLayout>(R.id.layout_violation_list)

        // Inflate and add each violation item to the list
        violations.forEach { violation ->
            val violationItemView = inflater.inflate(R.layout.item_violation, violationListContainer, false)

            // Set violation name
            violationItemView.findViewById<TextView>(R.id.tv_violation_name).text = violation.name

            // Set violation description
            val descriptionView = violationItemView.findViewById<TextView>(R.id.tv_violation_description)
            descriptionView.text = violation.description

            // Get detail link and violation item container
            val detailLink = violationItemView.findViewById<TextView>(R.id.tv_detail)
            val violationItem = violationItemView.findViewById<LinearLayout>(R.id.layout_violation_item)

            // Setup detail link to toggle description visibility
            detailLink.setOnClickListener {
                val isVisible = descriptionView.visibility == View.VISIBLE
                descriptionView.visibility = if (isVisible) View.GONE else View.VISIBLE
            }

            // Setup violation item click to select this violation
            violationItem.setOnClickListener {
                onViolationSelected?.invoke(violation)
                popupWindow.dismiss()
            }

            violationListContainer.addView(violationItemView)
        }

        // Display popup at anchor position
        popupWindow.showAsDropDown(anchor, 0, 8)
    }
}
