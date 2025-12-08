package com.hcmus.forumus_client.ui.common

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.data.model.Violation

/**
 * A popup menu for post-related actions.
 * Provides click callbacks for save and report actions.
 * Contains PopupReportsMenu for report violation selection.
 */
class PopupPostMenu(
    private val activity: AppCompatActivity
) {
    // Callback for when user taps "Save" option
    var onSaveClick: (() -> Unit)? = null
    // Callback for when user selects a violation from reports menu
    var onReportClick: ((Violation) -> Unit)? = null

    // Inner popup menu for violations
    private var reportsMenu: PopupReportsMenu? = null

    /**
     * Displays the popup menu at the specified anchor view position.
     * Sets up click listeners for save and report buttons.
     *
     * @param anchor The view to anchor the popup menu to (typically the post menu button)
     */
    fun show(anchor: View) {
        // Inflate the popup menu layout from XML
        val inflater = LayoutInflater.from(activity)
        val popupView = inflater.inflate(R.layout.popup_post_menu, null)

        // Create the popup window with wrap content dimensions and focusable enabled
        val popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            // Apply popup styling: background drawable and elevation shadow
            setBackgroundDrawable(activity.getDrawable(R.drawable.popup_menu_background))
            elevation = 8f
        }

        // Setup click listener for Save button
        popupView.findViewById<LinearLayout>(R.id.btnSave).setOnClickListener {
            onSaveClick?.invoke()
            popupWindow.dismiss()
        }

        // Setup click listener for Report button
        popupView.findViewById<LinearLayout>(R.id.btnReport).setOnClickListener {
            // Show reports menu when report is clicked
            showReportsMenu(anchor, popupWindow)
        }

        // Display popup below the anchor view with 8dp vertical offset
        popupWindow.showAsDropDown(anchor, 0, 8)
    }

    /**
     * Displays the reports menu for violation selection.
     * Called when user clicks on the Report button.
     *
     * @param anchor The view to anchor the reports menu to
     * @param parentPopup The parent popup menu to dismiss after selection
     */
    private fun showReportsMenu(anchor: View, parentPopup: PopupWindow) {
        // Create reports menu if not already created
        if (reportsMenu == null) {
            reportsMenu = PopupReportsMenu(activity)
            reportsMenu?.onViolationSelected = { violation ->
                onReportClick?.invoke(violation)
                parentPopup.dismiss()
            }
        }

        // Show the reports menu at the same anchor position
        reportsMenu?.show(anchor)
    }
}
