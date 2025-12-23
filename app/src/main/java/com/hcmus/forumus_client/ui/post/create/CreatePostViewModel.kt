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

class CreatePostViewModel(application: Application) : AndroidViewModel(application) {

    private val _selectedImages = MutableLiveData<MutableList<Uri>>(mutableListOf())
    val selectedImages: LiveData<MutableList<Uri>> get() = _selectedImages

    private val _postState = MutableLiveData<PostState>()
    val postState: LiveData<PostState> get() = _postState

    // --- MỚI: LiveData chứa thông tin User hiện tại ---
    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> get() = _currentUser

    // Các biến cũ
    private val _generatedTitle = MutableLiveData<String>()
    val generatedTitle: LiveData<String> = _generatedTitle
    private val _isLoadingAi = MutableLiveData<Boolean>()
    val isLoadingAi: LiveData<Boolean> = _isLoadingAi
    val errorAi = MutableLiveData<String>()

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance() // Thêm Firestore instance
    private val repository = PostRepository()

    init {
        fetchCurrentUser() // Gọi hàm lấy user ngay khi khởi tạo
    }

    // --- MỚI: Hàm lấy thông tin User từ Firestore ---
    private fun fetchCurrentUser() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            firestore.collection("users").document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        try {
                            // Parse document thành object User
                            val user = document.toObject(User::class.java)
                            _currentUser.value = user
                        } catch (e: Exception) {
                            Log.e("CreatePostVM", "Error parsing user", e)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("CreatePostVM", "Error fetching user", e)
                }
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

    fun generateTitleFromContent(content: String) { /* No-op */ }

    fun createPost(title: String, content: String, selectedTopics: List<String>, context: Context) {
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

                // Có thể thêm authorId vào Post nếu Model Post của bạn hỗ trợ
                val post = Post(
                    title = title,
                    content = content,
                    imageUrls = localImageUrls,
                    videoUrls = localVideoUrls,
                    topicIds = selectedTopics.map { it.trim().lowercase().replace(" ", "_") },
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
}