package com.hcmus.forumus_client.ui.post.create

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hcmus.forumus_client.data.model.Post
import com.hcmus.forumus_client.data.model.Topic
import com.hcmus.forumus_client.data.model.User
import com.hcmus.forumus_client.data.repository.PostRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class PostState {
    object Loading : PostState()
    object Success : PostState()
    data class Error(val msg: String) : PostState()
}

data class TopicAppearance(val colorHex: String, val alpha: Float)

class CreatePostViewModel(application: Application) : AndroidViewModel(application) {

    private val _selectedImages = MutableLiveData<MutableList<Uri>>(mutableListOf())
    val selectedImages: LiveData<MutableList<Uri>> get() = _selectedImages

    private val _postState = MutableLiveData<PostState>()
    val postState: LiveData<PostState> get() = _postState

    private val _allTopics = MutableLiveData<List<Topic>>()
    val allTopics: LiveData<List<Topic>> get() = _allTopics

    private val _suggestedTopics = MutableLiveData<List<Topic>>()
    val suggestedTopics: LiveData<List<Topic>> get() = _suggestedTopics

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> get() = _currentUser

    private val _topicColors = MutableLiveData<Map<String, TopicAppearance>>()
    val topicColors: LiveData<Map<String, TopicAppearance>> get() = _topicColors

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val repository = PostRepository()

    // Key cho SharedPreferences
    private val PREFS_NAME = "forumus_post_draft"

    init {
        fetchCurrentUser()
        fetchTopicColors()
    }

    private fun fetchCurrentUser() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        try {
                            val user = document.toObject(User::class.java)
                            _currentUser.value = user
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
        }
    }

    private fun fetchTopicColors() {
        firestore.collection("topics").get()
            .addOnSuccessListener { result ->
                val colorMap = mutableMapOf<String, TopicAppearance>()
                for (document in result) {
                    val name = document.getString("name")
                    val color = document.getString("fillColor")
                    val alpha = document.getDouble("fillAlpha")?.toFloat() ?: 1.0f
                    if (name != null && color != null) {
                        colorMap[name] = TopicAppearance(color, alpha)
                    }
                }
                _topicColors.value = colorMap
            }
    }

    fun addImages(uris: List<Uri>) {
        val currentList = _selectedImages.value ?: mutableListOf()
        currentList.addAll(uris)
        _selectedImages.value = currentList
    }

    fun removeImage(uri: Uri) {
        val currentList = _selectedImages.value ?: return
        currentList.remove(uri)
        _selectedImages.value = currentList
    }

    // --- CÁC HÀM XỬ LÝ DRAFT (LƯU NHÁP) ---

    // 1. Lưu nháp vào Local
    fun saveDraft(
        context: Context,
        title: String,
        content: String,
        locationName: String?,
        lat: Double?,
        lng: Double?,
        topics: List<String>
    ) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("draft_title", title)
            putString("draft_content", content)
            putString("draft_location_name", locationName)
            putString("draft_lat", lat?.toString()) // SharedPrefs ko lưu Double, convert sang String
            putString("draft_lng", lng?.toString())

            // Lưu list Topic thành chuỗi cách nhau dấu phẩy
            putString("draft_topics", topics.joinToString(","))

            // Lưu list ảnh (URI) thành chuỗi
            val imageUris = _selectedImages.value?.joinToString(";") { it.toString() } ?: ""
            putString("draft_images", imageUris)

            apply() // Lưu bất đồng bộ
        }
        Log.d("Draft", "Saved draft locally")
    }

    // 2. Khôi phục bản nháp
    fun restoreDraft(context: Context): Map<String, Any?>? {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val title = sharedPref.getString("draft_title", "")
        val content = sharedPref.getString("draft_content", "")
        val imagesStr = sharedPref.getString("draft_images", "")

        // Nếu không có dữ liệu quan trọng thì coi như không có draft
        if (title.isNullOrEmpty() && content.isNullOrEmpty() && imagesStr.isNullOrEmpty()) {
            return null
        }

        val locationName = sharedPref.getString("draft_location_name", null)
        val latStr = sharedPref.getString("draft_lat", null)
        val lngStr = sharedPref.getString("draft_lng", null)
        val topicsStr = sharedPref.getString("draft_topics", "")

        val lat = latStr?.toDoubleOrNull()
        val lng = lngStr?.toDoubleOrNull()
        val topics = if (topicsStr.isNullOrEmpty()) emptyList() else topicsStr.split(",")

        // Khôi phục ảnh vào LiveData ngay lập tức để RecyclerView update
        if (!imagesStr.isNullOrEmpty()) {
            val uris = imagesStr.split(";").map { it.toUri() }.toMutableList()
            _selectedImages.value = uris
        }

        return mapOf(
            "title" to title,
            "content" to content,
            "locationName" to locationName,
            "lat" to lat,
            "lng" to lng,
            "topics" to topics
        )
    }

    // 3. Xóa bản nháp (Khi post thành công hoặc Discard)
    fun clearDraft(context: Context) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            clear()
            apply()
        }
        _selectedImages.value = mutableListOf() // Reset UI ảnh
        Log.d("Draft", "Draft cleared")
    }

    // --- CREATE POST ---
    fun createPost(
        title: String,
        content: String,
        selectedTopics: List<String>,
        context: Context,
        locationName: String? = null,
        lat: Double? = null,
        lng: Double? = null
    ) {
        _postState.value = PostState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = auth.currentUser
                if (user == null) {
                    withContext(Dispatchers.Main) { _postState.value = PostState.Error("You must be logged in!") }
                    return@launch
                }

                val localImageUrls = mutableListOf<String>()
                val localVideoUrls = mutableListOf<String>()
                val allUris = _selectedImages.value ?: emptyList()

                for (uri in allUris) {
                    val mimeType = context.contentResolver.getType(uri)
                    if (mimeType != null && mimeType.startsWith("video")) {
                        localVideoUrls.add(uri.toString())
                    } else {
                        localImageUrls.add(uri.toString())
                    }
                }

                val post = Post(
                    title = title,
                    content = content,
                    imageUrls = localImageUrls,
                    videoUrls = localVideoUrls,
                    topicIds = selectedTopics.map { it.trim().lowercase().replace(" ", "_") },
                    locationName = locationName,
                    latitude = lat,
                    longitude = lng
                )

                val saveResult = repository.savePost(context, post)

                if (saveResult.isSuccess) {
                    val postId = saveResult.getOrNull()
                    if (postId != null) {
                        withContext(Dispatchers.Main) {
                            // Đăng thành công -> Xóa nháp
                            clearDraft(context)
                            _postState.value = PostState.Success
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            _postState.value = PostState.Error("Failed to retrieve Post ID")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _postState.value = PostState.Error("Failed to save post: ${saveResult.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _postState.value = PostState.Error("Error: ${e.message}")
                }
            }
        }
    }

    fun getAllTopics() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val topics = repository.getAllTopics()
                withContext(Dispatchers.Main) { _allTopics.value = topics }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { _allTopics.value = emptyList() }
            }
        }
    }

    fun getSuggestedTopics(title: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val suggestedTopics = repository.getSuggestedTopics(title, content)
                withContext(Dispatchers.Main) { _suggestedTopics.value = suggestedTopics }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { _suggestedTopics.value = emptyList() }
            }
        }
    }
}