# AI Article Summary - Implementation Plan

## 1. Current Architecture Analysis

### API Integration Pattern
- **Networking Library**: Retrofit 2 with Moshi JSON converter
- **Network Service**: Singleton `NetworkService` object in `data/remote/NetworkService.kt`
  - Uses `OkHttpClient` with logging interceptor
  - 30-second timeouts for connect/read/write
  - Base URL configured in `ApiConstants.kt`
- **API Service**: `ApiService` interface with suspend functions using `Response<T>` wrapper
- **Repository Pattern**: Repositories call `NetworkService.apiService.methodName(request)` directly
  - No dependency injection (manual instantiation)
  - Error handling via `try-catch` with `Result<T>` return type

### Article/Post Card Implementation
- **Layout**: `post_item.xml` - LinearLayout-based card with:
  - Author info section (avatar, name, timestamp, menu button)
  - Post content section (title, content text)
  - Media RecyclerView (images/videos)
  - Topic tags in HorizontalScrollView
  - Action bar (upvote/downvote, reply, share buttons)
- **ViewHolder**: `PostViewHolder.kt` in `ui/common/` - handles binding and click events
- **Adapter**: `HomeAdapter.kt` uses `PostViewHolder` for rendering

### State Management
- **ViewModel**: Standard `ViewModel` with `MutableLiveData`/`LiveData`
- **Coroutines**: `viewModelScope.launch` for async operations
- **No Hilt/Dagger**: Manual repository instantiation in ViewModels

### Button Styling Pattern
- Action buttons use `LinearLayout` with `@drawable/button_background_light` background
- Icon size: 13-15dp with `app:tint="@color/text_tertiary"`
- Text size: 13sp with `@font/montserrat_regular`
- Height: 37-40dp, horizontal padding: 8-13dp

---

## 2. Implementation Tasks Breakdown

### Phase 1: Backend - DTOs & Core Services (Estimated: 3 hours)

- [x] **Task 1.1**: Create PostSummaryRequest DTO
  - **Details**: 
    - Create file: `Forumus-server/src/main/java/com/hcmus/forumus_backend/dto/post/PostSummaryRequest.java`
    - Fields: `private String postId;`
    - Add constructor, getters, setters
    - Follow same pattern as existing `PostIdRequest.java`
  - **Dependencies**: None
  - **Acceptance Criteria**: 
    - Class compiles without errors
    - Matches JSON structure: `{"postId": "POST_xxx"}`
  - **Time Estimate**: 15 minutes

- [x] **Task 1.2**: Create PostSummaryResponse DTO
  - **Details**:
    - Create file: `Forumus-server/src/main/java/com/hcmus/forumus_backend/dto/post/PostSummaryResponse.java`
    - Fields: `boolean success`, `String summary`, `String errorMessage`, `boolean cached`
    - Add constructor overloads: success case, error case
    - Add static factory methods: `success(String summary, boolean cached)`, `error(String message)`
  - **Dependencies**: None
  - **Acceptance Criteria**:
    - Serializes to expected JSON format
    - Factory methods simplify controller code
  - **Time Estimate**: 20 minutes

- [x] **Task 1.3**: Implement summarizePost method in PostService
  - **Details**:
    - Add method signature: `public PostSummaryResponse summarizePost(String postId)`
    - Fetch post from Firestore using existing `getPostById(String postId)` method
    - Return error response if post is null
    - Build Gemini prompt for summarization:
      ```java
      String prompt = """
          Please provide a concise summary (2-3 sentences, max 100 words) of this forum post.
          Focus on the main topic and key points. Be neutral and informative.
          
          Title: "%s"
          Content: "%s"
          
          Respond with ONLY the summary text, no JSON or formatting.
          """.formatted(title, content);
      ```
    - Call existing `askGemini(String prompt)` method
    - Wrap response in `PostSummaryResponse.success(summary, false)`
    - Handle exceptions with `PostSummaryResponse.error(e.getMessage())`
  - **Dependencies**: Task 1.2
  - **Acceptance Criteria**:
    - Returns summary for valid post ID
    - Returns error for invalid post ID
    - Handles Gemini API timeout gracefully (60 second timeout exists)
    - Logs request and response for debugging
  - **Time Estimate**: 1.5 hours

- [x] **Task 1.4**: Add summarize endpoint to PostController
  - **Details**:
    - Add to `Forumus-server/src/main/java/com/hcmus/forumus_backend/controller/PostController.java`
    - Endpoint mapping: `@PostMapping("/summarize")`
    - Full path: `POST /api/posts/summarize`
    - Request body: `@RequestBody PostSummaryRequest request`
    - Return type: `PostSummaryResponse`
    - Add logging: `System.out.println("Summarizing Post ID: " + request.getPostId());`
    - Match style of existing `validatePost` endpoint
  - **Dependencies**: Task 1.1, Task 1.3
  - **Acceptance Criteria**:
    - Endpoint accessible via Postman/curl
    - Returns 200 with JSON response
    - Logs request to console
    - No authentication required (matches existing endpoints)
  - **Time Estimate**: 30 minutes

