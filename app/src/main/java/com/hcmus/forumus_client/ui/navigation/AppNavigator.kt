package com.hcmus.forumus_client.ui.navigation

import android.app.Activity
import android.content.Intent
import com.hcmus.forumus_client.ui.home.HomeActivity

class AppNavigator(private val activity: Activity) {

    /**
     * Helper: áp dụng flag cho intent
     * - clearStack = true  -> xoá task cũ, mở task mới (dùng cho login/logout, reset flow)
     * - clearStack = false -> giữ stack, nhưng tránh tạo nhiều instance trùng (CLEAR_TOP | SINGLE_TOP)
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

    fun openProfile(userId: String, clearStack: Boolean = false) {

    }

    /**
     * Mở màn chi tiết bài post
     */
    fun onDetailPost(postId: String, clearStack: Boolean = false) {

    }

    fun openSearch(clearStack: Boolean = false) {

    }

    fun openCreatePost(clearStack: Boolean = false) {

    }

    fun openAlerts(clearStack: Boolean = false) {

    }

    fun openChat(clearStack: Boolean = false) {

    }

    fun finish() {
        activity.finish()
    }
}
