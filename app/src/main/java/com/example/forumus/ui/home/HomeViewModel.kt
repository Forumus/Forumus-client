package com.example.forumus.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Post(
    val id: String,
    val communityName: String,
    val communityIcon: String,
    val timePosted: String,
    val title: String,
    val content: String,
    val images: List<String>,
    val upvotes: Int,
    val downvotes: Int,
    val comments: Int,
    val shares: Int
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    // Inject your repositories here
) : ViewModel() {

    private val _postsFlow = MutableStateFlow<List<Post>>(emptyList())
    val postsFlow: StateFlow<List<Post>> = _postsFlow

    private val _loadingState = MutableStateFlow(false)
    val loadingState: StateFlow<Boolean> = _loadingState

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState

    init {
        loadPosts()
    }

    private fun loadPosts() {
        viewModelScope.launch {
            try {
                _loadingState.value = true
                // TODO: Fetch posts from repository
                _postsFlow.value = getSamplePosts()
                _errorState.value = null
            } catch (e: Exception) {
                _errorState.value = e.message
            } finally {
                _loadingState.value = false
            }
        }
    }

    fun refreshPosts() {
        loadPosts()
    }

    fun upvotePost(postId: String) {
        // TODO: Call repository to upvote
    }

    fun downvotePost(postId: String) {
        // TODO: Call repository to downvote
    }

    fun sharePost(postId: String) {
        // TODO: Share post
    }

    private fun getSamplePosts(): List<Post> {
        return listOf(
            Post(
                id = "1",
                communityName = "c/Sports_Athletics",
                communityIcon = "S",
                timePosted = "1h",
                title = "Intramural Sports Week - Highlights from Day 1!",
                content = "What an amazing start to our annual sports week! Check out these action shots from soccer, basketball, tennis, track, gym, and swimming events. Go team!",
                images = listOf(
                    "https://www.figma.com/api/mcp/asset/8f2ebf0e-27f6-4e39-961f-466c5b66c5e9",
                    "https://www.figma.com/api/mcp/asset/62b1b2e5-e305-4c25-a5a4-68c5175a765b",
                    "https://www.figma.com/api/mcp/asset/101cb4d7-414b-47e1-9954-564fe281a4d9",
                    "https://www.figma.com/api/mcp/asset/18e40d40-27b7-487c-bd7f-fcd1072cb64a"
                ),
                upvotes = 156,
                downvotes = 0,
                comments = 43,
                shares = 0
            ),
            Post(
                id = "2",
                communityName = "c/CSE_Students",
                communityIcon = "C",
                timePosted = "2h",
                title = "Anyone else struggling with the Data Structures midterm?",
                content = "The recursion questions are killing me. Does anyone have good resources for understanding tree traversal better?",
                images = emptyList(),
                upvotes = 42,
                downvotes = 0,
                comments = 18,
                shares = 0
            ),
            Post(
                id = "3",
                communityName = "c/CampusEvents",
                communityIcon = "C",
                timePosted = "4h",
                title = "Tech Fest 2025 - Registration Open!",
                content = "Excited to announce that registration for our annual Tech Fest is now open! Early bird discount available for the first 100 students.",
                images = listOf(
                    "https://www.figma.com/api/mcp/asset/acf16811-f6ad-49e7-93b2-90df29d67557",
                    "https://www.figma.com/api/mcp/asset/6998c3c9-3793-4da0-a494-21c3e7011f9e",
                    "https://www.figma.com/api/mcp/asset/f42f84cb-d335-4f66-9952-658c9e2e9ece"
                ),
                upvotes = 89,
                downvotes = 0,
                comments = 24,
                shares = 0
            ),
            Post(
                id = "4",
                communityName = "c/StudyGroups",
                communityIcon = "S",
                timePosted = "6h",
                title = "Looking for Calculus 2 study partners",
                content = "Planning to meet in the library every Tuesday and Thursday at 4 PM. DM me if interested!",
                images = listOf(
                    "https://www.figma.com/api/mcp/asset/8065c333-bd2b-41bd-86d0-0f6a3100cdf5",
                    "https://www.figma.com/api/mcp/asset/9bc01993-40cc-4071-ad5b-09fc80505819"
                ),
                upvotes = 15,
                downvotes = 0,
                comments = 7,
                shares = 0
            )
        )
    }
}