- [x] **Task 1.5**: Test backend endpoint manually
  - **Details**:
    - Start Spring Boot server locally: `./mvnw spring-boot:run`
    - Test with curl or Postman:
      ```bash
      POST http://localhost:8081/api/posts/summarize
      Content-Type: application/json
      {"postId": "POST_20260125_143052_1234"}
      ```
    - Verify response structure matches DTO
    - Test error case with invalid post ID
    - Test with very long post content
  - **Dependencies**: Task 1.4
  - **Acceptance Criteria**:
    - Valid post returns summary within 30 seconds
    - Invalid post returns `{"success": false, "errorMessage": "Post not found"}`
    - No server crashes or unhandled exceptions
  - **Time Estimate**: 30 minutes

---

### Phase 2: Android - Data Layer (Estimated: 2.5 hours)

- [x] **Task 2.1**: Add SUMMARIZE_POST constant to ApiConstants
  - **Details**:
    - Edit file: `Forumus-client/app/src/main/java/com/hcmus/forumus_client/utils/ApiConstants.kt`
    - Add constant: `const val SUMMARIZE_POST = "api/posts/summarize"`
    - Place after existing `VALIDATE_POST` constant
  - **Dependencies**: Phase 1 complete
  - **Acceptance Criteria**:
    - Constant follows naming convention of existing constants
    - No compilation errors
  - **Time Estimate**: 5 minutes

- [x] **Task 2.2**: Create PostSummaryDto.kt with request and response classes
  - **Details**:
    - Create file: `Forumus-client/app/src/main/java/com/hcmus/forumus_client/data/dto/PostSummaryDto.kt`
    - Add `PostSummaryRequest` data class:
      ```kotlin
      @JsonClass(generateAdapter = true)
      data class PostSummaryRequest(
          @Json(name = "postId") val postId: String
      )
      ```
    - Add `PostSummaryResponse` data class:
      ```kotlin
      @JsonClass(generateAdapter = true)
      data class PostSummaryResponse(
          @Json(name = "success") val success: Boolean,
          @Json(name = "summary") val summary: String?,
          @Json(name = "errorMessage") val errorMessage: String?,
          @Json(name = "cached") val cached: Boolean = false
      )
      ```
    - Follow pattern of existing `PostValidationDto.kt`
  - **Dependencies**: None
  - **Acceptance Criteria**:
    - Classes compile without errors
    - Moshi adapters generated successfully
    - Fields match backend DTO exactly
  - **Time Estimate**: 15 minutes

- [x] **Task 2.3**: Add summarizePost method to ApiService interface
  - **Details**:
    - Edit file: `Forumus-client/app/src/main/java/com/hcmus/forumus_client/data/remote/ApiService.kt`
    - Add method after `validatePost`:
      ```kotlin
      @POST(ApiConstants.SUMMARIZE_POST)
      suspend fun summarizePost(
          @Body request: PostSummaryRequest
      ): Response<PostSummaryResponse>
      ```
    - Import new DTO classes
  - **Dependencies**: Task 2.1, Task 2.2
  - **Acceptance Criteria**:
    - Method signature matches existing API methods
    - No compilation errors
    - Follows suspend function pattern
  - **Time Estimate**: 10 minutes

- [x] **Task 2.4**: Add getPostSummary method to PostRepository
  - **Details**:
    - Edit file: `Forumus-client/app/src/main/java/com/hcmus/forumus_client/data/repository/PostRepository.kt`
    - Add method after `validatePost`:
      ```kotlin
      suspend fun getPostSummary(postId: String): Result<String> {
          return try {
              val request = PostSummaryRequest(postId)
              val response = NetworkService.apiService.summarizePost(request)
              
              if (response.isSuccessful && response.body()?.success == true) {
                  val summary = response.body()?.summary
                  if (summary != null) {
                      Result.success(summary)
                  } else {
                      Result.failure(Exception("Empty summary returned"))
                  }
              } else {
                  val errorMsg = response.body()?.errorMessage 
                      ?: "Failed to generate summary: ${response.code()}"
                  Result.failure(Exception(errorMsg))
              }
          } catch (e: Exception) {
              Log.e("PostRepository", "Error getting summary: ${e.message}")
              Result.failure(e)
          }
      }
      ```
    - Add import for `PostSummaryRequest`
  - **Dependencies**: Task 2.3
  - **Acceptance Criteria**:
    - Method follows existing `validatePost` pattern exactly
    - Returns `Result.success(summary)` on success
    - Returns `Result.failure(exception)` on any error
    - Logs errors for debugging
  - **Time Estimate**: 30 minutes

