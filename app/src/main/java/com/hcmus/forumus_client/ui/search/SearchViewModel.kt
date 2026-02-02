package com.hcmus.forumus_client.ui.search

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
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
    private val KEY_POST_KEYWORDS = "key_post_keywords"
    private val KEY_PEOPLE_KEYWORDS = "key_people_keywords"

    // --- LiveData ---
    private val _postResults = MutableLiveData<List<Post>>()
    val postResults: LiveData<List<Post>> = _postResults

    private val _userResults = MutableLiveData<List<User>>()
    val userResults: LiveData<List<User>> = _userResults

    private val _trendingTopics = MutableLiveData<List<Topic>>()
    val trendingTopics: LiveData<List<Topic>> = _trendingTopics

    private val _allTopics = MutableLiveData<List<Topic>>()
    val allTopics: LiveData<List<Topic>> = _allTopics

    private val _recentPostKeywords = MutableLiveData<List<String>>()
    val recentPostKeywords: LiveData<List<String>> = _recentPostKeywords

    private val _recentPeopleKeywords = MutableLiveData<List<String>>()
    val recentPeopleKeywords: LiveData<List<String>> = _recentPeopleKeywords

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private var topicNameMap: Map<String, String> = emptyMap()

    init {
        loadAllHistory()
        preloadAllTopics()
    }

    private fun preloadAllTopics() {
        viewModelScope.launch(Dispatchers.IO) {
            val topics = postRepository.getAllTopics()

            withContext(Dispatchers.Main) {
                _allTopics.value = topics
            }

            topicNameMap = topics.associate { it.id to it.name }
        }
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

        addPostKeyword(query)

        _isLoading.value = true
        val normalizedQuery = removeAccents(query)

        class SearchViewModel(application: Application) : AndroidViewModel(application) {

            private val postRepository = PostRepository()
            private val userRepository = UserRepository()
            private val context = application.applicationContext
            private val gson = Gson()

            private val PREFS_NAME = "search_history_prefs"
            private val KEY_POST_KEYWORDS = "key_post_keywords"
            private val KEY_PEOPLE_KEYWORDS = "key_people_keywords"

            // --- LiveData ---
            private val _postResults = MutableLiveData<List<Post>>()
            val postResults: LiveData<List<Post>> = _postResults

            private val _userResults = MutableLiveData<List<User>>()
            val userResults: LiveData<List<User>> = _userResults

            private val _trendingTopics = MutableLiveData<List<Topic>>()
            val trendingTopics: LiveData<List<Topic>> = _trendingTopics

            private val _allTopics = MutableLiveData<List<Topic>>()
            val allTopics: LiveData<List<Topic>> = _allTopics

            private val _recentPostKeywords = MutableLiveData<List<String>>()
            val recentPostKeywords: LiveData<List<String>> = _recentPostKeywords

            private val _recentPeopleKeywords = MutableLiveData<List<String>>()
            val recentPeopleKeywords: LiveData<List<String>> = _recentPeopleKeywords

            private val _isLoading = MutableLiveData(false)
            val isLoading: LiveData<Boolean> = _isLoading

            private var topicNameMap: Map<String, String> = emptyMap()

            init {
                loadAllHistory()
                preloadAllTopics()
            }

            private fun preloadAllTopics() {
                viewModelScope.launch(Dispatchers.IO) {
                    val topics = postRepository.getAllTopics()

                    withContext(Dispatchers.Main) {
                        _allTopics.value = topics
                    }

                    topicNameMap = topics.associate { it.id to it.name }
                }
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
                if (query.isBlank()) {
                    _postResults.value = emptyList(); return
                }

                addPostKeyword(query)

                _isLoading.value = true
                val normalizedQuery = removeAccents(query)

                viewModelScope.launch(Dispatchers.IO) {
                    val allPosts = postRepository.searchPostsCandidates()

                    val filtered = allPosts.filter { post ->
                        val titleNorm = removeAccents(post.title)

                        // 2. [FIXED] Search by Topic Name instead of ID
                        val hasMatchingTopic = post.topicIds.any { topicId ->
                            // Get real name from Map, fallback to ID if not found
                            val realName = topicNameMap[topicId] ?: topicId
                            val topicNameNorm = removeAccents(realName)

                            // Compare query with Topic Name
                            topicNameNorm.contains(normalizedQuery)
                        }

                        // Logic OR: Title match OR Topic Name match
                        titleNorm.contains(normalizedQuery) || hasMatchingTopic
                    }

                    withContext(Dispatchers.Main) {
                        _postResults.value = filtered
                        _isLoading.value = false
                    }
                }
            }

            fun searchUsers(query: String) {
                if (query.isBlank()) {
                    _userResults.value = emptyList(); return
                }
                addPeopleKeyword(query)
                _isLoading.value = true
                val normalizedQuery = removeAccents(query)

                viewModelScope.launch(Dispatchers.IO) {
                    val allUsers = userRepository.searchUsersCandidates()
                    val filtered = allUsers.filter { user ->
                        if (user.email == "admin@admin.forumus.me") {
                            return@filter false
                        }
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

            private fun loadAllHistory() {
                try {
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val type = object : TypeToken<List<String>>() {}.type
                    _recentPostKeywords.value =
                        gson.fromJson(prefs.getString(KEY_POST_KEYWORDS, "[]"), type) ?: emptyList()
                    _recentPeopleKeywords.value =
                        gson.fromJson(prefs.getString(KEY_PEOPLE_KEYWORDS, "[]"), type)
                            ?: emptyList()
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
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().putString(key, gson.toJson(list)).apply()
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            val allPosts = postRepository.searchPostsCandidates()

            val filtered = allPosts.filter { post ->
                val titleNorm = removeAccents(post.title)

                // 2. [ĐÃ SỬA] Tìm trong Tên Chủ Đề (Topic Name) thay vì ID
                val hasMatchingTopic = post.topicIds.any { topicId ->
                    // Lấy tên thật từ Map, nếu không có thì dùng tạm ID
                    val realName = topicNameMap[topicId] ?: topicId
                    val topicNameNorm = removeAccents(realName)

                    // So sánh query với Tên chủ đề
                    topicNameNorm.contains(normalizedQuery)
                }

                // Logic OR: Trùng tiêu đề HOẶC Trùng tên chủ đề
                titleNorm.contains(normalizedQuery) || hasMatchingTopic
            }

            withContext(Dispatchers.Main) {
                _postResults.value = filtered
                _isLoading.value = false
            }
        }
    }

    fun searchUsers(query: String) {
        if (query.isBlank()) { _userResults.value = emptyList(); return }
        addPeopleKeyword(query)
        _isLoading.value = true
        val normalizedQuery = removeAccents(query)

        viewModelScope.launch(Dispatchers.IO) {
            val allUsers = userRepository.searchUsersCandidates()
            val filtered = allUsers.filter { user ->
                if (user.email == "admin@admin.forumus.me") {
                    return@filter false
                }
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

    private fun loadAllHistory() {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val type = object : TypeToken<List<String>>() {}.type
            _recentPostKeywords.value = gson.fromJson(prefs.getString(KEY_POST_KEYWORDS, "[]"), type) ?: emptyList()
            _recentPeopleKeywords.value = gson.fromJson(prefs.getString(KEY_PEOPLE_KEYWORDS, "[]"), type) ?: emptyList()
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
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(key, gson.toJson(list)).apply()
    }
}