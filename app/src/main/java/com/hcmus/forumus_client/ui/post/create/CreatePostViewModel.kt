package com.hcmus.forumus_client.ui.post.create

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.hcmus.forumus_client.data.model.Post
import com.hcmus.forumus_client.data.repository.PostRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class PostState {
    object Loading : PostState()
    object Success : PostState()
    data class Error(val msg: String) : PostState()
}

// Chuyển thành AndroidViewModel để lấy Context check loại file (Ảnh hay Video)
class CreatePostViewModel(application: Application) : AndroidViewModel(application) {

    private val _selectedImages = MutableLiveData<MutableList<Uri>>(mutableListOf())
    val selectedImages: LiveData<MutableList<Uri>> get() = _selectedImages

    private val _postState = MutableLiveData<PostState>()
    val postState: LiveData<PostState> get() = _postState

    // Giữ LiveData cũ
    private val _generatedTitle = MutableLiveData<String>()
    val generatedTitle: LiveData<String> = _generatedTitle
    private val _isLoadingAi = MutableLiveData<Boolean>()
    val isLoadingAi: LiveData<Boolean> = _isLoadingAi
    val errorAi = MutableLiveData<String>()

    private val auth = FirebaseAuth.getInstance()
    private val repository = PostRepository()
    private val context = application.applicationContext

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

    fun createPost(title: String, content: String, selectedTopics: List<String>) {
        _postState.value = PostState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = auth.currentUser
                if (user == null) {
                    withContext(Dispatchers.Main) { _postState.value = PostState.Error("You must be logged in!") }
                    return@launch
                }

                // 1. Tạo ID theo quy ước: POST_yyyyMMdd_HHmmss_random
                val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val dateStr = sdf.format(Date())
                val randomPart = (1000..9999).random()
                val postId = "POST_${dateStr}_${randomPart}"

                // 2. Thông tin tác giả
                val finalAuthorName = if (!user.displayName.isNullOrEmpty()) {
                    user.displayName!!
                } else {
                    user.email?.substringBefore("@") ?: "Unknown User"
                }
                val finalAvatar = user.photoUrl?.toString() ?: ""

                // 3. Phân loại Ảnh và Video từ danh sách đã chọn
                val localImageUrls = mutableListOf<String>()
                val localVideoUrls = mutableListOf<String>()
                val allUris = _selectedImages.value ?: emptyList()

                for (uri in allUris) {
                    val mimeType = context.contentResolver.getType(uri)
                    if (mimeType != null && mimeType.startsWith("video")) {
                        localVideoUrls.add(uri.toString())
                    } else {
                        // Mặc định coi là ảnh nếu không phải video
                        localImageUrls.add(uri.toString())
                    }
                }

                // 4. Tạo Object Post (Khớp với Post.kt gốc của bạn)
                val post = Post(
                    id = postId,
                    title = title,
                    content = content,

                    // Truyền URI nội bộ, Repository sẽ upload và thay thế bằng link online
                    imageUrls = localImageUrls,
                    videoUrls = localVideoUrls,

                    // Mapping selectedTopics vào topicIds
                    topicIds = selectedTopics,

                    authorId = user.uid,
                    authorName = finalAuthorName,
                    authorAvatarUrl = finalAvatar,
                    createdAt = Timestamp.now()
                )

                // 5. Gọi Repository để xử lý upload và lưu
                val result = repository.savePost(post)

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