- [x] **Task 2.5**: Add import statements and verify compilation
  - **Details**:
    - Verify all imports are correct in modified files
    - Run Gradle sync: `./gradlew build` or Android Studio sync
    - Fix any compilation errors
  - **Dependencies**: Task 2.4
  - **Acceptance Criteria**:
    - Project compiles successfully
    - No unresolved imports
    - No type mismatches
  - **Time Estimate**: 15 minutes

---

### Phase 3: Android - UI Components (Estimated: 2.5 hours)

- [x] **Task 3.1**: Create ic_ai_sparkle.xml vector drawable
  - **Details**:
    - Create file: `Forumus-client/app/src/main/res/drawable/ic_ai_sparkle.xml`
    - Use Material Design "auto_awesome" icon or custom sparkle:
      ```xml
      <vector xmlns:android="http://schemas.android.com/apk/res/android"
          android:width="24dp"
          android:height="24dp"
          android:viewportWidth="24"
          android:viewportHeight="24">
          <path
              android:fillColor="#666666"
              android:pathData="M19,9l1.25,-2.75L23,5l-2.75,-1.25L19,1l-1.25,2.75L15,5l2.75,1.25L19,9zM11.5,9.5L9,4L6.5,9.5L1,12l5.5,2.5L9,20l2.5,-5.5L17,12L11.5,9.5zM19,15l-1.25,2.75L15,19l2.75,1.25L19,23l1.25,-2.75L23,19l-2.75,-1.25L19,15z"/>
      </vector>
      ```
  - **Dependencies**: None
  - **Acceptance Criteria**:
    - Icon displays correctly in Android Studio preview
    - Size matches other action icons (can scale to 15dp)
    - Supports tinting via `app:tint`
  - **Time Estimate**: 15 minutes

- [x] **Task 3.2**: Add SUMMARY action to PostAction enum
  - **Details**:
    - Find and edit PostAction enum (likely in `data/model/` or inline)
    - Search for: `enum class PostAction`
    - Add new value: `SUMMARY` after `SHARE`
    - Full enum should include: `OPEN, UPVOTE, DOWNVOTE, REPLY, SHARE, SUMMARY, AUTHOR_PROFILE, MENU`
  - **Dependencies**: None
  - **Acceptance Criteria**:
    - Enum compiles without errors
    - No impact on existing functionality
  - **Time Estimate**: 10 minutes

- [x] **Task 3.3**: Add Summary button to post_item.xml layout
  - **Details**:
    - Edit file: `Forumus-client/app/src/main/res/layout/post_item.xml`
    - Locate `replyShareContainer` LinearLayout (around line 195)
    - Add Summary button BEFORE Share button (inside `replyShareContainer`):
      ```xml
      <!-- AI Summary Button -->
      <LinearLayout
          android:id="@+id/summaryButton"
          android:layout_width="40dp"
          android:layout_height="40dp"
          android:gravity="center"
          android:background="@drawable/button_background_light"
          android:layout_marginEnd="8dp">
          <ImageView
              android:id="@+id/summaryIcon"
              android:layout_width="15dp"
              android:layout_height="15dp"
              android:src="@drawable/ic_ai_sparkle"
              app:tint="@color/text_tertiary"
              android:scaleType="fitCenter"/>
      </LinearLayout>
      
      <!-- Summary Loading Indicator -->
      <FrameLayout
          android:id="@+id/summaryLoadingContainer"
          android:layout_width="40dp"
          android:layout_height="40dp"
          android:visibility="gone"
          android:layout_marginEnd="8dp">
          <ProgressBar
              android:id="@+id/summaryLoading"
              android:layout_width="20dp"
              android:layout_height="20dp"
              android:layout_gravity="center"
              style="?android:attr/progressBarStyleSmall"/>
      </FrameLayout>
      ```
  - **Dependencies**: Task 3.1
  - **Acceptance Criteria**:
    - Button appears between Reply and Share buttons
    - Matches styling of adjacent Share button exactly
    - Loading indicator hidden by default
    - Layout preview shows correctly in Android Studio
  - **Time Estimate**: 30 minutes

