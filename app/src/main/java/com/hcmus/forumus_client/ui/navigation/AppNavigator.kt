package com.hcmus.forumus_client.ui.navigation

import android.app.Activity
import android.content.Intent
import android.util.Log
//import com.hcmus.forumus_client.ui.profile.ProfileActivity.Companion.EXTRA_USER_ID
import com.hcmus.forumus_client.ui.profile.ProfileMode
//import com.hcmus.forumus_client.ui.post.detail.PostDetailActivity.Companion.EXTRA_POST_ID

class AppNavigator(private val activity: Activity) {

    /** Applies intent flags based on navigation mode. */
    private fun applyFlags(intent: Intent, clearStack: Boolean) {
        if (clearStack) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
    }

    fun finish() {
        activity.finish()
    }
}
