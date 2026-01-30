package com.hcmus.forumus_client.ui.common

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.data.model.Violation

class PopupPostMenu(
    private val activity: AppCompatActivity
) {
    var onSaveClick: (() -> Unit)? = null
    var onReportClick: ((Violation) -> Unit)? = null
    var saveButtonText: String = activity.getString(R.string.save)

    private var reportsMenu: PopupReportsMenu? = null

    fun show(anchor: View) {
        val inflater = LayoutInflater.from(activity)
        val popupView = inflater.inflate(R.layout.popup_post_menu, null)

        val popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            setBackgroundDrawable(activity.getDrawable(R.drawable.popup_menu_background))
            elevation = 8f
        }

        val saveButton = popupView.findViewById<LinearLayout>(R.id.btnSave)
        val saveTextView = saveButton.findViewById<android.widget.TextView>(R.id.tvSaveText)
        saveTextView?.text = saveButtonText
        
        saveButton.setOnClickListener {
            onSaveClick?.invoke()
            popupWindow.dismiss()
        }

        popupView.findViewById<LinearLayout>(R.id.btnReport).setOnClickListener {
            showReportsMenu(anchor, popupWindow)
        }

        popupWindow.showAsDropDown(anchor, 0, 8)
    }

    private fun showReportsMenu(anchor: View, parentPopup: PopupWindow) {
        if (reportsMenu == null) {
            reportsMenu = PopupReportsMenu(activity)
            reportsMenu?.onViolationSelected = { violation ->
                onReportClick?.invoke(violation)
                parentPopup.dismiss()
            }
        }

        reportsMenu?.show(anchor)
    }
}