- [x] **Task 3.4**: Add summary button bindings to PostViewHolder
  - **Details**:
    - Edit file: `Forumus-client/app/src/main/java/com/hcmus/forumus_client/ui/common/PostViewHolder.kt`
    - Add view bindings after `shareButton`:
      ```kotlin
      val summaryButton: LinearLayout = itemView.findViewById(R.id.summaryButton)
      val summaryLoadingContainer: FrameLayout = itemView.findViewById(R.id.summaryLoadingContainer)
      ```
    - Add click listener in `bind()` method after `shareButton.setOnClickListener`:
      ```kotlin
      summaryButton.setOnClickListener { onActionClick(post, PostAction.SUMMARY, it) }
      ```
    - Add helper method for loading state:
      ```kotlin
      fun setSummaryLoading(isLoading: Boolean) {
          summaryButton.visibility = if (isLoading) View.GONE else View.VISIBLE
          summaryLoadingContainer.visibility = if (isLoading) View.VISIBLE else View.GONE
      }
      ```
  - **Dependencies**: Task 3.2, Task 3.3
  - **Acceptance Criteria**:
    - Views bound without null exceptions
    - Click triggers `PostAction.SUMMARY`
    - Loading state toggles visibility correctly
  - **Time Estimate**: 30 minutes

- [x] **Task 3.5**: Create dialog_post_summary.xml layout
  - **Details**:
    - Create file: `Forumus-client/app/src/main/res/layout/dialog_post_summary.xml`
    - Layout structure:
      ```xml
      <?xml version="1.0" encoding="utf-8"?>
      <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
          xmlns:app="http://schemas.android.com/apk/res-auto"
          android:layout_width="match_parent"
          android:layout_height="wrap_content"
          android:orientation="vertical"
          android:padding="20dp"
          android:background="@color/surface">
      
          <!-- Header -->
          <LinearLayout
              android:layout_width="match_parent"
              android:layout_height="wrap_content"
              android:orientation="horizontal"
              android:gravity="center_vertical"
              android:layout_marginBottom="16dp">
      
              <ImageView
                  android:layout_width="24dp"
                  android:layout_height="24dp"
                  android:src="@drawable/ic_ai_sparkle"
                  app:tint="@color/primary"
                  android:layout_marginEnd="12dp"/>
      
              <TextView
                  android:layout_width="0dp"
                  android:layout_height="wrap_content"
                  android:layout_weight="1"
                  android:text="AI Summary"
                  android:textSize="18sp"
                  android:textColor="@color/text_primary"
                  android:fontFamily="@font/montserrat_semibold"/>
      
              <ImageButton
                  android:id="@+id/btnClose"
                  android:layout_width="32dp"
                  android:layout_height="32dp"
                  android:background="?attr/selectableItemBackgroundBorderless"
                  android:src="@drawable/ic_cancel"
                  app:tint="@color/text_tertiary"
                  android:contentDescription="Close"/>
          </LinearLayout>
      
          <!-- Summary Content -->
          <ScrollView
              android:layout_width="match_parent"
              android:layout_height="wrap_content"
              android:maxHeight="300dp">
      
              <TextView
                  android:id="@+id/tvSummaryContent"
                  android:layout_width="match_parent"
                  android:layout_height="wrap_content"
                  android:textSize="15sp"
                  android:textColor="@color/text_tertiary"
                  android:fontFamily="@font/montserrat_regular"
                  android:lineSpacingMultiplier="1.5"/>
          </ScrollView>
      
          <!-- Powered by label -->
          <TextView
              android:layout_width="match_parent"
              android:layout_height="wrap_content"
              android:layout_marginTop="16dp"
              android:text="Powered by Gemini AI"
              android:textSize="11sp"
              android:textColor="@color/text_secondary_light"
              android:fontFamily="@font/montserrat_regular"
              android:gravity="center"/>
      </LinearLayout>
      ```
  - **Dependencies**: Task 3.1
  - **Acceptance Criteria**:
    - Layout previews correctly
    - Scrollable for long summaries
    - Close button accessible
    - Matches app's visual style
  - **Time Estimate**: 30 minutes

---

### Phase 4: Android - ViewModel & Fragment Logic (Estimated: 3 hours)

- [x] **Task 4.1**: Add summary state properties to HomeViewModel
  - **Details**:
    - Edit file: `Forumus-client/app/src/main/java/com/hcmus/forumus_client/ui/home/HomeViewModel.kt`
    - Add LiveData properties after `_savePostResult`:
      ```kotlin
      // AI Summary state
      private val _summaryResult = MutableLiveData<Pair<String, Result<String>>?>()
      val summaryResult: LiveData<Pair<String, Result<String>>?> = _summaryResult
      
      private val _summaryLoadingPostId = MutableLiveData<String?>()
      val summaryLoadingPostId: LiveData<String?> = _summaryLoadingPostId
      ```
  - **Dependencies**: None
  - **Acceptance Criteria**:
    - Properties follow existing LiveData patterns
    - Pair contains postId and Result for proper tracking
    - No compilation errors
  - **Time Estimate**: 15 minutes

