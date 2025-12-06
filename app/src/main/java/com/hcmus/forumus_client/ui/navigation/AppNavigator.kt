package com.hcmus.forumus_client.ui.navigation

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.hcmus.forumus_client.ui.home.HomeActivity
import com.hcmus.forumus_client.ui.profile.ProfileActivity
import com.hcmus.forumus_client.ui.profile.ProfileActivity.Companion.EXTRA_MODE
import com.hcmus.forumus_client.ui.profile.ProfileActivity.Companion.EXTRA_USER_ID
import com.hcmus.forumus_client.ui.profile.ProfileMode

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

    fun openHome(clearStack: Boolean = false) {
        val intent = Intent(activity, HomeActivity::class.java)
        applyFlags(intent, clearStack)
        activity.startActivity(intent)
    }

    /**
     * Navigates to the Profile screen.
     *
     * @param userId the target user's ID to display
     * @param mode the profile mode to open (GENERAL, POSTS, REPLIES)
     * @param clearStack whether to reset the navigation stack
     */
    fun openProfile(
        userId: String,
        mode: ProfileMode = ProfileMode.GENERAL,
        clearStack: Boolean = false
    ) {
        Log.d("AppNavigator", "Opening profile for user: $userId")

        val intent = Intent(activity, ProfileActivity::class.java).apply {
            putExtra(EXTRA_USER_ID, userId)
            putExtra(EXTRA_MODE, mode.name)
        }

        applyFlags(intent, clearStack)
        activity.startActivity(intent)
    }

    /**
     * Opens the post detail screen.
     * TODO: implement when PostDetailActivity is ready.
     */
    fun onDetailPost(postId: String, clearStack: Boolean = false) {
        // TODO: Implement navigation to PostDetailActivity
    }

    fun openSearch(clearStack: Boolean = false) {
        // TODO: Implement search navigation
    }

    fun openCreatePost(clearStack: Boolean = false) {
        // TODO: Implement create post navigation
    }

    fun openAlerts(clearStack: Boolean = false) {
        // TODO: Implement alerts navigation
    }

    fun openChat(clearStack: Boolean = false) {
        // TODO: Implement chat navigation
    }

    fun finish() {
        activity.finish()
    }
}
