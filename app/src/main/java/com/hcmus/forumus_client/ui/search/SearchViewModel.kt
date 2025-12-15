package com.hcmus.forumus_client.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hcmus.forumus_client.data.model.Post
import com.hcmus.forumus_client.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SearchViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private val _postResults = MutableLiveData<List<Post>>()
    val postResults: LiveData<List<Post>> = _postResults

    private val _userResults = MutableLiveData<List<User>>()
    val userResults: LiveData<List<User>> = _userResults

    private val _trendingTopics = MutableLiveData<List<String>>()
    val trendingTopics: LiveData<List<String>> = _trendingTopics

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Load 5 chủ đề hot nhất dựa trên postCount (đã setup ở CreatePostViewModel)
    fun loadTrendingTopics() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("topics")
                    .orderBy("postCount", Query.Direction.DESCENDING)
                    .limit(5)
                    .get()
                    .await()

                val topics = snapshot.documents.mapNotNull { it.getString("name") }
                withContext(Dispatchers.Main) { _trendingTopics.value = topics }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun searchPosts(query: String) {
        if (query.isBlank()) return
        _isLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Firestore không hỗ trợ search full-text tốt.
                // Cách đơn giản: lấy bài mới nhất về và filter client-side (với tập dữ liệu nhỏ)
                // Hoặc dùng: array-contains 'topics' nếu search topic

                // Ở đây demo cách filter client-side cho linh hoạt (title hoặc content hoặc topic)
                val allPosts = firestore.collection("posts")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(100) // Giới hạn lấy 100 bài mới nhất để filter
                    .get()
                    .await()
                    .toObjects(Post::class.java)

                val filtered = allPosts.filter { post ->
                    post.title.contains(query, ignoreCase = true) ||
                            post.content.contains(query, ignoreCase = true) ||
                            post.topicIds.any { it.contains(query, ignoreCase = true) }
                }

                withContext(Dispatchers.Main) {
                    _postResults.value = filtered
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { _isLoading.value = false }
            }
        }
    }

    fun searchUsers(query: String) {
        if (query.isBlank()) return
        _isLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Search User theo Name hoặc Email
                // Lưu ý: Firestore search String cần chính xác hoặc prefix.
                // Hack: search prefix
                val usersRef = firestore.collection("users")

                // Demo lấy list user về filter cho chính xác
                val allUsers = usersRef.limit(50).get().await().toObjects(User::class.java)

                val filtered = allUsers.filter { user ->
                    (user.fullName ?: "").contains(query, ignoreCase = true) ||
                            (user.email ?: "").contains(query, ignoreCase = true)
                }

                withContext(Dispatchers.Main) {
                    _userResults.value = filtered
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { _isLoading.value = false }
            }
        }
    }
}