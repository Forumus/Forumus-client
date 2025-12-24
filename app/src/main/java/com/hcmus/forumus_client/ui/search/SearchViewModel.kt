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
    private val gson = Gson()

    private val PREFS_NAME = "search_history_prefs"
    // Hai key riêng biệt cho 2 loại tìm kiếm
    private val KEY_POST_KEYWORDS = "key_post_keywords"
    private val KEY_PEOPLE_KEYWORDS = "key_people_keywords"

    // --- LiveData ---
    private val _postResults = MutableLiveData<List<Post>>()
    val postResults: LiveData<List<Post>> = _postResults

    private val _userResults = MutableLiveData<List<User>>()
    val userResults: LiveData<List<User>> = _userResults

    private val _trendingTopics = MutableLiveData<List<Topic>>()
    val trendingTopics: LiveData<List<Topic>> = _trendingTopics

    // --- TÁCH BIỆT LỊCH SỬ TỪ KHÓA ---
    private val _recentPostKeywords = MutableLiveData<List<String>>()
    val recentPostKeywords: LiveData<List<String>> = _recentPostKeywords

    private val _recentPeopleKeywords = MutableLiveData<List<String>>()
    val recentPeopleKeywords: LiveData<List<String>> = _recentPeopleKeywords

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadAllHistory()
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

    // --- TÌM KIẾM BÀI VIẾT ---
    fun searchPosts(query: String) {
        if (query.isBlank()) { _postResults.value = emptyList(); return }

        // Lưu vào lịch sử POST
        addPostKeyword(query)

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

    // --- TÌM KIẾM NGƯỜI DÙNG (NAME/EMAIL) ---
    fun searchUsers(query: String) {
        if (query.isBlank()) { _userResults.value = emptyList(); return }

        // Lưu vào lịch sử PEOPLE
        addPeopleKeyword(query)

        _isLoading.value = true
        val normalizedQuery = removeAccents(query)

        viewModelScope.launch(Dispatchers.IO) {
            // Lấy danh sách candidate và lọc theo tên hoặc email
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

    // --- QUẢN LÝ LỊCH SỬ ---
    private fun loadAllHistory() {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val type = object : TypeToken<List<String>>() {}.type

            // Load Post Keywords
            val postJson = prefs.getString(KEY_POST_KEYWORDS, "[]")
            val postList: List<String> = gson.fromJson(postJson, type) ?: emptyList()
            _recentPostKeywords.value = postList

            // Load People Keywords
            val peopleJson = prefs.getString(KEY_PEOPLE_KEYWORDS, "[]")
            val peopleList: List<String> = gson.fromJson(peopleJson, type) ?: emptyList()
            _recentPeopleKeywords.value = peopleList

        } catch (e: Exception) {
            _recentPostKeywords.value = emptyList()
            _recentPeopleKeywords.value = emptyList()
        }
    }

    private fun addPostKeyword(query: String) {
        val list = _recentPostKeywords.value?.toMutableList() ?: mutableListOf()
        updateListAndSave(list, query, KEY_POST_KEYWORDS)
        _recentPostKeywords.value = list
    }

    private fun addPeopleKeyword(query: String) {
        val list = _recentPeopleKeywords.value?.toMutableList() ?: mutableListOf()
        updateListAndSave(list, query, KEY_PEOPLE_KEYWORDS)
        _recentPeopleKeywords.value = list
    }

    private fun updateListAndSave(list: MutableList<String>, query: String, key: String) {
        val trimmed = query.trim()
        list.removeAll { it.equals(trimmed, ignoreCase = true) }
        list.add(0, trimmed)
        if (list.size > 5) list.removeAt(list.size - 1)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(key, gson.toJson(list)).apply()
    }
}