- [x] **Task 4.2**: Implement requestSummary method in HomeViewModel
  - **Details**:
    - Add method in `HomeViewModel.kt`:
      ```kotlin
      fun requestSummary(postId: String) {
          // Prevent duplicate requests
          if (_summaryLoadingPostId.value == postId) return
          
          viewModelScope.launch {
              _summaryLoadingPostId.value = postId
              try {
                  val result = postRepository.getPostSummary(postId)
                  _summaryResult.value = postId to result
              } catch (e: Exception) {
                  _summaryResult.value = postId to Result.failure(e)
              } finally {
                  _summaryLoadingPostId.value = null
              }
          }
      }
      
      fun clearSummaryResult() {
          _summaryResult.value = null
      }
      ```
  - **Dependencies**: Task 4.1, Phase 2 complete
  - **Acceptance Criteria**:
    - Prevents duplicate simultaneous requests
    - Sets loading state before API call
    - Clears loading state after completion
    - Handles both success and failure
  - **Time Estimate**: 30 minutes

- [x] **Task 4.3**: Update onPostAction to handle SUMMARY action
  - **Details**:
    - Edit `onPostAction` method in `HomeViewModel.kt`
    - Add case for SUMMARY:
      ```kotlin
      fun onPostAction(post: Post, postAction: PostAction) {
          when (postAction) {
              PostAction.UPVOTE -> handleVote(post, isUpvote = true)
              PostAction.DOWNVOTE -> handleVote(post, isUpvote = false)
              PostAction.SUMMARY -> requestSummary(post.id)
              else -> Unit
          }
      }
      ```
  - **Dependencies**: Task 4.2
  - **Acceptance Criteria**:
    - SUMMARY action triggers `requestSummary`
    - Other actions unchanged
    - No compilation errors
  - **Time Estimate**: 10 minutes

- [x] **Task 4.4**: Add summary observers in HomeFragment
  - **Details**:
    - Edit file: `Forumus-client/app/src/main/java/com/hcmus/forumus_client/ui/home/HomeFragment.kt`
    - Add observers in `onViewCreated` or appropriate lifecycle method:
      ```kotlin
      // Observe summary loading state for button UI
      viewModel.summaryLoadingPostId.observe(viewLifecycleOwner) { loadingPostId ->
          // Update adapter to show/hide loading indicators
          adapter.setSummaryLoadingPostId(loadingPostId)
      }
      
      // Observe summary result to show dialog
      viewModel.summaryResult.observe(viewLifecycleOwner) { result ->
          result?.let { (postId, summaryResult) ->
              summaryResult.onSuccess { summary ->
                  showSummaryDialog(summary)
              }.onFailure { error ->
                  showSummaryError(error.message ?: "Failed to generate summary")
              }
              viewModel.clearSummaryResult()
          }
      }
      ```
  - **Dependencies**: Task 4.3
  - **Acceptance Criteria**:
    - Observers registered in correct lifecycle
    - Success shows dialog
    - Failure shows error message
    - Result cleared after handling
  - **Time Estimate**: 30 minutes

- [x] **Task 4.5**: Implement showSummaryDialog method in HomeFragment
  - **Details**:
    - Add method in `HomeFragment.kt`:
      ```kotlin
      private fun showSummaryDialog(summary: String) {
          val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
          val view = layoutInflater.inflate(R.layout.dialog_post_summary, null)
          
          view.findViewById<TextView>(R.id.tvSummaryContent).text = summary
          view.findViewById<ImageButton>(R.id.btnClose).setOnClickListener {
              dialog.dismiss()
          }
          
          dialog.setContentView(view)
          dialog.show()
      }
      
      private fun showSummaryError(message: String) {
          Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
      }
      ```
  - **Dependencies**: Task 3.5, Task 4.4
  - **Acceptance Criteria**:
    - Dialog displays summary text
    - Close button dismisses dialog
    - Error shows as Toast
    - No crashes on configuration change
  - **Time Estimate**: 30 minutes

- [x] **Task 4.6**: Update HomeAdapter to handle loading state
  - **Details**:
    - Edit file: `Forumus-client/app/src/main/java/com/hcmus/forumus_client/ui/home/HomeAdapter.kt`
    - Add property and method:
      ```kotlin
      private var summaryLoadingPostId: String? = null
      
      fun setSummaryLoadingPostId(postId: String?) {
          val oldId = summaryLoadingPostId
          summaryLoadingPostId = postId
          
          // Notify items that need to update their loading state
          if (oldId != null) {
              val oldIndex = items.indexOfFirst { it.id == oldId }
              if (oldIndex >= 0) notifyItemChanged(oldIndex, "loading")
          }
          if (postId != null) {
              val newIndex = items.indexOfFirst { it.id == postId }
              if (newIndex >= 0) notifyItemChanged(newIndex, "loading")
          }
      }
      ```
    - Update `onBindViewHolder`:
      ```kotlin
      override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
          if (holder is PostViewHolder) {
              holder.bind(items[position], topicMap)
              holder.setSummaryLoading(items[position].id == summaryLoadingPostId)
          }
      }
      ```
  - **Dependencies**: Task 3.4
  - **Acceptance Criteria**:
    - Loading indicator shows on correct post
    - Indicator hides when loading completes
    - Uses efficient partial updates
  - **Time Estimate**: 45 minutes

