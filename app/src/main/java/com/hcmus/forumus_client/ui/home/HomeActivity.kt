package com.hcmus.forumus_client.ui.home

import android.content.res.ColorStateList
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
// Import the OnBackPressedCallback
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.doOnLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.hcmus.forumus_client.R

class HomeActivity : AppCompatActivity() {

    private lateinit var profileButton: View
    private lateinit var userMenuContainer: View
    private lateinit var viewProfileItem: View
    private lateinit var editProfileItem: View
    private lateinit var darkModeItem: View
    private lateinit var settingsItem: View

    private lateinit var navHome: View
    private lateinit var navExplore: View
    private lateinit var navAlerts: View
    private lateinit var navChat: View
    private lateinit var postsRecycler: androidx.recyclerview.widget.RecyclerView
    private lateinit var postAdapter: PostAdapter
    private lateinit var topAppBar: View
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var bottomNavContainer: View

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        bindViews()
        setupObservers()
        attachClickListeners()
        setupRecycler()
        viewModel.loadSamplePosts()
        configureEdgeToEdge()

        // --- NEW CODE STARTS HERE ---

        // 1. Create a new OnBackPressedCallback
        val onBackPressedCallback = object : OnBackPressedCallback(true) { // 'true' means the callback is enabled
            override fun handleOnBackPressed() {
                // 2. Put your custom back press logic here
                if (viewModel.menuVisible.value == true) {
                    viewModel.hideMenu()
                } else {
                    // 3. If you want to perform the default back action (e.g., finish the activity),
                    // you need to disable this callback and call onBackPressed again.
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }

        // 4. Add the callback to the dispatcher
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        // --- NEW CODE ENDS HERE ---
    }

    private fun bindViews() {
        profileButton = findViewById(R.id.btn_profile)
        userMenuContainer = findViewById(R.id.user_menu_container)
        viewProfileItem = findViewById(R.id.menu_view_profile)
        editProfileItem = findViewById(R.id.menu_edit_profile)
        darkModeItem = findViewById(R.id.menu_dark_mode)
        settingsItem = findViewById(R.id.menu_settings)

        navHome = findViewById(R.id.nav_home)
        navExplore = findViewById(R.id.nav_explore)
        navAlerts = findViewById(R.id.nav_alerts)
        navChat = findViewById(R.id.nav_chat)
        postsRecycler = findViewById(R.id.rv_posts)
        topAppBar = findViewById(R.id.top_app_bar)
        appBarLayout = findViewById(R.id.app_bar_layout)
        bottomNavContainer = findViewById(R.id.bottom_nav_container)
    }

    private fun setupObservers() {
        viewModel.menuVisible.observe(this) { visible ->
            userMenuContainer.visibility = if (visible) View.VISIBLE else View.GONE
        }
        viewModel.activeTab.observe(this) { tab ->
            val tabMap = mapOf(
                NavTab.HOME to navHome,
                NavTab.EXPLORE to navExplore,
                NavTab.ALERTS to navAlerts,
                NavTab.CHAT to navChat
            )
            // Reset all then style active
            tabMap.values.forEach { applyInactiveStyle(it) }
            tabMap[tab]?.let { applyActiveStyle(it) }
        }
    }

    private fun attachClickListeners() {
        profileButton.setOnClickListener { viewModel.onProfileIconClicked() }

        viewProfileItem.setOnClickListener { viewModel.hideMenu() }
        editProfileItem.setOnClickListener { viewModel.hideMenu() }
        darkModeItem.setOnClickListener { viewModel.hideMenu() }
        settingsItem.setOnClickListener { viewModel.hideMenu() }

        navHome.setOnClickListener { viewModel.onTabSelected(NavTab.HOME) }
        navExplore.setOnClickListener { viewModel.onTabSelected(NavTab.EXPLORE) }
        navAlerts.setOnClickListener { viewModel.onTabSelected(NavTab.ALERTS) }
        navChat.setOnClickListener { viewModel.onTabSelected(NavTab.CHAT) }
    }

    private fun setupRecycler() {
        postAdapter = PostAdapter(object : PostAdapter.PostInteractionListener {
            override fun onUpvote(post: com.hcmus.forumus_client.data.model.Post) {
                viewModel.onUpvote(post.id)
            }
            override fun onDownvote(post: com.hcmus.forumus_client.data.model.Post) {
                viewModel.onDownvote(post.id)
            }
            override fun onComments(post: com.hcmus.forumus_client.data.model.Post) { /* TODO */ }
            override fun onShare(post: com.hcmus.forumus_client.data.model.Post) { /* TODO */ }
            override fun onPostClicked(post: com.hcmus.forumus_client.data.model.Post) { /* TODO */ }
        })
        postsRecycler.layoutManager = LinearLayoutManager(this)
        postsRecycler.adapter = postAdapter
        viewModel.posts.observe(this) { list -> postAdapter.submitList(list) }

        // Ensure bottom nav height available before using
        bottomNavContainer.doOnLayout {
            appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { layout, verticalOffset ->
                val total = layout.totalScrollRange.takeIf { it > 0 } ?: return@OnOffsetChangedListener
                val progress = (-verticalOffset).coerceAtLeast(0).toFloat() / total.toFloat() // 0 -> 1
                // Sync bottom nav translation with progress
                bottomNavContainer.translationY = progress * bottomNavContainer.height
                // Bars considered hidden only when almost fully collapsed; lower threshold enables quicker reappear
                val fullyHidden = progress >= 0.95f
                viewModel.setBarsHidden(fullyHidden)
                // Reposition popup menu during scroll if visible (handles slow drags/micro-holds)
                if (userMenuContainer.visibility == View.VISIBLE) {
                    positionUserMenuBelowProfile()
                }
            })
            // Recycler scroll listener for immediate bar reveal on upward scroll while collapsed
            postsRecycler.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                    if (dy < 0 && viewModel.barsHidden.value == true) {
                        // User scrolling up: expand AppBar and show bottom bar immediately
                        appBarLayout.setExpanded(true, true)
                        bottomNavContainer.animate().translationY(0f).setDuration(180).start()
                        viewModel.setBarsHidden(false)
                    }
                }
            })
        }
    }

    private fun configureEdgeToEdge() {
        // Enable edge-to-edge but manually reserve the status bar / notch by padding the coordinator root.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val coordinatorRoot = findViewById<View>(R.id.coordinator_root)
        ViewCompat.setOnApplyWindowInsetsListener(coordinatorRoot) { _, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout())
            // Apply top inset as padding so no content or the app bar overlaps the notch area.
            if (coordinatorRoot.paddingTop != sys.top) {
                coordinatorRoot.setPadding(
                    coordinatorRoot.paddingLeft,
                    sys.top,
                    coordinatorRoot.paddingRight,
                    coordinatorRoot.paddingBottom
                )
            }
            // Keep toolbar at fixed height (already set in XML) – do not inflate into the notch.
            (topAppBar.layoutParams as? ViewGroup.LayoutParams)?.let { lp ->
                val fixed = (70 * resources.displayMetrics.density).toInt()
                if (lp.height != fixed) {
                    lp.height = fixed
                    topAppBar.layoutParams = lp
                }
            }
            // Ensure AppBarLayout has no stray padding that could cause visual jump.
            if (appBarLayout.paddingTop != 0) {
                appBarLayout.setPadding(appBarLayout.paddingLeft, 0, appBarLayout.paddingRight, appBarLayout.paddingBottom)
            }
            if (userMenuContainer.visibility == View.VISIBLE) positionUserMenuBelowProfile()
            insets
        }
        ViewCompat.requestApplyInsets(coordinatorRoot)
        // Decide status bar icon color based on top bar (white) vs light/dark background.
        val bgColor = (topAppBar.background as? ColorDrawable)?.color
            ?: ContextCompat.getColor(this, R.color.background_light)
        val isLightBackground = ColorUtils.calculateLuminance(bgColor) > 0.5
        WindowInsetsControllerCompat(window, coordinatorRoot).isAppearanceLightStatusBars = isLightBackground
    }

    private fun applyInactiveStyle(container: View) {
        container.setBackgroundResource(0)
        if (container is ViewGroup && container.childCount >= 2) {
            val icon = container.getChildAt(0) as? ImageView
            val label = container.getChildAt(1) as? TextView
            // Clear any special backgrounds from home icon
            icon?.background = null
            val inactiveColor = ContextCompat.getColor(this, R.color.text_tertiary)
            when (container) {
                navHome -> icon?.setImageResource(R.drawable.ic_home)
                navExplore -> icon?.setImageResource(R.drawable.ic_explore)
                navAlerts -> icon?.setImageResource(R.drawable.ic_notification)
                navChat -> icon?.setImageResource(R.drawable.ic_chat)
            }
            icon?.imageTintList = ColorStateList.valueOf(inactiveColor)
            label?.setTextColor(inactiveColor)
        }
    }

    private fun applyActiveStyle(container: View) {
        val blue = ContextCompat.getColor(this, R.color.link_color)
        // All tabs (including Home): slightly darken container; icon and label turn blue
        container.setBackgroundResource(R.drawable.nav_item_active_dim)
        if (container is ViewGroup && container.childCount >= 2) {
            val icon = container.getChildAt(0) as? ImageView
            val label = container.getChildAt(1) as? TextView
            icon?.background = null
            when (container) {
                navHome -> {
                    icon?.setImageResource(R.drawable.ic_home_filled)
                    icon?.imageTintList = null
                }
                navExplore -> {
                    icon?.setImageResource(R.drawable.ic_explore_filled)
                    icon?.imageTintList = null
                }
                navAlerts -> {
                    icon?.setImageResource(R.drawable.ic_notification_filled)
                    icon?.imageTintList = null
                }
                navChat -> {
                    icon?.setImageResource(R.drawable.ic_chat_filled)
                    icon?.imageTintList = null
                }
            }
            // Other non-icon changes
            label?.setTextColor(blue)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // Provide global rects to ViewModel for decision-making
        val menuRect = Rect().takeIf { userMenuContainer.visibility == View.VISIBLE }?.apply { userMenuContainer.getGlobalVisibleRect(this) }
        val profileRect = Rect().apply { profileButton.getGlobalVisibleRect(this) }
        viewModel.onTouch(ev.action, ev.rawX, ev.rawY, menuRect, profileRect)
        return super.dispatchTouchEvent(ev)
    }

    private fun positionUserMenuBelowProfile() {
        val parentView = userMenuContainer.parent as? View ?: return
        if (userMenuContainer.measuredWidth == 0) {
            userMenuContainer.post { positionUserMenuBelowProfile() }
            return
        }
        val parentLoc = IntArray(2)
        val profileLoc = IntArray(2)
        parentView.getLocationOnScreen(parentLoc)
        profileButton.getLocationOnScreen(profileLoc)
        val profileRight = profileLoc[0] - parentLoc[0] + profileButton.width
        val profileBottom = profileLoc[1] - parentLoc[1] + profileButton.height
        val menuWidth = userMenuContainer.measuredWidth
        val menuHeight = userMenuContainer.measuredHeight
        var desiredX = (profileRight - menuWidth).toFloat().coerceAtLeast(0f)
        val maxX = (parentView.width - menuWidth).toFloat()
        if (desiredX > maxX) desiredX = maxX
        var desiredY = profileBottom.toFloat()
        val maxY = (parentView.height - menuHeight).toFloat()
        if (desiredY > maxY) desiredY = maxY
        userMenuContainer.x = desiredX
        userMenuContainer.y = desiredY
    }

}
