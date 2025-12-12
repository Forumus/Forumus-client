package com.hcmus.forumus_client.ui.navigation

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.hcmus.forumus_client.ui.chats.ChatsActivity
import com.hcmus.forumus_client.ui.post.create.CreatePostActivity
//import com.hcmus.forumus_client.ui.profile.ProfileActivity.Companion.EXTRA_USER_ID
import com.hcmus.forumus_client.ui.profile.ProfileMode
//import com.hcmus.forumus_client.ui.post.detail.PostDetailActivity.Companion.EXTRA_POST_ID

class AppNavigator(private val activity: Activity) {

    /**
     * Applies appropriate intent flags based on navigation mode.
     *
     * @param clearStack:
     *  - true  → clears the existing task and starts a new one
     *             (used for flows like login/logout or resetting navigation)
     *  - false → keeps the current back stack but prevents creating duplicate
     *             activity instances (via CLEAR_TOP | SINGLE_TOP)
     */
    private fun applyFlags(intent: Intent, clearStack: Boolean) {
        if (clearStack) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
    }

    fun openSearch(clearStack: Boolean = false) {
        // TODO: Implement search navigation
    }

    fun openCreatePost(clearStack: Boolean = false) {
        val intent = Intent(activity, CreatePostActivity::class.java)
        applyFlags(intent, clearStack)
        activity.startActivity(intent)
    }

    fun openAlerts(clearStack: Boolean = false) {
        // TODO: Implement alerts navigation
    }

    fun finish() {
        activity.finish()
    }
}
