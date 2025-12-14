package com.hcmus.forumus_client.ui.navigation

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.hcmus.forumus_client.ui.chats.ChatsActivity
import com.hcmus.forumus_client.ui.home.HomeActivity
import com.hcmus.forumus_client.ui.profile.ProfileActivity
import com.hcmus.forumus_client.ui.profile.ProfileActivity.Companion.EXTRA_MODE
import com.hcmus.forumus_client.ui.profile.ProfileActivity.Companion.EXTRA_USER_ID
import com.hcmus.forumus_client.ui.profile.ProfileMode
import com.hcmus.forumus_client.ui.post.detail.PostDetailActivity
import com.hcmus.forumus_client.ui.post.detail.PostDetailActivity.Companion.EXTRA_POST_ID
import com.hcmus.forumus_client.ui.post.create.CreatePostActivity
import com.hcmus.forumus_client.ui.settings.SettingsActivity

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
     * Navigates to the Post Detail screen for the given post ID.
     *
     * @param postId ID of the post to display
     * @param clearStack whether to reset the current navigation stack
     *
     * This method wraps the navigation logic to PostDetailActivity and ensures
     * consistent intent flag handling across the application.
     */
    fun onDetailPost(postId: String, clearStack: Boolean = false) {
        val intent = Intent(activity, PostDetailActivity::class.java).apply {
            putExtra(EXTRA_POST_ID, postId)
        }
        applyFlags(intent, clearStack)
        activity.startActivity(intent)
    }

    /**
     * Navigates to the Settings screen.
     * This method wraps the navigation logic to SettingsActivity and ensures
     * consistent intent flag handling across the application.
     */
    fun openSettings(clearStack: Boolean = false) {
        val intent = Intent(activity, SettingsActivity::class.java)
        applyFlags(intent, clearStack)
        activity.startActivity(intent)
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

    fun openChat(clearStack: Boolean = false) {
        val intent = Intent(activity, ChatsActivity::class.java)
        applyFlags(intent, clearStack)
        activity.startActivity(intent)
    }

    fun finish() {
        activity.finish()
    }
}
