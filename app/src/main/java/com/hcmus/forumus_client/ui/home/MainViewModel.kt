package com.hcmus.forumus_client.ui.home

import android.graphics.Rect
import android.view.MotionEvent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hcmus.forumus_client.data.model.Post

enum class NavTab { HOME, EXPLORE, ALERTS, CHAT }

class MainViewModel : ViewModel() {

	private val _menuVisible = MutableLiveData(false)
	val menuVisible: LiveData<Boolean> = _menuVisible

	private val _activeTab = MutableLiveData(NavTab.HOME)
	val activeTab: LiveData<NavTab> = _activeTab

	private val _posts = MutableLiveData<List<Post>>(emptyList())
	val posts: LiveData<List<Post>> = _posts

	private val _barsHidden = MutableLiveData(false)
	val barsHidden: LiveData<Boolean> = _barsHidden

	private val _drawerOpen = MutableLiveData(false)
	val drawerOpen: LiveData<Boolean> = _drawerOpen

	fun toggleDrawer() { _drawerOpen.value = !(_drawerOpen.value ?: false) }
	fun setDrawerOpen(open: Boolean) { if (_drawerOpen.value != open) _drawerOpen.value = open }
	fun setBarsHidden(hidden: Boolean) { if (_barsHidden.value != hidden) _barsHidden.value = hidden }
	fun onProfileIconClicked() { _menuVisible.value = !(_menuVisible.value ?: false) }
	fun hideMenu() { _menuVisible.value = false }

	fun onTabSelected(tab: NavTab) {
		if (_activeTab.value != tab) _activeTab.value = tab
	}

	// --- SỬA DỮ LIỆU GIẢ KHỚP VỚI MODEL MỚI ---
	fun loadSamplePosts() {
		if (_posts.value.isNullOrEmpty()) {
			_posts.value = listOf(
				Post(
					id = "1",
					communityName = "c/Sports_Athletics",
					communityIconLetter = "S",
					timePosted = "1h",
					title = "Intramural Sports Week - Highlights from Day 1!",
					content = "Amazing start to sports week with soccer, basketball...",
					voteCount = 156,
					commentCount = 43,
					userVote = "NONE" // Dùng String
				),
				Post(
					id = "2",
					communityName = "c/Tech_Club",
					communityIconLetter = "T",
					timePosted = "3h",
					title = "Hackathon kicks off tomorrow",
					content = "Don't forget to register and bring your energy!",
					voteCount = 89,
					commentCount = 12,
					userVote = "NONE"
				),
				Post(
					id = "3",
					communityName = "c/Campus_Life",
					communityIconLetter = "C",
					timePosted = "5h",
					title = "Study spots recommendation thread",
					content = "Share your favorite quiet spaces around campus.",
					voteCount = 42,
					commentCount = 18,
					userVote = "NONE"
				)
			)
		}
	}

	// --- SỬA LOGIC VOTE (STRING) ---
	fun onUpvote(postId: String) {
		_posts.value = _posts.value?.map { p ->
			if (p.id != postId) p else {
				when (p.userVote) {
					"UP" -> p.copy(userVote = "NONE", voteCount = p.voteCount - 1)
					"DOWN" -> p.copy(userVote = "UP", voteCount = p.voteCount + 2)
					else -> p.copy(userVote = "UP", voteCount = p.voteCount + 1) // NONE case
				}
			}
		}
	}

	fun onDownvote(postId: String) {
		_posts.value = _posts.value?.map { p ->
			if (p.id != postId) p else {
				when (p.userVote) {
					"DOWN" -> p.copy(userVote = "NONE", voteCount = p.voteCount + 1)
					"UP" -> p.copy(userVote = "DOWN", voteCount = p.voteCount - 2)
					else -> p.copy(userVote = "DOWN", voteCount = p.voteCount - 1) // NONE case
				}
			}
		}
	}

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