package com.hcmus.forumus_client.ui.home

import android.content.res.ColorStateList
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
// Import the OnBackPressedCallback
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
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
        postAdapter = PostAdapter()
        postsRecycler.layoutManager = LinearLayoutManager(this)
        postsRecycler.adapter = postAdapter
        viewModel.posts.observe(this) { list ->
            postAdapter.submitList(list)
        }
    }

    private fun configureEdgeToEdge() {
        // Edge-to-edge with only top inset applied.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val root = findViewById<View>(R.id.drawer_layout)
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            topAppBar.setPadding(
                topAppBar.paddingLeft,
                sysBars.top,
                topAppBar.paddingRight,
                topAppBar.paddingBottom
            )
            insets
        }
    }

    private fun applyInactiveStyle(container: View) {
        container.setBackgroundResource(0)
        if (container is ViewGroup && container.childCount >= 2) {
            val icon = container.getChildAt(0) as? ImageView
            val label = container.getChildAt(1) as? TextView
            // Clear any special backgrounds from home icon
            icon?.background = null
            val inactive = ContextCompat.getColor(this, R.color.text_tertiary)
            icon?.imageTintList = ColorStateList.valueOf(inactive)
            label?.setTextColor(inactive)
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
            icon?.imageTintList = ColorStateList.valueOf(blue)
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

    // You can now safely remove the old onBackPressed method.
    /*
    override fun onBackPressed() {
        if (viewModel.menuVisible.value == true) {
            viewModel.hideMenu()
        } else {
            super.onBackPressed()
        }
    }
    */
}
