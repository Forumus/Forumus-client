package com.hcmus.forumus_client.ui.media

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MediaViewerViewModel(application: Application) : AndroidViewModel(application) {
    private val _mediaItems = MutableLiveData<List<MediaViewerItem>>(emptyList())
    val mediaItems: LiveData<List<MediaViewerItem>> = _mediaItems

    private val _currentIndex = MutableLiveData<Int>(0)
    val currentIndex: LiveData<Int> = _currentIndex

    private val imageLoader = ImageLoader(application)

    /**
     * Set media list and optionally prefetch current + neighbors.
     * Preloading logic lives here so the Fragment can be lightweight.
     */
    fun setMediaList(items: List<MediaViewerItem>, startIndex: Int) {
        _mediaItems.value = items
        _currentIndex.value = startIndex.coerceIn(0, maxOf(0, items.size - 1))

        // Prefetch current and neighbor images/videos
        viewModelScope.launch(Dispatchers.IO) {
            val indices = listOf((_currentIndex.value ?: 0) - 1, _currentIndex.value ?: 0, (_currentIndex.value ?: 0) + 1)
            indices.forEach { i ->
                if (i in items.indices) {
                    val it = items[i]
                    val url = it.imageUrl ?: it.thumbnailUrl ?: it.videoUrl
                    if (!url.isNullOrBlank() && it.type == MediaViewerItem.Type.IMAGE) {
                        val req = ImageRequest.Builder(getApplication()).data(url).build()
                        try { imageLoader.execute(req) } catch (_: Exception) { }
                    }
                }
            }
        }
    }

    fun prev() {
        val idx = (_currentIndex.value ?: 0)
        if (idx > 0) _currentIndex.value = idx - 1
    }

    fun next() {
        val idx = (_currentIndex.value ?: 0)
        val size = _mediaItems.value?.size ?: 0
        if (idx < size - 1) _currentIndex.value = idx + 1
    }

    /**
     * Set the current index directly (used by ViewPager2 callback)
     */
    fun setCurrentIndex(index: Int) {
        val size = _mediaItems.value?.size ?: 0
        if (index in 0 until size) {
            _currentIndex.value = index
        }
    }
}
