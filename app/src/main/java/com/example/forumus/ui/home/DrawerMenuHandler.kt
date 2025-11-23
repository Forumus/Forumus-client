package com.example.forumus.ui.home

import android.view.MenuItem
import android.view.View
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.ui.NavigationUI
import com.google.android.material.navigation.NavigationView

class DrawerMenuHandler(
    private val drawerLayout: DrawerLayout,
    private val navigationView: NavigationView
) {

    init {
        setupDrawerMenu()
    }

    private fun setupDrawerMenu() {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            handleMenuItemClick(menuItem)
            drawerLayout.closeDrawers()
            true
        }
    }

    private fun handleMenuItemClick(menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.menu_computer_science -> {
                // Filter posts by Computer Science
                onCategorySelected("Computer Science")
            }
            R.id.menu_mathematics -> {
                onCategorySelected("Mathematics")
            }
            R.id.menu_engineering -> {
                onCategorySelected("Engineering")
            }
            R.id.menu_sciences -> {
                onCategorySelected("Sciences")
            }
            R.id.menu_business_school -> {
                onCategorySelected("Business School")
            }
            R.id.menu_arts_humanities -> {
                onCategorySelected("Arts & Humanities")
            }
            R.id.menu_social_sciences -> {
                onCategorySelected("Social Sciences")
            }
            R.id.menu_study_groups -> {
                onCategorySelected("Study Groups")
            }
            R.id.menu_campus_events -> {
                onCategorySelected("Campus Events")
            }
            R.id.menu_career_services -> {
                onCategorySelected("Career Services")
            }
            R.id.menu_library_resources -> {
                onCategorySelected("Library & Resources")
            }
            R.id.menu_residence_life -> {
                onCategorySelected("Residence Life")
            }
            R.id.menu_financial_aid -> {
                onCategorySelected("Financial Aid")
            }
        }
    }

    private fun onCategorySelected(category: String) {
        // Emit event or callback to update the feed
        // This will be connected to ViewModel
    }

    fun setupSortOptions(view: View) {
        view.findViewById<View>(R.id.menu_sort_new)?.setOnClickListener {
            onSortSelected("new")
        }
        view.findViewById<View>(R.id.menu_sort_trending)?.setOnClickListener {
            onSortSelected("trending")
        }
    }

    private fun onSortSelected(sortType: String) {
        // Emit sort event to ViewModel
    }

    fun closeDrawer() {
        drawerLayout.closeDrawers()
    }
}