---

### Phase 5: Apply Same Pattern to PostDetailFragment (Estimated: 1.5 hours)

- [ ] **Task 5.1**: Add summary state to PostDetailViewModel
  - **Details**:
    - Edit file: `Forumus-client/app/src/main/java/com/hcmus/forumus_client/ui/post/detail/PostDetailViewModel.kt`
    - Add same LiveData properties as HomeViewModel:
      ```kotlin
      private val _summaryResult = MutableLiveData<Result<String>?>()
      val summaryResult: LiveData<Result<String>?> = _summaryResult
      
      private val _isSummaryLoading = MutableLiveData<Boolean>(false)
      val isSummaryLoading: LiveData<Boolean> = _isSummaryLoading
      
      fun requestSummary() {
          val postId = currentPost?.id ?: return
          if (_isSummaryLoading.value == true) return
          
          viewModelScope.launch {
              _isSummaryLoading.value = true
              try {
                  val result = postRepository.getPostSummary(postId)
                  _summaryResult.value = result
              } finally {
                  _isSummaryLoading.value = false
              }
          }
      }
      
      fun clearSummaryResult() {
          _summaryResult.value = null
      }
      ```
  - **Dependencies**: Phase 2 complete
  - **Acceptance Criteria**:
    - Uses `currentPost` from existing ViewModel state
    - Matches HomeViewModel pattern
  - **Time Estimate**: 30 minutes

- [ ] **Task 5.2**: Add summary button to fragment_post_detail.xml (if applicable)
  - **Details**:
    - Check if post detail view uses same `post_item.xml` or has separate layout
    - If separate: Add summary button following same pattern as Task 3.3
    - If reuses `post_item.xml`: Button already included, skip this task
  - **Dependencies**: Task 3.3
  - **Acceptance Criteria**:
    - Summary button visible in post detail view
    - Consistent styling with home feed
  - **Time Estimate**: 20 minutes (if needed)

- [ ] **Task 5.3**: Add summary observers to PostDetailFragment
  - **Details**:
    - Edit file: `Forumus-client/app/src/main/java/com/hcmus/forumus_client/ui/post/detail/PostDetailFragment.kt`
    - Add observers and dialog methods (same as HomeFragment)
    - Wire up summary button click to `viewModel.requestSummary()`
  - **Dependencies**: Task 5.1, Task 5.2
  - **Acceptance Criteria**:
    - Summary works identically in detail view
    - Same dialog and error handling
  - **Time Estimate**: 30 minutes

---

### Phase 6: Testing & Quality Assurance (Estimated: 3 hours)

- [ ] **Task 6.1**: Test backend endpoint with various inputs
  - **Details**:
    - Test cases:
      1. Valid post ID → Returns summary
      2. Invalid/non-existent post ID → Returns error
      3. Post with empty content → Returns appropriate summary
      4. Post with very long content (5000+ chars) → Handles gracefully
      5. Rapid consecutive requests → No crashes
    - Document any issues found
  - **Dependencies**: Phase 1 complete
  - **Acceptance Criteria**:
    - All test cases pass
    - Response times under 30 seconds
    - No server exceptions in logs
  - **Time Estimate**: 45 minutes

- [ ] **Task 6.2**: Test Android API integration
  - **Details**:
    - Build and run app on emulator/device
    - Test API connectivity to backend
    - Verify request/response parsing
    - Check Logcat for any errors
  - **Dependencies**: Phase 2 complete, Task 6.1
  - **Acceptance Criteria**:
    - API calls succeed from Android
    - Response parsed correctly
    - No network errors on good connection
  - **Time Estimate**: 30 minutes

- [ ] **Task 6.3**: Test UI flow end-to-end
  - **Details**:
    - Test cases:
      1. Tap summary button → Loading shows → Dialog appears with summary
      2. Tap close button → Dialog dismisses
      3. Tap summary on different post → Correct summary shows
      4. Network error → Error toast appears
      5. Multiple rapid taps → Only one request made
      6. Rotate device during loading → No crash, state preserved
    - Test on both Home feed and Post Detail
  - **Dependencies**: Phase 4 complete
  - **Acceptance Criteria**:
    - All scenarios work as expected
    - No ANRs or crashes
    - UI responsive during loading
  - **Time Estimate**: 45 minutes

