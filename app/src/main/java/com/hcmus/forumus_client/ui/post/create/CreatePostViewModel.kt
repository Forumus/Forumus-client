package com.hcmus.forumus_client.ui.post.create

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.hcmus.forumus_client.data.model.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

sealed class PostState {
    object Loading : PostState()
    object Success : PostState()
    data class Error(val msg: String) : PostState()
}

class CreatePostViewModel : ViewModel() {

    private val _selectedImages = MutableLiveData<MutableList<Uri>>(mutableListOf())
    val selectedImages: LiveData<MutableList<Uri>> get() = _selectedImages

    private val _postState = MutableLiveData<PostState>()
    val postState: LiveData<PostState> get() = _postState

    // Giữ lại LiveData AI để không lỗi Activity
    private val _generatedTitle = MutableLiveData<String>()
    val generatedTitle: LiveData<String> = _generatedTitle
    private val _isLoadingAi = MutableLiveData<Boolean>()
    val isLoadingAi: LiveData<Boolean> = _isLoadingAi
    val errorAi = MutableLiveData<String>()

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

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

                // 1. Upload ảnh
                val uploadedUrls = mutableListOf<String>()
                val uris = _selectedImages.value.orEmpty()
                for (uri in uris) {
                    val fileName = "${UUID.randomUUID()}"
                    val ref = storage.reference.child("posts_images/$fileName")
                    ref.putFile(uri).await()
                    uploadedUrls.add(ref.downloadUrl.await().toString())
                }

                // 2. Xử lý TÊN TÁC GIẢ (Fix lỗi tên rỗng)
                val finalAuthorName = if (!user.displayName.isNullOrEmpty()) {
                    user.displayName!!
                } else {
                    user.email?.substringBefore("@") ?: "Unknown User"
                }

                val finalAvatar = user.photoUrl?.toString() ?: ""

                // 3. Tạo Post
                val newPostRef = firestore.collection("posts").document()
                val mainTopic = selectedTopics.firstOrNull() ?: "General"
                val mainIcon = mainTopic.take(1).uppercase()

                val post = Post(
                    id = newPostRef.id,
                    title = title,
                    content = content,
                    imageUrls = uploadedUrls,

                    // Dữ liệu quan trọng
                    topics = selectedTopics,
                    communityName = mainTopic,
                    communityIconLetter = mainIcon,

                    authorId = user.uid,
                    authorName = finalAuthorName, // Đã fix
                    authorAvatarUrl = finalAvatar,

                    createdAt = Timestamp.now()
                )

                // 4. Lưu
                val batch = firestore.batch()
                batch.set(newPostRef, post)

                for (topicName in selectedTopics) {
                    // Chuyển tên topic thành ID (viết thường, không dấu cách) để làm Document ID
                    val topicId = topicName.lowercase().replace(" ", "_")
                    val topicRef = firestore.collection("topics").document(topicId)

                    // Dùng set với merge để an toàn: Tự tạo nếu chưa có
                    val topicData = hashMapOf(
                        "name" to topicName,
                        "postCount" to FieldValue.increment(1)
                    )
                    batch.set(topicRef, topicData, com.google.firebase.firestore.SetOptions.merge())
                }

                batch.commit().await()

                withContext(Dispatchers.Main) {
                    _postState.value = PostState.Success
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _postState.value = PostState.Error("Error: ${e.message}")
                }
            }
        }
    }
}