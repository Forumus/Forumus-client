package com.hcmus.forumus_client.ui.home

import android.graphics.Rect
import android.view.MotionEvent
import com.hcmus.forumus_client.data.model.Post
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

enum class NavTab { HOME, EXPLORE, ALERTS, CHAT }

class MainViewModel : ViewModel() {

	private val _menuVisible = MutableLiveData(false)
	val menuVisible: LiveData<Boolean> = _menuVisible

	private val _activeTab = MutableLiveData(NavTab.HOME)
	val activeTab: LiveData<NavTab> = _activeTab

	private val _posts = MutableLiveData<List<Post>>(emptyList())
	val posts: LiveData<List<Post>> = _posts

	fun onProfileIconClicked() {
		_menuVisible.value = !(_menuVisible.value ?: false)
	}

	fun hideMenu() { _menuVisible.value = false }

	fun onTabSelected(tab: NavTab) {
		if (_activeTab.value != tab) {
			_activeTab.value = tab
		}
	}

	fun loadSamplePosts() {
		if (_posts.value.isNullOrEmpty()) {
			_posts.value = listOf(
				Post(
					id = "1",
					communityName = "c/Sports_Athletics",
					communityIconLetter = "S",
					timePosted = "1h",
					title = "Intramural Sports Week - Highlights from Day 1!",
					content = "Amazing start to sports week with soccer, basketball, tennis and more.",
					voteCount = 156,
					commentCount = 43,
					imageUrls = emptyList()
				),
				Post(
					id = "2",
					communityName = "c/Tech_Club",
					communityIconLetter = "T",
					timePosted = "3h",
					title = "Hackathon kicks off tomorrow",
					content = "Don't forget to register and bring your energy! Prizes for top 3 teams.",
					voteCount = 89,
					commentCount = 12,
					imageUrls = emptyList()
				),
				Post(
					id = "3",
					communityName = "c/Campus_Life",
					communityIconLetter = "C",
					timePosted = "5h",
					title = "Study spots recommendation thread",
					content = "Share your favorite quiet or collaborative spaces around campus.",
					voteCount = 42,
					commentCount = 18,
					imageUrls = emptyList()
				)
			)
		}
	}

	/**
	 * Handle a raw touch event. The Activity passes global Rects for the popup and profile button.
	 * If ACTION_DOWN occurs outside both while the menu is visible, hide the menu.
	 */
	fun onTouch(action: Int, rawX: Float, rawY: Float, menuRect: Rect?, profileRect: Rect?) {
		if (action == MotionEvent.ACTION_DOWN && _menuVisible.value == true) {
			val x = rawX.toInt(); val y = rawY.toInt()
			val insideMenu = menuRect?.contains(x, y) == true
			val insideProfile = profileRect?.contains(x, y) == true
			if (!insideMenu && !insideProfile) {
				_menuVisible.value = false
			}
		}
	}
}