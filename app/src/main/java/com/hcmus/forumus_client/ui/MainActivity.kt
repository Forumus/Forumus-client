package com.hcmus.forumus_client.ui

import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import android.util.Log
import androidx.databinding.DataBindingUtil
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.android.material.appbar.AppBarLayout
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.data.local.TokenManager
import com.hcmus.forumus_client.data.repository.AuthRepository
import com.hcmus.forumus_client.databinding.ActivityMainBinding
import com.hcmus.forumus_client.ui.fragments.HomeFragment
import com.hcmus.forumus_client.ui.fragments.ChatsFragment
import com.hcmus.forumus_client.ui.fragments.NavbarFragment
import com.hcmus.forumus_client.ui.home.MainViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams
import androidx.core.view.isVisible

class MainActivity : AppCompatActivity(), NavbarFragment.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var profileButton: View
    private lateinit var userMenuContainer: View
    private lateinit var viewProfileItem: View
    private lateinit var editProfileItem: View
    private lateinit var darkModeItem: View
    private lateinit var settingsItem: View

    private lateinit var topAppBar: View
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var bottomNavContainer: View
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var hamburgerButton: View
    private lateinit var navView: NavigationView

    private val viewModel: MainViewModel by viewModels()

    private lateinit var authRepository: AuthRepository
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        bindViews()
        setupObservers()
        attachClickListeners()
        setupNavbarFragment()
        configureEdgeToEdge()

        authRepository = AuthRepository(
            FirebaseAuth.getInstance(),
            FirebaseFirestore.getInstance(),
            context = this
        )
        tokenManager = TokenManager(this)

        logSessionInfo()

        // Load home fragment by default
        if (savedInstanceState == null) {
            loadHomeFragment()
        }

        // Handle back press
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.menuVisible.value == true) {
                    viewModel.hideMenu()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private fun showAppBar() {
        appBarLayout.visibility = View.VISIBLE
        
        // Set the fragment container behavior to respect the AppBar
        val fragmentContainer = binding.fragmentContainer
        val layoutParams = fragmentContainer.layoutParams as LayoutParams
        layoutParams.behavior = AppBarLayout.ScrollingViewBehavior()
        fragmentContainer.layoutParams = layoutParams
    }

    private fun hideAppBar() {
        appBarLayout.visibility = View.GONE
        
        // Remove the behavior so fragment container fills the entire space
        val fragmentContainer = binding.fragmentContainer
        val layoutParams = fragmentContainer.layoutParams as LayoutParams
        layoutParams.behavior = null
        fragmentContainer.layoutParams = layoutParams
    }

    override fun onNavigationItemSelected(item: String) {
        when (item) {
            "home" -> loadHomeFragment()
            "explore" -> {
                // TODO: Load explore fragment
            }
            "alerts" -> {
                // TODO: Load alerts fragment
            }
            "chat" -> loadChatsFragment()
        }
    }

    private fun loadHomeFragment() {
        showAppBar()

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, HomeFragment())
            .commit()
    }

    private fun loadChatsFragment() {
        hideAppBar()

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, ChatsFragment())
            .commit()
    }
    
    private fun logSessionInfo() {
        if (tokenManager.hasValidSession()) {
            val user = authRepository.getCurrentUserFromSession()
            val remainingTime = authRepository.getRemainingSessionTime()
            val hoursRemaining = remainingTime / (60 * 60 * 1000)
            
            Log.d("MainActivity", "Current user: ${user?.email}")
            Log.d("MainActivity", "Session remaining: $hoursRemaining hours")
        } else {
            Log.d("MainActivity", "No saved session - user didn't check Remember Me")
        }
    }
    
    override fun onResume() {
        super.onResume()
        // No session validity check - users can access app regardless of Remember Me choice
    }

    private fun bindViews() {
        profileButton = binding.btnProfile
        userMenuContainer = binding.userMenuContainer
        viewProfileItem = binding.menuViewProfile
        editProfileItem = binding.menuEditProfile
        darkModeItem = binding.menuDarkMode
        settingsItem = binding.menuSettings

        topAppBar = binding.topAppBar
        appBarLayout = binding.appBarLayout
        bottomNavContainer = binding.bottomNavContainer
        drawerLayout = binding.drawerLayout
        hamburgerButton = binding.btnMenu
        navView = binding.navView
    }

    private fun setupObservers() {
        viewModel.menuVisible.observe(this) { visible ->
            userMenuContainer.visibility = if (visible) View.VISIBLE else View.GONE
        }
        viewModel.drawerOpen.observe(this) { open ->
            if (open) {
                if (!drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.openDrawer(GravityCompat.START)
            } else {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START)
            }
        }
    }

    private fun attachClickListeners() {
        profileButton.setOnClickListener { viewModel.onProfileIconClicked() }

        viewProfileItem.setOnClickListener { viewModel.hideMenu() }
        editProfileItem.setOnClickListener { viewModel.hideMenu() }
        darkModeItem.setOnClickListener { viewModel.hideMenu() }
        settingsItem.setOnClickListener { viewModel.hideMenu() }

        hamburgerButton.setOnClickListener { viewModel.toggleDrawer() }

        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) { /* no-op */ }
            override fun onDrawerOpened(drawerView: View) { viewModel.setDrawerOpen(true) }
            override fun onDrawerClosed(drawerView: View) { viewModel.setDrawerOpen(false) }
            override fun onDrawerStateChanged(newState: Int) { /* no-op */ }
        })
    }

    private fun setupNavbarFragment() {
        if (supportFragmentManager.findFragmentById(R.id.bottom_nav_container) == null) {
            val navbarFragment = NavbarFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.bottom_nav_container, navbarFragment)
                .commit()
        }
    }

    private fun configureEdgeToEdge() {
//        WindowCompat.setDecorFitsSystemWindows(window, false)
        val coordinatorRoot = binding.coordinatorRoot
        ViewCompat.setOnApplyWindowInsetsListener(coordinatorRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            topAppBar.layoutParams?.let { lp ->
                val fixed = (70 * resources.displayMetrics.density).toInt()
                if (lp.height != fixed) {
                    lp.height = fixed
                    topAppBar.layoutParams = lp
                }
            }
            if (appBarLayout.paddingTop != 0) {
                appBarLayout.setPadding(appBarLayout.paddingLeft, 0, appBarLayout.paddingRight, appBarLayout.paddingBottom)
            }
            if (userMenuContainer.isVisible) positionUserMenuBelowProfile()
            insets
        }
        ViewCompat.requestApplyInsets(coordinatorRoot)
        
        ViewCompat.setOnApplyWindowInsetsListener(navView) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout())
            if (v.paddingTop != sys.top) {
                v.setPadding(v.paddingLeft, sys.top, v.paddingRight, v.paddingBottom)
            }
            insets
        }
        ViewCompat.requestApplyInsets(navView)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val menuRect = Rect().takeIf { userMenuContainer.isVisible }?.apply { userMenuContainer.getGlobalVisibleRect(this) }
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