- [ ] **Task 6.4**: Test on multiple device configurations
  - **Details**:
    - Test on:
      - Small phone (5" display)
      - Large phone (6.5" display)
      - Tablet (if supported)
      - Dark mode
      - Different font sizes (accessibility)
    - Verify layout doesn't break
  - **Dependencies**: Task 6.3
  - **Acceptance Criteria**:
    - Button visible on all sizes
    - Dialog readable on all sizes
    - Dark mode colors correct
  - **Time Estimate**: 30 minutes

- [ ] **Task 6.5**: Performance testing
  - **Details**:
    - Test with slow network (throttle to 3G)
    - Verify timeout handling (>60 seconds)
    - Check memory usage in Android Profiler
    - Verify no memory leaks from dialogs
  - **Dependencies**: Task 6.3
  - **Acceptance Criteria**:
    - Graceful timeout after 60s
    - No memory leaks
    - App remains responsive
  - **Time Estimate**: 30 minutes

---

### Phase 7: Documentation & Cleanup (Estimated: 1 hour)

- [ ] **Task 7.1**: Add code comments and documentation
  - **Details**:
    - Add KDoc comments to new public methods
    - Document any non-obvious logic
    - Update any outdated comments
  - **Dependencies**: All implementation complete
  - **Acceptance Criteria**:
    - All public methods documented
    - Comments are accurate and helpful
  - **Time Estimate**: 30 minutes

- [ ] **Task 7.2**: Code cleanup and formatting
  - **Details**:
    - Remove any debug logging (or gate behind BuildConfig.DEBUG)
    - Format code consistently
    - Remove unused imports
    - Ensure no TODOs left unaddressed
  - **Dependencies**: Task 7.1
  - **Acceptance Criteria**:
    - Code passes linting
    - Consistent formatting throughout
    - No debug artifacts in production code
  - **Time Estimate**: 20 minutes

- [ ] **Task 7.3**: Update project documentation
  - **Details**:
    - Update README if needed
    - Document new API endpoint in API docs
    - Note any configuration requirements
  - **Dependencies**: Task 7.2
  - **Acceptance Criteria**:
    - Documentation accurate and complete
    - New feature discoverable by other developers
  - **Time Estimate**: 10 minutes

---

## Total Time Estimate Summary

| Phase | Description | Estimated Hours |
|-------|-------------|-----------------|
| Phase 1 | Backend - DTOs & Core Services | 3.0 |
| Phase 2 | Android - Data Layer | 2.5 |
| Phase 3 | Android - UI Components | 2.5 |
| Phase 4 | Android - ViewModel & Fragment Logic | 3.0 |
| Phase 5 | PostDetailFragment Integration | 1.5 |
| Phase 6 | Testing & Quality Assurance | 3.0 |
| Phase 7 | Documentation & Cleanup | 1.0 |
| **TOTAL** | | **16.5 hours** |

---

## Quick Reference: File Changes Summary

### New Files to Create
| File | Location |
|------|----------|
| `PostSummaryRequest.java` | `Forumus-server/src/main/java/com/hcmus/forumus_backend/dto/post/` |
| `PostSummaryResponse.java` | `Forumus-server/src/main/java/com/hcmus/forumus_backend/dto/post/` |
| `PostSummaryDto.kt` | `Forumus-client/app/src/main/java/com/hcmus/forumus_client/data/dto/` |
| `ic_ai_sparkle.xml` | `Forumus-client/app/src/main/res/drawable/` |
| `dialog_post_summary.xml` | `Forumus-client/app/src/main/res/layout/` |

### Files to Modify
| File | Changes |
|------|---------|
| `PostController.java` | Add `/summarize` endpoint |
| `PostService.java` | Add `summarizePost()` method |
| `ApiConstants.kt` | Add `SUMMARIZE_POST` constant |
| `ApiService.kt` | Add `summarizePost()` method |
| `PostRepository.kt` | Add `getPostSummary()` method |
| `PostAction.kt` (or enum location) | Add `SUMMARY` enum value |
| `post_item.xml` | Add summary button to action bar |
| `PostViewHolder.kt` | Bind summary button, handle loading |
| `HomeViewModel.kt` | Add summary request logic |
| `HomeFragment.kt` | Add observers, show dialog |
| `HomeAdapter.kt` | Handle loading state |
| `PostDetailViewModel.kt` | Add summary logic |
| `PostDetailFragment.kt` | Add observers, show dialog |

---

## 3. Dependency Graph

```
Phase 1 (Backend)
├── Task 1.1 (PostSummaryRequest DTO)
├── Task 1.2 (PostSummaryResponse DTO)
├── Task 1.3 (PostService.summarizePost) ─────────────┐
│   └── depends on: Task 1.2                          │
├── Task 1.4 (PostController endpoint) ───────────────┤
│   └── depends on: Task 1.1, Task 1.3                │
└── Task 1.5 (Manual testing)                         │
    └── depends on: Task 1.4                          │
                                                      │
Phase 2 (Android Data Layer)                          │
├── Task 2.1 (ApiConstants) ◄─────────────────────────┘
├── Task 2.2 (PostSummaryDto.kt)
├── Task 2.3 (ApiService.summarizePost)
│   └── depends on: Task 2.1, Task 2.2
├── Task 2.4 (PostRepository.getPostSummary)
│   └── depends on: Task 2.3
└── Task 2.5 (Verify compilation)
    └── depends on: Task 2.4

Phase 3 (Android UI Components) ─── Can start in parallel with Phase 2
├── Task 3.1 (ic_ai_sparkle.xml icon)
├── Task 3.2 (PostAction.SUMMARY enum)
├── Task 3.3 (post_item.xml button)
│   └── depends on: Task 3.1
├── Task 3.4 (PostViewHolder bindings)
│   └── depends on: Task 3.2, Task 3.3
└── Task 3.5 (dialog_post_summary.xml)
    └── depends on: Task 3.1

Phase 4 (Android ViewModel & Fragment)
├── Task 4.1 (HomeViewModel state properties)
├── Task 4.2 (HomeViewModel.requestSummary)
│   └── depends on: Task 4.1, Phase 2 complete
├── Task 4.3 (onPostAction SUMMARY case)
│   └── depends on: Task 4.2
├── Task 4.4 (HomeFragment observers)
│   └── depends on: Task 4.3
├── Task 4.5 (showSummaryDialog method)
│   └── depends on: Task 3.5, Task 4.4
└── Task 4.6 (HomeAdapter loading state)
    └── depends on: Task 3.4

Phase 5 (PostDetailFragment) ─── Parallel after Phase 4 patterns established
├── Task 5.1 (PostDetailViewModel)
├── Task 5.2 (Layout if separate)
└── Task 5.3 (PostDetailFragment observers)

Phase 6 (Testing) ─── After all implementation complete
├── Task 6.1 (Backend testing)
├── Task 6.2 (Android API testing)
├── Task 6.3 (E2E UI testing)
├── Task 6.4 (Device testing)
└── Task 6.5 (Performance testing)

Phase 7 (Documentation) ─── Final phase
├── Task 7.1 (Code comments)
├── Task 7.2 (Code cleanup)
└── Task 7.3 (Project documentation)
```

---

## 4. Critical Path

The minimum time to complete this feature (assuming no parallelization):

1. **Task 1.1** → **Task 1.2** → **Task 1.3** → **Task 1.4** → **Task 1.5** (3 hours)
2. **Task 2.1** → **Task 2.2** → **Task 2.3** → **Task 2.4** → **Task 2.5** (1.25 hours)
3. **Task 3.1** → **Task 3.3** → **Task 3.4** (1.25 hours)
4. **Task 4.1** → **Task 4.2** → **Task 4.3** → **Task 4.4** → **Task 4.5** → **Task 4.6** (2.5 hours)
5. **Task 6.1** → **Task 6.3** (1.5 hours)

**Critical Path Total: ~9.5 hours** (with parallelization possible for 16.5 hours of work)

---

## 5. Risk Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| Gemini API rate limits | High | Implement client-side debouncing, show "please wait" message |
| Gemini API timeout (>60s) | Medium | Already handled in existing code, ensure UI reflects timeout |
| Post content too long | Medium | Truncate content to first 5000 chars before sending to API |
| Summary quality poor | Low | Refine prompt, add user feedback mechanism in v2 |
| Loading state not clearing | Medium | Use `finally` block in all coroutine launches |
| Memory leak from dialog | Medium | Use weak references or ensure proper lifecycle handling |

---

## 6. Quick Start Checklist

For developers starting this implementation:

- [ ] Read through all tasks in Phase 1 before starting
- [ ] Set up local backend environment
- [ ] Verify Gemini API key is configured
- [ ] Create feature branch: `feature/ai-post-summary`
- [ ] Complete Phase 1 and verify with Postman
- [ ] Complete Phase 2 and verify in Logcat
- [ ] Complete Phase 3 and verify layout in Android Studio preview
- [ ] Complete Phase 4 and test on emulator
- [ ] Complete Phase 5 for Post Detail screen
- [ ] Run full test suite (Phase 6)
- [ ] Create PR with before/after screenshots

