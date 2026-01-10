package com.hcmus.forumus_client.ui.post.create

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
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

    // --- HÀM NÀY ĐÃ ĐƯỢC CẬP NHẬT ĐỂ NHẬN VỊ TRÍ (Location) ---
    fun createPost(
        title: String,
        content: String,
        selectedTopics: List<String>,
        context: Context,
        locationName: String? = null, // Thêm tham số
        lat: Double? = null,          // Thêm tham số
        lng: Double? = null           // Thêm tham số
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

                    // Gán thông tin vị trí vào Post
                    locationName = locationName,
                    latitude = lat,
                    longitude = lng
                )

                val result = repository.savePost(context, post)

                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        _postState.value = PostState.Success
                    } else {
                        _postState.value = PostState.Error("Failed: ${result.exceptionOrNull()?.message}")
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