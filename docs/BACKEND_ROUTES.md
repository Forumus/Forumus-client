# Forumus Backend API Documentation

**Internal Forum for University Members**  
Spring Boot Backend for Android Kotlin Mobile Application

---

## Table of Contents

1. [API Endpoints Reference](#api-endpoints-reference)
2. [Phase 1: Core Features](#phase-1-core-features)
3. [Phase 2: Email Services](#phase-2-email-services)
4. [Phase 3: Post Management and AI Features](#phase-3-post-management-and-ai-features)
5. [Phase 4: Notification System](#phase-4-notification-system)
6. [Phase 5: Topic Management and Real-time Listeners](#phase-5-topic-management-and-real-time-listeners)
7. [System Architecture](#system-architecture)
8. [Technology Stack](#technology-stack)

---

## API Endpoints Reference

The following table provides a comprehensive overview of all available API endpoints in the Forumus Backend system.

| No. | Feature              | Method | Endpoint                        | Category       | Description                                         |
| --- | -------------------- | ------ | ------------------------------- | -------------- | --------------------------------------------------- |
| 1   | Health Check         | GET    | `/api/health`                   | Core           | Verifies server operational status                  |
| 2   | Password Reset       | POST   | `/api/auth/resetPassword`       | Authentication | Resets user password via Firebase Auth (admin only) |
| 3   | Send OTP Email       | POST   | `/api/email/send-otp`           | Email          | Sends OTP verification code to user email           |
| 4   | Send Welcome Email   | POST   | `/api/email/send-welcome`       | Email          | Sends welcome email after account verification      |
| 5   | Send Report Email    | POST   | `/api/email/send-report`        | Email          | Sends account status notification email             |
| 6   | Ask Gemini AI        | POST   | `/api/posts/askGemini`          | AI             | General Q&A interaction with Gemini AI              |
| 7   | Validate Post        | POST   | `/api/posts/validatePost`       | AI             | AI-powered content moderation for posts             |
| 8   | Summarize Post       | POST   | `/api/posts/summarize`          | AI             | Generates AI summary with intelligent caching       |
| 9   | Get Suggested Topics | POST   | `/api/posts/getSuggestedTopics` | AI             | Extracts relevant topics using AI analysis          |
| 10  | Trigger Notification | POST   | `/api/notifications`            | Notifications  | Sends push notification and stores in Firestore     |
| 11  | Get All Topics       | GET    | `/api/topics/getAll`            | Topics         | Retrieves all forum topics from cache               |
| 12  | Add Topics           | POST   | `/api/topics/add`               | Topics         | Adds new topics to the forum                        |

---

## Phase 1: Core Features

### 1. Health Check

**Endpoint:** `GET /api/health`

**Description:**  
A health check endpoint to verify that the Forumus Backend server is running and operational. This endpoint is used for monitoring, load balancer health probes, and deployment verification.

**Response Structure:**

| Field   | Type   | Description                   |
| ------- | ------ | ----------------------------- |
| status  | String | Server status indicator (UP)  |
| message | String | Human-readable status message |

```mermaid
sequenceDiagram
    participant Client as Android App
    participant Server as Spring Boot Server

    Client->>Server: GET /api/health
    Server-->>Client: 200 OK with status response
```

---

### 2. Authentication - Password Reset

**Endpoint:** `POST /api/auth/resetPassword`

**Description:**  
Allows administrators to reset a user's password in Firebase Authentication. This is a secure endpoint that requires a secret admin key for authorization.

**Request Parameters:**

| Field       | Type   | Required | Description                        |
| ----------- | ------ | -------- | ---------------------------------- |
| email       | String | Yes      | User's email address               |
| newPassword | String | Yes      | New password to set                |
| secretKey   | String | Yes      | Admin secret key for authorization |

**Response Parameters:**

| Field   | Type    | Description                         |
| ------- | ------- | ----------------------------------- |
| success | Boolean | Operation result status             |
| message | String  | Result message or error description |

**Security Requirements:**

- Requires valid secretKey matching server configuration
- Returns 403 Forbidden if secret key is invalid
- Returns 400 Bad Request if email or password is missing

```mermaid
sequenceDiagram
    participant Client as Android App
    participant Controller as AuthController
    participant Firebase as Firebase Auth

    Client->>Controller: POST /api/auth/resetPassword
    Controller->>Controller: Validate Secret Key
    alt Invalid Secret Key
        Controller-->>Client: 403 Forbidden
    else Valid Secret Key
        Controller->>Controller: Validate Input
        alt Missing Fields
            Controller-->>Client: 400 Bad Request
        else Valid Input
            Controller->>Firebase: getUserByEmail()
            alt User Not Found
                Firebase-->>Controller: FirebaseAuthException
                Controller-->>Client: 500 Error
            else User Found
                Controller->>Firebase: updateUser(password)
                alt Update Success
                    Firebase-->>Controller: Success
                    Controller-->>Client: 200 OK - Password updated
                else Update Failure
                    Firebase-->>Controller: FirebaseAuthException
                    Controller-->>Client: 500 Error
                end
            end
        end
    end
```

---

## Phase 2: Email Services

### 3. Send OTP Email

**Endpoint:** `POST /api/email/send-otp`

**Description:**  
Sends a One-Time Password (OTP) verification email to a user during registration or account verification. The email contains a styled HTML template with the OTP code.

**Request Parameters:**

| Field          | Type   | Required | Description               |
| -------------- | ------ | -------- | ------------------------- |
| recipientEmail | String | Yes      | Recipient's email address |
| otpCode        | String | Yes      | OTP code to send          |

**Response Parameters:**

| Field   | Type    | Description           |
| ------- | ------- | --------------------- |
| success | Boolean | Email delivery status |
| message | String  | Result message        |

```mermaid
sequenceDiagram
    participant App as Android App
    participant Server as Spring Boot
    participant Mail as SMTP Server
    participant User as User Inbox

    App->>Server: POST /api/email/send-otp
    Server->>Server: Validate email format
    Server->>Server: Generate HTML template
    Server->>Mail: Send MIME message
    Mail->>User: Deliver OTP email
    Server-->>App: 200 OK
```

---

### 4. Send Welcome Email

**Endpoint:** `POST /api/email/send-welcome`

**Description:**  
Sends a welcome email to new users after successful account verification. This email welcomes users to the Forumus community and provides information about getting started.

**Request Parameters:**

| Field          | Type   | Required | Description               |
| -------------- | ------ | -------- | ------------------------- |
| recipientEmail | String | Yes      | Recipient's email address |
| userName       | String | Yes      | User's display name       |

**Response Parameters:**

| Field   | Type    | Description           |
| ------- | ------- | --------------------- |
| success | Boolean | Email delivery status |
| message | String  | Result message        |

```mermaid
sequenceDiagram
    participant App as Android App
    participant Controller as EmailController
    participant Service as EmailService
    participant Mail as SMTP Server
    participant User as User Inbox

    App->>Controller: POST /api/email/send-welcome
    Controller->>Controller: Validate email format
    Controller->>Service: sendWelcomeEmail()
    Service->>Service: Generate HTML template
    Service->>Mail: Send MIME message
    Mail->>User: Deliver welcome email
    Mail-->>Service: Delivery confirmed
    Service-->>Controller: Success
    Controller-->>App: 200 OK - Email sent
```

---

### 5. Send Report Email

**Endpoint:** `POST /api/email/send-report`

**Description:**  
Sends an account status report email when a user's account status changes. This email includes details about reported posts and the reason for the status change.

**Request Parameters:**

| Field          | Type   | Required | Description                                  |
| -------------- | ------ | -------- | -------------------------------------------- |
| recipientEmail | String | Yes      | Recipient's email address                    |
| userName       | String | Yes      | User's display name                          |
| userStatus     | String | Yes      | New account status                           |
| reportedPosts  | Array  | No       | List of reported posts with title and reason |

**User Status Values:**

| Status     | Description                       |
| ---------- | --------------------------------- |
| ACTIVE     | Normal account status             |
| WARNING    | Account has received a warning    |
| RESTRICTED | Account has limited functionality |
| BANNED     | Account is suspended              |

**Response Parameters:**

| Field   | Type    | Description           |
| ------- | ------- | --------------------- |
| success | Boolean | Email delivery status |
| message | String  | Result message        |

```mermaid
sequenceDiagram
    participant App as Android App
    participant Controller as EmailController
    participant Service as EmailService
    participant Mail as SMTP Server
    participant User as User Inbox

    App->>Controller: POST /api/email/send-report
    Controller->>Controller: Validate Input
    alt Invalid UserStatus
        Controller-->>App: 400 Bad Request
    else Valid
        Controller->>Service: sendReportEmail()
        Service->>Service: Generate Report HTML
        Service->>Service: Include Reported Posts
        Service->>Mail: Send MIME Message
        Mail->>User: Deliver Report Email
        Mail-->>Service: Delivery Confirmed
        Service-->>Controller: Success
        Controller-->>App: 200 OK - Email sent
    end
```

---

### Email Service Architecture

```mermaid
flowchart TB
    subgraph "Email Controller"
        A1[send-otp]
        A2[send-welcome]
        A3[send-report]
    end

    subgraph "Email Service"
        B1[Validate Email Format]
        B2[Create MIME Message]
        B3[Generate HTML Template]
    end

    subgraph "Templates"
        C1[OTP Template]
        C2[Welcome Template]
        C3[Report Template]
    end

    subgraph "External"
        D[JavaMailSender]
        E[SMTP Server]
    end

    A1 --> B1 --> B3 --> C1
    A2 --> B1 --> B3 --> C2
    A3 --> B1 --> B3 --> C3

    C1 --> B2
    C2 --> B2
    C3 --> B2

    B2 --> D --> E
```

---

## Phase 3: Post Management and AI Features

This phase covers AI-powered features using the Google Gemini 2.5 Flash model for content moderation, summarization, and topic suggestion.

### 6. Ask Gemini AI

**Endpoint:** `POST /api/posts/askGemini`

**Description:**  
A general-purpose endpoint for interaction with Gemini AI. Sends a question or prompt to the AI and returns the response. The AI is configured as a helpful assistant for an academic forum.

**Request Parameters:**

| Field  | Type   | Required | Description                   |
| ------ | ------ | -------- | ----------------------------- |
| (body) | String | Yes      | Plain text question or prompt |

**Response Parameters:**

| Field    | Type   | Description           |
| -------- | ------ | --------------------- |
| response | String | AI-generated response |

**AI System Configuration:**
The AI operates with the following system instruction: "You are a helpful assistant for an academic forum. You should help ensure that posts adhere to community guidelines suitable for university students. You should answer in the language of the user's prompt."

```mermaid
sequenceDiagram
    participant App as Android App
    participant Server as Spring Boot
    participant Gemini as Gemini AI API

    App->>Server: POST /api/posts/askGemini
    Server->>Server: Build prompt with system instruction
    Server->>Gemini: Generate Content Request
    Gemini-->>Server: AI Response
    Server->>Server: Extract text from response
    Server-->>App: Response with AI answer
```

---

### 7. Post Validation (AI Content Moderation)

**Endpoint:** `POST /api/posts/validatePost`

**Description:**  
Validates a forum post using AI to ensure adherence to community guidelines. The system fetches the post from Firestore, sends it to Gemini AI for analysis, and automatically updates the post status. If rejected, a push notification is sent to the author.

**Request Parameters:**

| Field  | Type   | Required | Description                |
| ------ | ------ | -------- | -------------------------- |
| postId | String | Yes      | ID of the post to validate |

**Response Parameters:**

| Field   | Type    | Description                                  |
| ------- | ------- | -------------------------------------------- |
| valid   | Boolean | Validation result                            |
| message | String  | Rejection reasons if invalid, empty if valid |

**Validation Criteria:**

- No offensive language or hate speech
- No personal attacks
- No inappropriate content
- Suitable for university students
- Respects diversity and inclusion
- Relevant to academic topics

```mermaid
sequenceDiagram
    participant App as Android App
    participant Controller as PostController
    participant Service as PostService
    participant Firestore as Firestore DB
    participant Gemini as Gemini AI
    participant NotifSvc as NotificationService

    App->>Controller: POST /api/posts/validatePost
    Controller->>Service: getPostById(postId)
    Service->>Firestore: Get post document
    alt Post Not Found
        Firestore-->>Service: null
        Service-->>Controller: null
        Controller-->>App: Post not found
    else Post Found
        Firestore-->>Service: Post data
        Service->>Service: Build validation prompt
        Service->>Gemini: generateContent(prompt)
        Gemini-->>Service: AI analysis result
        Service->>Service: Parse JSON response
        alt Valid Post
            Service->>Firestore: Update status: APPROVED
            Service-->>Controller: valid=true
            Controller-->>App: 200 OK - Post approved
        else Invalid Post
            Service->>Firestore: Update status: REJECTED
            Service->>NotifSvc: triggerNotification(POST_REJECTED)
            NotifSvc->>Firestore: Save notification
            NotifSvc->>NotifSvc: Send FCM push
            Service-->>Controller: valid=false + reasons
            Controller-->>App: 200 OK - Post rejected
        end
    end
```

**Automatic Post Listener:**  
Posts are also validated automatically when added to Firestore with PENDING status via a real-time listener.

```mermaid
flowchart TD
    subgraph "Android App"
        A[User Creates Post]
    end

    subgraph "Firestore"
        B[(posts collection)]
    end

    subgraph "Spring Boot Server"
        C[PostListener]
        D[PostService.validatePost]
        E{Validation Result}
        F[Update Status: APPROVED]
        G[Update Status: REJECTED]
        H[NotificationService]
    end

    subgraph "Firebase FCM"
        I[Push Notification]
    end

    A -->|status: PENDING| B
    B -->|Real-time trigger| C
    C --> D
    D --> E
    E -->|Valid| F
    E -->|Invalid| G
    F -->|APPROVED| B
    G -->|REJECTED| B
    G --> H
    H --> I
```

---

### 8. Post Summarization (AI-Powered with Caching)

**Endpoint:** `POST /api/posts/summarize`

**Description:**  
Generates a concise AI-powered summary (2-3 sentences, maximum 100 words) of a forum post. The system implements intelligent caching using content hashing to detect changes and only regenerates summaries when content is modified.

**Request Parameters:**

| Field  | Type   | Required | Description                 |
| ------ | ------ | -------- | --------------------------- |
| postId | String | Yes      | ID of the post to summarize |

**Response Parameters (Success):**

| Field       | Type    | Description                               |
| ----------- | ------- | ----------------------------------------- |
| success     | Boolean | Operation status                          |
| summary     | String  | Generated summary text                    |
| fromCache   | Boolean | Whether summary was retrieved from cache  |
| contentHash | String  | Hash of post content for cache validation |
| generatedAt | Long    | Timestamp of summary generation           |

**Response Parameters (Error):**

| Field   | Type    | Description       |
| ------- | ------- | ----------------- |
| success | Boolean | false             |
| message | String  | Error description |

```mermaid
sequenceDiagram
    participant App as Android App
    participant Controller as PostController
    participant Service as PostService
    participant Cache as SummaryCacheService
    participant Firestore as Firestore DB
    participant Gemini as Gemini AI

    App->>Controller: POST /api/posts/summarize
    Controller->>Service: summarizePost(postId)
    Service->>Firestore: Get post document
    alt Post Not Found
        Firestore-->>Service: null
        Service-->>Controller: Error response
        Controller-->>App: Post not found
    else Post Found
        Firestore-->>Service: Post data
        Service->>Service: Compute content hash
        Service->>Cache: get(postId, contentHash)
        alt Cache HIT
            Cache-->>Service: Cached summary
            Service-->>Controller: Summary (fromCache=true)
            Controller-->>App: 200 OK - Cached summary
        else Cache MISS
            Cache-->>Service: null
            Service->>Service: Truncate content (max 5000 chars)
            Service->>Gemini: generateContent(prompt)
            Gemini-->>Service: AI-generated summary
            Service->>Service: Clean response
            Service->>Cache: put(postId, summary, hash)
            Service-->>Controller: Summary (fromCache=false)
            Controller-->>App: 200 OK - New summary
        end
    end
```

**Caching Features:**

| Feature           | Description                                         |
| ----------------- | --------------------------------------------------- |
| Content Hash      | SHA-256 hash of title and content to detect changes |
| TTL Support       | Time-to-live for cache entries                      |
| Hit Tracking      | Tracks cache hits for monitoring                    |
| Thread Safety     | Uses ConcurrentHashMap                              |
| Auto-invalidation | Cache invalidates when content changes              |

---

### 9. Get Suggested Topics (AI Topic Extraction)

**Endpoint:** `POST /api/posts/getSuggestedTopics`

**Description:**  
Uses Gemini AI to analyze a post's title and content, then suggests up to 3 relevant topics from existing topics in the database.

**Request Parameters:**

| Field   | Type   | Required | Description  |
| ------- | ------ | -------- | ------------ |
| title   | String | Yes      | Post title   |
| content | String | Yes      | Post content |

**Response Parameters (Success):**

| Field   | Type    | Description                           |
| ------- | ------- | ------------------------------------- |
| success | Boolean | Operation status                      |
| topics  | Array   | List of matched TopicResponse objects |

**TopicResponse Structure:**

| Field       | Type   | Description             |
| ----------- | ------ | ----------------------- |
| topicId     | String | Unique topic identifier |
| name        | String | Topic display name      |
| description | String | Topic description       |

```mermaid
sequenceDiagram
    participant App as Android App
    participant Controller as PostController
    participant Service as PostService
    participant Listener as TopicsListener
    participant Gemini as Gemini AI

    App->>Controller: POST /api/posts/getSuggestedTopics
    Controller->>Service: extractTopics(title, content)
    Service->>Listener: getAllTopics()
    Listener-->>Service: List of all topics
    Service->>Service: Build topic list string
    Service->>Service: Create AI prompt
    Service->>Gemini: generateContent(prompt)
    Gemini-->>Service: JSON response with topics
    Service->>Service: Parse JSON response
    Service->>Service: Filter topics by name match
    Service->>Service: Limit to max 3 topics
    Service-->>Controller: Matched topics
    Controller-->>App: 200 OK - Suggested topics
```

---

### Post Service Architecture

```mermaid
flowchart TB
    subgraph "PostController"
        A1["/askGemini"]
        A2["/validatePost"]
        A3["/summarize"]
        A4["/getSuggestedTopics"]
    end

    subgraph "PostService"
        B1[askGemini]
        B2[validatePost]
        B3[summarizePost]
        B4[extractTopics]
        B5[getPostById]
        B6[updatePostStatus]
    end

    subgraph "External Services"
        C1[Gemini AI Client]
        C2[Firestore DB]
        C3[SummaryCacheService]
        C4[TopicsListener]
        C5[NotificationService]
    end

    A1 --> B1 --> C1
    A2 --> B2 --> C1
    A2 --> B5 --> C2
    A2 --> B6 --> C2
    A2 -.-> C5
    A3 --> B3 --> C1
    A3 --> B3 --> C3
    A4 --> B4 --> C1
    A4 --> B4 --> C4
```

---

## Phase 4: Notification System

This phase covers the push notification infrastructure using Firebase Cloud Messaging (FCM) for real-time user notifications with Firestore persistence.

### 10. Trigger Notification

**Endpoint:** `POST /api/notifications`

**Description:**  
Triggers a push notification to a specific user. The system stores the notification in Firestore under the user's notifications subcollection and sends a push notification via FCM if the user has a valid FCM token.

**Request Parameters:**

| Field               | Type   | Required | Description                         |
| ------------------- | ------ | -------- | ----------------------------------- |
| type                | String | Yes      | Notification type (see types below) |
| actorId             | String | No       | ID of user who triggered the action |
| actorName           | String | No       | Display name of actor               |
| targetId            | String | No       | Post ID or Comment ID               |
| targetUserId        | String | Yes      | User to receive notification        |
| previewText         | String | No       | Preview snippet of content          |
| originalPostTitle   | String | No       | For rejection notifications         |
| originalPostContent | String | No       | For rejection notifications         |
| rejectionReason     | String | No       | Reason for post rejection           |

**Response:**

- Success: "Notification triggered successfully"
- Error: "Failed to trigger notification"

```mermaid
sequenceDiagram
    participant App as Android App
    participant Controller as NotificationController
    participant Service as NotificationService
    participant UserSvc as UserService
    participant Firestore as Firestore DB
    participant FCM as Firebase FCM

    App->>Controller: POST /api/notifications
    Controller->>Service: triggerNotification(request)
    Service->>Service: Validate input
    alt Missing targetUserId
        Service-->>Controller: Failed
        Controller-->>App: Failed to trigger notification
    else Actor equals Target User
        Service-->>Controller: Success (skipped)
        Controller-->>App: Notification triggered successfully
    else Valid Request
        Service->>UserSvc: getUserById(targetUserId)
        alt User Not Found
            UserSvc-->>Service: null
            Service-->>Controller: Failed
            Controller-->>App: Failed to trigger notification
        else User Found
            UserSvc-->>Service: User data
            Service->>Service: Generate notification ID
            Service->>Service: Prepare notification data
            Service->>Firestore: Write to users/{userId}/notifications/{id}
            alt User has FCM Token
                Service->>Service: Generate title and body
                Service->>FCM: Send push notification
                FCM-->>Service: Push sent
            else No FCM Token
                Service->>Service: Skip push notification
            end
            Service-->>Controller: Success
            Controller-->>App: Notification triggered successfully
        end
    end
```

---

### Notification Types

| Type           | Title                 | Body Template                              | Use Case                    |
| -------------- | --------------------- | ------------------------------------------ | --------------------------- |
| UPVOTE         | New Upvote            | {actor} upvoted your post: {preview}       | User upvotes a post         |
| COMMENT        | New Comment           | {actor} commented on your post: {preview}  | User comments on a post     |
| REPLY          | New Reply             | {actor} replied to your comment: {preview} | User replies to a comment   |
| POST_REJECTED  | Post Rejected         | {previewText}                              | AI rejects a post           |
| POST_APPROVED  | Post Approved         | {previewText}                              | AI approves a post          |
| POST_DELETED   | Post Removed          | {previewText}                              | Admin removes a post        |
| STATUS_CHANGED | Account Status Update | {previewText}                              | User account status changes |

**Trigger Sources:**

| Type           | Trigger Source                | Auto-Generated |
| -------------- | ----------------------------- | -------------- |
| UPVOTE         | Android App                   | No             |
| COMMENT        | Android App                   | No             |
| REPLY          | Android App                   | No             |
| POST_REJECTED  | PostListener / PostController | Yes            |
| POST_APPROVED  | PostController                | Yes            |
| POST_DELETED   | Admin Action                  | No             |
| STATUS_CHANGED | Admin Action                  | No             |

---

### Notification Data Structure (Firestore)

**Path:** `users/{targetUserId}/notifications/{notificationId}`

| Field               | Type      | Description                    |
| ------------------- | --------- | ------------------------------ |
| id                  | String    | Unique notification identifier |
| type                | String    | Notification type              |
| actorId             | String    | User who triggered the action  |
| actorName           | String    | Display name of actor          |
| targetId            | String    | Post ID or Comment ID          |
| previewText         | String    | Content preview snippet        |
| createdAt           | Timestamp | Creation timestamp             |
| isRead              | Boolean   | Read status                    |
| originalPostTitle   | String    | Optional - for rejections      |
| originalPostContent | String    | Optional - for rejections      |
| rejectionReason     | String    | Optional - for rejections      |

---

### FCM Push Notification Payload Structure

| Field          | Path         | Description                   |
| -------------- | ------------ | ----------------------------- |
| token          | root         | User's FCM token              |
| title          | notification | Notification title            |
| body           | notification | Notification body text        |
| type           | data         | general_notification          |
| notificationId | data         | UUID of notification          |
| targetId       | data         | Post/Comment ID for deep link |
| click_action   | data         | FLUTTER_NOTIFICATION_CLICK    |

---

### Integration with Post Validation

```mermaid
sequenceDiagram
    participant Firestore as Firestore
    participant Listener as PostListener
    participant PostSvc as PostService
    participant NotifSvc as NotificationService
    participant FCM as Firebase FCM
    participant App as Android App

    Firestore->>Listener: New Post Added (PENDING)
    Listener->>PostSvc: validatePost(title, content)
    PostSvc->>PostSvc: Call Gemini AI

    alt Post Valid
        PostSvc->>Firestore: Update status: APPROVED
    else Post Invalid
        PostSvc->>Firestore: Update status: REJECTED
        Listener->>NotifSvc: triggerNotification(POST_REJECTED)
        NotifSvc->>Firestore: Save notification
        NotifSvc->>FCM: Send push notification
        FCM->>App: Display Post Rejected notification
    end
```

---

### Notification Service Architecture

```mermaid
flowchart TB
    subgraph "Triggers"
        A1[NotificationController]
        A2[PostController]
        A3[PostListener]
    end

    subgraph "NotificationService"
        B1[triggerNotification]
        B2[generateNotificationTitle]
        B3[generateNotificationBody]
    end

    subgraph "Dependencies"
        C1[UserService - Get FCM Token]
        C2[FCMService - Send Push]
        C3[Firestore - Store Notification]
    end

    subgraph "Output"
        D1[Push Notification]
        D2[Firestore Record]
    end

    A1 --> B1
    A2 --> B1
    A3 --> B1

    B1 --> C1
    B1 --> B2
    B1 --> B3
    B2 --> C2
    B3 --> C2
    B1 --> C3

    C2 --> D1
    C3 --> D2
```

---

### Self-Action Prevention

The notification system skips notifications when a user performs an action on their own content.

```mermaid
flowchart LR
    A[User A upvotes User A's post] --> B{actorId == targetUserId?}
    B -->|Yes| C[Skip Notification]
    B -->|No| D[Send Notification]
```

---

## Phase 5: Topic Management and Real-time Listeners

This phase covers topic management APIs and real-time Firestore listeners for automatic post validation and topic caching.

### 11. Get All Topics

**Endpoint:** `GET /api/topics/getAll`

**Description:**  
Retrieves all available forum topics from the in-memory cache. Topics are pre-loaded at server startup and kept synchronized with Firestore via a real-time listener.

**Response Parameters (Success):**

| Field   | Type    | Description                   |
| ------- | ------- | ----------------------------- |
| success | Boolean | Operation status              |
| topics  | Array   | List of TopicResponse objects |

**TopicResponse Structure:**

| Field       | Type   | Description             |
| ----------- | ------ | ----------------------- |
| topicId     | String | Unique topic identifier |
| name        | String | Topic display name      |
| description | String | Topic description       |

```mermaid
sequenceDiagram
    participant App as Android App
    participant Server as Spring Boot
    participant Cache as TopicsListener Cache
    participant Firestore as Firestore

    Note over Cache,Firestore: Server Startup
    Firestore->>Cache: Load all topics
    Cache->>Cache: Store in ConcurrentHashMap

    Note over App,Server: API Request
    App->>Server: GET /api/topics/getAll
    Server->>Cache: getAllTopics()
    Cache-->>Server: List of TopicResponse
    Server-->>App: Response with topics
```

---

### 12. Add Topics

**Endpoint:** `POST /api/topics/add`

**Description:**  
Adds one or more new topics to the forum. Topics are stored in Firestore with auto-generated IDs based on the topic name (lowercase with underscores).

**Request Parameters:**
Array of topic objects:

| Field       | Type   | Required | Description        |
| ----------- | ------ | -------- | ------------------ |
| name        | String | Yes      | Topic display name |
| description | String | No       | Topic description  |

**Response Parameters:**

| Field   | Type    | Description      |
| ------- | ------- | ---------------- |
| success | Boolean | Operation status |

**Topic ID Generation:**

- Input: "Machine Learning"
- Output: "machine_learning"

```mermaid
sequenceDiagram
    participant App as Android App
    participant Controller as TopicController
    participant Service as TopicService
    participant Firestore as Firestore DB
    participant Listener as TopicsListener

    App->>Controller: POST /api/topics/add
    Controller->>Service: addTopic(topicRequests)
    loop For each topic request
        Service->>Service: Generate topic ID (lowercase + underscores)
        alt Valid ID
            Service->>Service: Create TopicResponse
            Service->>Firestore: Write to topics/{topicId}
            Firestore-->>Service: Write successful
            Note over Firestore,Listener: Real-time listener detects change
            Firestore->>Listener: Document ADDED event
            Listener->>Listener: Update cache
        else Empty ID
            Service->>Service: Skip topic
        end
    end
    Service-->>Controller: Success
    Controller-->>App: 200 OK - Topics added
```

---

### Real-time Topics Listener

**Component:** TopicsListener

**Description:**  
A Spring component that maintains an in-memory cache of all topics, synchronized with Firestore in real-time.

**Features:**

| Feature        | Description                                 |
| -------------- | ------------------------------------------- |
| Fast Startup   | Pre-loads all topics into cache             |
| Real-time Sync | Listens for ADDED, MODIFIED, REMOVED events |
| Thread-safe    | Uses ConcurrentHashMap                      |
| Zero Latency   | API calls served from memory                |

```mermaid
flowchart TB
    subgraph "Server Startup"
        A[Spring Boot Starts] --> B[PostConstruct initialize]
        B --> C[Fetch all topics from Firestore]
        C --> D[Populate ConcurrentHashMap]
        D --> E[Start Snapshot Listener]
    end

    subgraph "Real-time Updates"
        F[(Firestore topics)] -->|ADDED| G[Add to Cache]
        F -->|MODIFIED| H[Update in Cache]
        F -->|REMOVED| I[Remove from Cache]
    end

    subgraph "API Requests"
        J[GET /api/topics/getAll] --> K[Read from Cache]
        L[POST /api/posts/getSuggestedTopics] --> K
    end

    E --> F
    G --> K
    H --> K
    I --> K
```

---

### Real-time Post Listener

**Component:** PostListener

**Description:**  
A Spring component that listens for new posts in Firestore and automatically triggers AI validation for posts with PENDING status.

**Features:**

| Feature           | Description                                   |
| ----------------- | --------------------------------------------- |
| Auto-validation   | New PENDING posts are automatically validated |
| Auto-notification | Rejected posts trigger user notifications     |
| Skip Initial      | Ignores existing posts on startup             |
| Status Update     | Automatically updates post status             |

```mermaid
flowchart TD
    subgraph "Android App"
        A[User Creates Post]
    end

    subgraph "Firestore"
        B[(posts collection)]
    end

    subgraph "PostListener"
        C{Is Initial Snapshot?}
        D{Document Change Type}
        E[handleNewPost]
    end

    subgraph "Validation Flow"
        F{Status == PENDING?}
        G{Title and Content Valid?}
        H[PostService.validatePost]
        I[Update: APPROVED]
        J[Update: REJECTED]
        K[Trigger Notification]
    end

    A -->|status: PENDING| B
    B -->|Snapshot Event| C
    C -->|Yes| L[Skip]
    C -->|No| D
    D -->|ADDED| E
    D -->|MODIFIED| M[Log Only]
    D -->|REMOVED| N[Log Only]

    E --> F
    F -->|No| O[Skip Validation]
    F -->|Yes| G
    G -->|No| P[Skip - Invalid Data]
    G -->|Yes| H

    H -->|Valid| I
    H -->|Invalid| J
    J --> K
```

**Lifecycle:**

```mermaid
sequenceDiagram
    participant Spring as Spring Boot
    participant Listener as PostListener
    participant Firestore as Firestore
    participant PostSvc as PostService
    participant NotifSvc as NotificationService

    Note over Spring,Listener: Server Startup
    Spring->>Listener: PostConstruct startListening()
    Listener->>Firestore: addSnapshotListener(posts)
    Firestore-->>Listener: Initial snapshot (skipped)

    Note over Firestore,NotifSvc: New Post Created
    Firestore->>Listener: DocumentChange (ADDED)
    Listener->>Listener: handleNewPost()
    Listener->>PostSvc: validatePost(title, content)
    PostSvc-->>Listener: ValidationResponse

    alt Valid Post
        Listener->>PostSvc: updatePostStatus(APPROVED)
    else Invalid Post
        Listener->>PostSvc: updatePostStatus(REJECTED)
        Listener->>NotifSvc: triggerNotification(POST_REJECTED)
    end

    Note over Spring,Listener: Server Shutdown
    Spring->>Listener: PreDestroy stopListening()
    Listener->>Firestore: Remove listener
```

---

### Real-time Message Listener

**Component:** FirestoreMessageListener

**Description:**  
A Spring component that listens for new chat messages across all chat conversations using Firestore collection group queries. When a new message is detected, it automatically sends a push notification to the recipient via FCM.

**Features:**

| Feature                | Description                                                |
| ---------------------- | ---------------------------------------------------------- |
| Collection Group Query | Monitors all messages across all chats simultaneously      |
| Duplicate Prevention   | Tracks processed messages to avoid duplicate notifications |
| Skip Initial           | Ignores existing messages on startup                       |
| Message Type Support   | Handles both TEXT and IMAGE message types                  |
| Auto-notification      | Sends FCM push notification to message recipient           |

**Message Types Handled:**

| Type    | Notification Behavior                             |
| ------- | ------------------------------------------------- |
| TEXT    | Sends chat notification with message content      |
| IMAGE   | Sends image message notification with image count |
| DELETED | Skips notification (deleted messages)             |

```mermaid
flowchart TD
    subgraph "Android App"
        A[User Sends Message]
    end

    subgraph "Firestore"
        B["chats/{chatId}/messages"]
    end

    subgraph "FirestoreMessageListener"
        C{Is Initial Snapshot?}
        D{Document Change Type}
        E[handleNewMessage]
    end

    subgraph "Message Processing"
        F{Message Type?}
        G[Get Sender Info]
        H[Get Chat Document]
        I[Find Recipient]
        J{Recipient has FCM Token?}
        K[Send Chat Notification]
        L[Send Image Notification]
    end

    A -->|New message| B
    B -->|Snapshot Event| C
    C -->|Yes| M[Skip - Pre-populate processed set]
    C -->|No| D
    D -->|ADDED| E

    E --> F
    F -->|DELETED| N[Skip Notification]
    F -->|TEXT/IMAGE| G
    G --> H
    H --> I
    I --> J
    J -->|No| O[Skip - No FCM Token]
    J -->|Yes| P{Message Type?}
    P -->|TEXT| K
    P -->|IMAGE| L
```

**Lifecycle:**

```mermaid
sequenceDiagram
    participant Spring as Spring Boot
    participant Listener as FirestoreMessageListener
    participant Firestore as Firestore
    participant UserSvc as UserService
    participant FCMSvc as FCMService

    Note over Spring,Listener: Server Startup
    Spring->>Listener: PostConstruct startListening()
    Listener->>Firestore: collectionGroup(messages).addSnapshotListener()
    Firestore-->>Listener: Initial snapshot (skipped)
    Listener->>Listener: Pre-populate processedMessages set

    Note over Firestore,FCMSvc: New Message Sent
    Firestore->>Listener: DocumentChange (ADDED)
    Listener->>Listener: Check if already processed
    Listener->>Listener: handleNewMessage()
    Listener->>UserSvc: getUserById(senderId)
    Listener->>Firestore: Get chat document
    Listener->>Listener: Find recipient from userIds
    Listener->>UserSvc: getUserById(recipientId)

    alt Has FCM Token
        alt Image Message
            Listener->>FCMSvc: sendImageMessageNotification()
        else Text Message
            Listener->>FCMSvc: sendChatNotification()
        end
    else No FCM Token
        Listener->>Listener: Skip notification
    end

    Note over Spring,Listener: Server Shutdown
    Spring->>Listener: PreDestroy stopListening()
    Listener->>Firestore: Remove listener
```

**Notification Payload Structure:**

**Text Message:**

| Field            | Path         | Description                   |
| ---------------- | ------------ | ----------------------------- |
| token            | root         | Recipient's FCM token         |
| title            | notification | Sender's full name            |
| body             | notification | Message content               |
| chatId           | data         | Chat identifier for deep link |
| senderId         | data         | Sender's user ID              |
| senderEmail      | data         | Sender's email                |
| senderProfileUrl | data         | Sender's profile picture URL  |

**Image Message:**

| Field            | Path         | Description                   |
| ---------------- | ------------ | ----------------------------- |
| token            | root         | Recipient's FCM token         |
| title            | notification | Sender's full name            |
| body             | notification | "Sent {count} image(s)"       |
| imageContent     | notification | Message content/caption       |
| chatId           | data         | Chat identifier for deep link |
| senderId         | data         | Sender's user ID              |
| senderEmail      | data         | Sender's email                |
| senderProfileUrl | data         | Sender's profile picture URL  |

---

### Real-time Listeners Summary

| Listener                 | Collection                  | Purpose                         |
| ------------------------ | --------------------------- | ------------------------------- |
| TopicsListener           | topics                      | Cache topics for fast access    |
| PostListener             | posts                       | Auto-validate new PENDING posts |
| FirestoreMessageListener | messages (collection group) | Send chat message notifications |

---

## System Architecture

```mermaid
flowchart TB
    subgraph "Android Kotlin App"
        A1[Authentication]
        A2[Post Management]
        A3[Notifications]
        A4[Topics]
    end

    subgraph "Spring Boot Backend"
        subgraph "Controllers"
            B1[AuthController]
            B2[EmailController]
            B3[PostController]
            B4[NotificationController]
            B5[TopicController]
            B6[HealthController]
        end

        subgraph "Services"
            C1[EmailService]
            C2[PostService]
            C3[NotificationService]
            C4[TopicService]
            C5[FCMService]
            C6[UserService]
            C7[SummaryCacheService]
        end

        subgraph "Real-time Listeners"
            D1[PostListener]
            D2[TopicsListener]
        end
    end

    subgraph "External Services"
        E1[Firebase Auth]
        E2[Firestore DB]
        E3[Firebase FCM]
        E4[Gemini AI API]
        E5[SMTP Server]
    end

    A1 --> B1 --> E1
    A1 --> B2 --> C1 --> E5
    A2 --> B3 --> C2 --> E4
    A3 --> B4 --> C3 --> E3
    A4 --> B5 --> C4

    C2 --> C7
    C2 --> E2
    C3 --> C5
    C3 --> C6
    C4 --> D2

    D1 --> C2
    D1 --> C3
    D2 --> E2

    E2 --> D1
    E2 --> D2
```

---

## Technology Stack

| Component          | Technology                     |
| ------------------ | ------------------------------ |
| Backend Framework  | Spring Boot 3.x                |
| Language           | Java 17+                       |
| Database           | Firebase Firestore             |
| Authentication     | Firebase Auth                  |
| Push Notifications | Firebase Cloud Messaging (FCM) |
| AI/ML              | Google Gemini 2.5 Flash        |
| Email              | JavaMailSender (SMTP)          |
| Caching            | In-memory (ConcurrentHashMap)  |
| Containerization   | Docker                         |

---

**Document Version:** 1.0  
**Last Updated:** January 2026
