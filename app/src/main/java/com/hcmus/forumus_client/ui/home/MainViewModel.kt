package com.hcmus.forumus_client.ui.home

import android.graphics.Rect
import android.view.MotionEvent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hcmus.forumus_client.data.model.Post
import com.hcmus.forumus_client.data.model.VoteState // Import Enum VoteState

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
					title = "Intramural Sports Week",
					content = "Amazing start to sports week...",
					upvoteCount = 156, // Dùng upvoteCount thay vì voteCount
					commentCount = 43,
					userVote = VoteState.NONE // Dùng Enum, không dùng String "NONE"
					// Các trường communityName, timePosted đã bị bỏ trong Post mới, không truyền vào nữa
				),
				Post(
					id = "2",
					title = "Hackathon kicks off tomorrow",
					content = "Don't forget to register...",
					upvoteCount = 89,
					commentCount = 12,
					userVote = VoteState.NONE
				),
				Post(
					id = "3",
					title = "Study spots recommendation",
					content = "Share your favorite quiet spaces...",
					upvoteCount = 42,
					commentCount = 18,
					userVote = VoteState.NONE
				)
			)
		}
	}

	// --- SỬA LOGIC VOTE (DÙNG ENUM VOTESTATE) ---
	fun onUpvote(postId: String) {
		_posts.value = _posts.value?.map { p ->
			if (p.id != postId) p else {
				when (p.userVote) {
					VoteState.UPVOTE -> p.copy(userVote = VoteState.NONE, upvoteCount = p.upvoteCount - 1)
					VoteState.DOWNVOTE -> p.copy(userVote = VoteState.UPVOTE, upvoteCount = p.upvoteCount + 1, downvoteCount = p.downvoteCount - 1)
					else -> p.copy(userVote = VoteState.UPVOTE, upvoteCount = p.upvoteCount + 1)
				}
			}
		}
	}

	fun onDownvote(postId: String) {
		_posts.value = _posts.value?.map { p ->
			if (p.id != postId) p else {
				when (p.userVote) {
					VoteState.DOWNVOTE -> p.copy(userVote = VoteState.NONE, downvoteCount = p.downvoteCount - 1)
					VoteState.UPVOTE -> p.copy(userVote = VoteState.DOWNVOTE, downvoteCount = p.downvoteCount + 1, upvoteCount = p.upvoteCount - 1)
					else -> p.copy(userVote = VoteState.DOWNVOTE, downvoteCount = p.downvoteCount + 1)
				}
			}
		}
	}

	// ... (Phần onTouch giữ nguyên)
}