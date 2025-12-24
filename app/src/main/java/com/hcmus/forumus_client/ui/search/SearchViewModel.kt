package com.hcmus.forumus_client.ui.search

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import com.hcmus.forumus_client.data.model.Post
import com.hcmus.forumus_client.data.model.Topic
import com.hcmus.forumus_client.data.model.User
import com.hcmus.forumus_client.data.repository.PostRepository
import com.hcmus.forumus_client.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.util.regex.Pattern

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val postRepository = PostRepository()
    private val userRepository = UserRepository()
    private val context = application.applicationContext

    // --- CẤU HÌNH GSON ĐỂ XỬ LÝ TIMESTAMP ---
    // Đây là chìa khóa để fix lỗi Recent Search
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Timestamp::class.java, JsonSerializer<Timestamp> { src, _, _ ->
            JsonPrimitive(src.seconds) // Lưu dưới dạng số giây (Long)
        })
        .registerTypeAdapter(Timestamp::class.java, JsonDeserializer { json, _, _ ->
            Timestamp(json.asLong, 0) // Đọc lại thành Timestamp
        })
        .create()

    private val PREFS_NAME = "search_history_prefs"
    private val KEY_RECENT_POSTS = "key_recent_posts"
    private val KEY_RECENT_USERS = "key_recent_users"

    // --- LiveData ---
    private val _postResults = MutableLiveData<List<Post>>()
    val postResults: LiveData<List<Post>> = _postResults

    private val _userResults = MutableLiveData<List<User>>()
    val userResults: LiveData<List<User>> = _userResults

    private val _trendingTopics = MutableLiveData<List<Topic>>()
    val trendingTopics: LiveData<List<Topic>> = _trendingTopics

    private val _recentPosts = MutableLiveData<List<Post>>()
    val recentPosts: LiveData<List<Post>> = _recentPosts

    private val _recentUsers = MutableLiveData<List<User>>()
    val recentUsers: LiveData<List<User>> = _recentUsers

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadRecentHistory()
    }

    fun loadTrendingTopics() {
        viewModelScope.launch(Dispatchers.IO) {
            val topics = postRepository.getTrendingTopics()
            withContext(Dispatchers.Main) { _trendingTopics.value = topics }
        }
    }

    private fun removeAccents(str: String?): String {
        if (str == null) return ""
        val nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD)
        val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
        return pattern.matcher(nfdNormalizedString).replaceAll("").lowercase().trim()
    }

    fun searchPosts(query: String) {
        if (query.isBlank()) { _postResults.value = emptyList(); return }
        _isLoading.value = true
        val normalizedQuery = removeAccents(query)

        viewModelScope.launch(Dispatchers.IO) {
            val allPosts = postRepository.searchPostsCandidates()
            val filtered = allPosts.filter { post ->
                val titleNorm = removeAccents(post.title)
                val topicNorms = post.topicIds.map { removeAccents(it) }
                titleNorm.contains(normalizedQuery) || topicNorms.any { it.contains(normalizedQuery) }
            }
            withContext(Dispatchers.Main) {
                _postResults.value = filtered
                _isLoading.value = false
            }
        }
    }

    fun searchUsers(query: String) {
        if (query.isBlank()) { _userResults.value = emptyList(); return }
        _isLoading.value = true
        val normalizedQuery = removeAccents(query)
        viewModelScope.launch(Dispatchers.IO) {
            val allUsers = userRepository.searchUsersCandidates()
            val filtered = allUsers.filter { user ->
                val nameNorm = removeAccents(user.fullName)
                val emailNorm = removeAccents(user.email)
                nameNorm.contains(normalizedQuery) || emailNorm.contains(normalizedQuery)
            }
            withContext(Dispatchers.Main) {
                _userResults.value = filtered
                _isLoading.value = false
            }
        }
    }

    // --- RECENT HISTORY ---
    private fun loadRecentHistory() {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            val postsJson = prefs.getString(KEY_RECENT_POSTS, "[]")
            val postType = object : TypeToken<List<Post>>() {}.type
            // Sử dụng Gson đã cấu hình Timestamp
            val savedPosts: List<Post> = gson.fromJson(postsJson, postType) ?: emptyList()
            _recentPosts.value = savedPosts

            val usersJson = prefs.getString(KEY_RECENT_USERS, "[]")
            val userType = object : TypeToken<List<User>>() {}.type
            val savedUsers: List<User> = gson.fromJson(usersJson, userType) ?: emptyList()
            _recentUsers.value = savedUsers
        } catch (e: Exception) {
            // Nếu dữ liệu cũ lỗi, reset về rỗng để tránh crash
            _recentPosts.value = emptyList()
            _recentUsers.value = emptyList()
        }
    }

    fun addToRecentPosts(post: Post) {
        val currentList = _recentPosts.value?.toMutableList() ?: mutableListOf()
        currentList.removeAll { it.id == post.id }
        currentList.add(0, post)
        if (currentList.size > 5) currentList.removeAt(currentList.size - 1)

        _recentPosts.value = currentList
        saveToPrefs(KEY_RECENT_POSTS, currentList)
    }

    fun addToRecentUsers(user: User) {
        val currentList = _recentUsers.value?.toMutableList() ?: mutableListOf()
        currentList.removeAll { it.uid == user.uid }
        currentList.add(0, user)
        if (currentList.size > 5) currentList.removeAt(currentList.size - 1)

        _recentUsers.value = currentList
        saveToPrefs(KEY_RECENT_USERS, currentList)
    }

    private fun saveToPrefs(key: String, list: List<Any>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(list)
        prefs.edit().putString(key, json).apply()
    }
}