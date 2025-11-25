package com.hcmus.forumus_client.ui.home

import android.graphics.Rect
import android.view.MotionEvent
import com.hcmus.forumus_client.data.model.Post
import com.hcmus.forumus_client.data.model.VoteState
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

	private val _barsHidden = MutableLiveData(false)
	val barsHidden: LiveData<Boolean> = _barsHidden

	// Drawer state (true = open, false = closed)
	private val _drawerOpen = MutableLiveData(false)
	val drawerOpen: LiveData<Boolean> = _drawerOpen

	fun toggleDrawer() {
		_drawerOpen.value = !(_drawerOpen.value ?: false)
	}

	fun setDrawerOpen(open: Boolean) {
		if (_drawerOpen.value != open) _drawerOpen.value = open
	}

	fun setBarsHidden(hidden: Boolean) {
		if (_barsHidden.value != hidden) _barsHidden.value = hidden
	}

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
					imageUrls = emptyList(),
					userVote = VoteState.NONE
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
					imageUrls = emptyList(),
					userVote = VoteState.NONE
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
					imageUrls = emptyList(),
					userVote = VoteState.NONE
				),
				Post(
					id = "4",
					communityName = "c/Information_Technology",
					communityIconLetter = "C",
					timePosted = "2h",
					title = "Finding teammates for a group project",
					content = "I'm currently working on an android application project for the mobile development class." +
						"\nWe need a team of 5 and currently short 2 people." +
						"\nRequirements are in comments. ",
					voteCount = 23,
					commentCount = 36,
					imageUrls = emptyList(),
					userVote = VoteState.NONE
				),
				Post(
					id = "5",
					communityName = "c/Information_Technology",
					communityIconLetter = "C",
					timePosted = "2h",
					title = "Finding teammates for a group project",
					content = "I'm currently working on an android application project for the mobile development class." +
							"\nWe need a team of 5 and currently short 2 people." +
							"\nRequirements are in comments. ",
					voteCount = 23,
					commentCount = 36,
					imageUrls = emptyList(),
					userVote = VoteState.NONE
				),
				Post(
					id = "6",
					communityName = "c/Information_Technology",
					communityIconLetter = "C",
					timePosted = "2h",
					title = "Finding teammates for a group project",
					content = "I'm currently working on an android application project for the mobile development class." +
							"\nWe need a team of 5 and currently short 2 people." +
							"\nRequirements are in comments. ",
					voteCount = 23,
					commentCount = 36,
					imageUrls = emptyList(),
					userVote = VoteState.NONE
				)
			)
		}
	}

	fun onUpvote(postId: String) {
		_posts.value = _posts.value?.map { p ->
			if (p.id != postId) p else {
				when (p.userVote) {
					VoteState.UP -> p.copy(userVote = VoteState.NONE, voteCount = p.voteCount - 1)
					VoteState.DOWN -> p.copy(userVote = VoteState.UP, voteCount = p.voteCount + 2)
					VoteState.NONE -> p.copy(userVote = VoteState.UP, voteCount = p.voteCount + 1)
				}
			}
		}
	}

	fun onDownvote(postId: String) {
		_posts.value = _posts.value?.map { p ->
			if (p.id != postId) p else {
				when (p.userVote) {
					VoteState.DOWN -> p.copy(userVote = VoteState.NONE, voteCount = p.voteCount + 1) // remove downvote (+1 net)
					VoteState.UP -> p.copy(userVote = VoteState.DOWN, voteCount = p.voteCount - 2)
					VoteState.NONE -> p.copy(userVote = VoteState.DOWN, voteCount = p.voteCount - 1)
				}
			}
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