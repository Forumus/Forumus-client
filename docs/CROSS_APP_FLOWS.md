# Forumus Cross-App Integration Flows

**Demonstrating End-to-End Workflows Across Client, Admin & Backend**

This document describes features that require coordination between multiple applications in the Forumus ecosystem to demonstrate complete functionality.

---

## System Overview

```mermaid
flowchart TB
    subgraph "Mobile Applications"
        A[Client App<br/>Android Kotlin]
        B[Admin App<br/>Android Kotlin]
    end

    subgraph "Backend Services"
        C[Spring Boot Server<br/>Java]
        D[Gemini AI<br/>Google AI]
        E[SMTP Server<br/>Email Service]
    end

    subgraph "Firebase Services"
        F[Firestore<br/>Database]
        G[Firebase Auth]
        H[FCM<br/>Push Notifications]
        I[Firebase Storage]
    end

    A <--> F
    A <--> G
    A <--> C
    A <--> H

    B <--> F
    B <--> C

    C <--> D
    C <--> E
    C <--> F
    C <--> G
    C <--> H
```

---

## Applications Reference

| App | Technology | Primary Role |
|-----|------------|--------------|
| **Client App** | Android Kotlin | End-user forum access (posts, chat, profile) |
| **Admin App** | Android Kotlin | Content moderation, user management, analytics |
| **Backend Server** | Spring Boot (Java) | AI processing, email delivery, push notifications |

---

## Sequential User Journey

The following flows are organized in the order a typical user would experience them.

# Phase 1: User Onboarding

## Flow 1: Registration with OTP Email Verification

**Apps Involved:** Client → Backend → Email → Client

**Description:**  
New user registers an account, receives OTP via email, and verifies their email address to complete registration.

### Sequence Diagram

```mermaid
sequenceDiagram
    participant User
    participant Client as Client App
    participant Firebase as Firebase Auth
    participant Firestore as Firestore
    participant Backend as Backend Server
    participant SMTP as SMTP Server
    participant Email as User Email

    User->>Client: Fill registration form
    Client->>Client: Generate 6-digit OTP
    Client->>Backend: POST /api/email/send-otp
    Note over Backend: {recipientEmail, otpCode}
    Backend->>Backend: Generate HTML template
    Backend->>SMTP: Send MIME message
    SMTP->>Email: Deliver OTP email
    Backend-->>Client: 200 OK {success: true}

    Client->>Client: Navigate to Verification Screen
    User->>Email: Open email, view OTP
    User->>Client: Enter OTP code

    Client->>Client: Validate OTP matches
    alt OTP Valid
        Client->>Firebase: createUserWithEmailAndPassword()
        Firebase-->>Client: User created
        Client->>Firestore: Create user document
        Note over Firestore: users/{uid} with status: NORMAL
        Client->>Client: Navigate to Success Screen
    else OTP Invalid
        Client->>Client: Show error, allow retry
    end
```

### API Endpoints

| Endpoint | Method | Request Body | Response |
|----------|--------|--------------|----------|
| `/api/email/send-otp` | POST | `{recipientEmail, otpCode}` | `{success, message}` |

### Firestore Changes

| Collection | Document | Action | Fields |
|------------|----------|--------|--------|
| `users` | `{uid}` | CREATE | email, fullName, role, status, profilePictureUrl, fcmToken |

---

## Flow 2: Welcome Email After Verification

**Apps Involved:** Client → Backend → Email

**Description:**  
After successful OTP verification and account creation, the system sends a welcome email to the new user.

### Sequence Diagram

```mermaid
sequenceDiagram
    participant Client as Client App
    participant Backend as Backend Server
    participant SMTP as SMTP Server
    participant Email as User Email

    Note over Client: After successful registration
    Client->>Backend: POST /api/email/send-welcome
    Note over Backend: {recipientEmail, userName}
    Backend->>Backend: Generate welcome HTML
    Backend->>SMTP: Send welcome email
    SMTP->>Email: Deliver welcome email
    Backend-->>Client: 200 OK {success: true}
    Client->>Client: Navigate to Login/Home
```

### API Endpoints

| Endpoint | Method | Request Body | Response |
|----------|--------|--------------|----------|
| `/api/email/send-welcome` | POST | `{recipientEmail, userName}` | `{success, message}` |

---

## Flow 3: Forgot Password Reset

**Apps Involved:** Client → Backend → Firebase Auth

**Description:**  
User requests password reset, receives OTP via email, and sets a new password through the backend server.

### Sequence Diagram

```mermaid
sequenceDiagram
    participant User
    participant Client as Client App
    participant Backend as Backend Server
    participant Firebase as Firebase Auth
    participant SMTP as SMTP Server
    participant Email as User Email

    User->>Client: Tap "Forgot Password"
    Client->>Client: Enter email address
    Client->>Client: Generate OTP
    Client->>Backend: POST /api/email/send-otp
    Backend->>SMTP: Send OTP email
    SMTP->>Email: Deliver OTP
    Backend-->>Client: 200 OK

    User->>Email: View OTP
    User->>Client: Enter OTP + New Password

    Client->>Client: Validate OTP
    alt OTP Valid
        Client->>Backend: POST /api/auth/resetPassword
        Note over Backend: {email, newPassword, secretKey}
        Backend->>Firebase: getUserByEmail()
        Firebase-->>Backend: User record
        Backend->>Firebase: updateUser(password)
        Firebase-->>Backend: Success
        Backend-->>Client: 200 OK {success: true}
        Client->>Client: Navigate to Login
    else OTP Invalid
        Client->>Client: Show error
    end
```

### API Endpoints

| Endpoint | Method | Request Body | Response |
|----------|--------|--------------|----------|
| `/api/email/send-otp` | POST | `{recipientEmail, otpCode}` | `{success, message}` |
| `/api/auth/resetPassword` | POST | `{email, newPassword, secretKey}` | `{success, message}` |

---

# Phase 2: Content Creation

## Flow 4: Post Creation with AI Topic Suggestions

**Apps Involved:** Client → Backend (Gemini AI) → Client

**Description:**  
User creates a new post and requests AI-powered topic suggestions based on the post content.

### Sequence Diagram

```mermaid
sequenceDiagram
    participant User
    participant Client as Client App
    participant Backend as Backend Server
    participant Gemini as Gemini AI
    participant Cache as Topics Cache

    User->>Client: Write post title and content
    User->>Client: Tap "Suggest Topics"

    Client->>Backend: POST /api/posts/getSuggestedTopics
    Note over Backend: {title, content}

    Backend->>Cache: getAllTopics()
    Cache-->>Backend: List of available topics
    Backend->>Backend: Build AI prompt with topic list
    Backend->>Gemini: Generate content request
    Gemini-->>Backend: JSON with suggested topic names
    Backend->>Backend: Match names to topic objects
    Backend-->>Client: {success: true, topics: [...]}

    Client->>Client: Display suggested topics as chips
    User->>Client: Select/deselect topics
    User->>Client: Tap "Post"
```

### API Endpoints

| Endpoint | Method | Request Body | Response |
|----------|--------|--------------|----------|
| `/api/posts/getSuggestedTopics` | POST | `{title, content}` | `{success, topics: [{topicId, name, description}]}` |

---

## Flow 5: Post Creation & Automatic AI Validation

**Apps Involved:** Client → Firestore → Backend (Listener + AI) → Client

**Description:**  
User submits a new post. The backend's real-time listener automatically validates the content using AI and updates the post status. If rejected, a push notification is sent to the author.

### Sequence Diagram

```mermaid
sequenceDiagram
    participant User
    participant Client as Client App
    participant Firestore as Firestore
    participant Listener as PostListener
    participant PostSvc as PostService
    participant Gemini as Gemini AI
    participant NotifSvc as NotificationService
    participant FCM as FCM

    User->>Client: Complete post and tap Submit
    Client->>Client: Upload media to Firebase Storage
    Client->>Firestore: Create post document
    Note over Firestore: status: "PENDING"

    Firestore->>Listener: Snapshot event (ADDED)
    Listener->>Listener: Check status == PENDING
    Listener->>PostSvc: validatePost(title, content)

    PostSvc->>Gemini: Send content for analysis
    Gemini-->>PostSvc: Validation result

    alt Content Valid
        PostSvc->>Firestore: Update status: "APPROVED"
        Firestore-->>Client: Real-time update
        Client->>Client: Post appears in feed
    else Content Invalid
        PostSvc->>Firestore: Update status: "REJECTED"
        PostSvc->>Firestore: Add violation_type array
        Listener->>NotifSvc: triggerNotification(POST_REJECTED)
        NotifSvc->>Firestore: Save notification
        NotifSvc->>FCM: Send push notification
        FCM->>Client: Display rejection notification
        Client->>Client: Show in Notification Center
    end
```

### API Endpoints

| Endpoint | Method | Trigger | Description |
|----------|--------|---------|-------------|
| (Automatic) | - | Firestore Listener | PostListener detects new PENDING posts |
| `/api/notifications` | POST | Internal | Triggered on rejection |

### Firestore Changes

| Collection | Document | Action | Fields Changed |
|------------|----------|--------|----------------|
| `posts` | `{postId}` | UPDATE | status (PENDING → APPROVED/REJECTED), violation_type |
| `users/{uid}/notifications` | `{notificationId}` | CREATE | type, previewText, rejectionReason, isRead |

---

## Flow 6: AI Post Summary Generation

**Apps Involved:** Client → Backend (Gemini AI + Cache) → Client

**Description:**  
User requests an AI-generated summary of a post. The backend uses intelligent caching to avoid redundant API calls.

### Sequence Diagram

```mermaid
sequenceDiagram
    participant User
    participant Client as Client App
    participant Backend as Backend Server
    participant Cache as Summary Cache
    participant Gemini as Gemini AI

    User->>Client: Tap "Summarize" on post
    Client->>Client: Show loading indicator
    Client->>Backend: POST /api/posts/summarize
    Note over Backend: {postId}

    Backend->>Backend: Fetch post from Firestore
    Backend->>Backend: Compute content hash (SHA-256)
    Backend->>Cache: Check cache with hash

    alt Cache HIT
        Cache-->>Backend: Return cached summary
        Backend-->>Client: {success, summary, fromCache: true}
    else Cache MISS
        Backend->>Gemini: Generate summary request
        Gemini-->>Backend: Summary text
        Backend->>Cache: Store with hash key
        Backend-->>Client: {success, summary, fromCache: false}
    end

    Client->>Client: Display summary in dialog
```

### API Endpoints

| Endpoint | Method | Request Body | Response |
|----------|--------|--------------|----------|
| `/api/posts/summarize` | POST | `{postId}` | `{success, summary, fromCache, contentHash, generatedAt}` |

---

# Phase 3: User Interactions

## Flow 7: Upvote/Comment/Reply Push Notifications

**Apps Involved:** Client (Sender) → Backend → FCM → Client (Recipient)

**Description:**  
When a user upvotes a post, comments, or replies, the system sends a push notification to the content owner.

### Sequence Diagram

```mermaid
sequenceDiagram
    participant UserA as User A (Actor)
    participant ClientA as Client App (A)
    participant Firestore as Firestore
    participant Backend as Backend Server
    participant FCM as FCM
    participant ClientB as Client App (B)
    participant UserB as User B (Recipient)

    UserA->>ClientA: Upvote/Comment/Reply on User B's post

    par Update Firestore
        ClientA->>Firestore: Update post/comment data
    and Send Notification
        ClientA->>Backend: POST /api/notifications
        Note over Backend: {type: UPVOTE/COMMENT/REPLY,<br/>actorId, actorName,<br/>targetId, targetUserId,<br/>previewText}
    end

    Backend->>Backend: Validate actorId ≠ targetUserId
    Backend->>Firestore: Fetch target user (get FCM token)
    Backend->>Firestore: Save notification document
    Backend->>FCM: Send push notification
    FCM->>ClientB: Deliver notification
    ClientB->>ClientB: Show system notification
    UserB->>ClientB: Tap notification
    ClientB->>ClientB: Navigate to post/comment
```

### API Endpoints

| Endpoint | Method | Request Body | Response |
|----------|--------|--------------|----------|
| `/api/notifications` | POST | `{type, actorId, actorName, targetId, targetUserId, previewText}` | `"Notification triggered successfully"` |

### Notification Types

| Type | Title | Body Template |
|------|-------|---------------|
| UPVOTE | New Upvote | {actorName} upvoted your post: {previewText} |
| COMMENT | New Comment | {actorName} commented on your post: {previewText} |
| REPLY | New Reply | {actorName} replied to your comment: {previewText} |

### Firestore Changes

| Collection | Document | Action | Fields |
|------------|----------|--------|--------|
| `users/{targetUserId}/notifications` | `{notificationId}` | CREATE | type, actorId, actorName, targetId, previewText, createdAt, isRead |

---

## Flow 8: Chat Message Push Notification

**Apps Involved:** Client (Sender) → Backend → FCM → Client (Recipient)

**Description:**  
When a user sends a chat message, the recipient receives a real-time push notification even when the app is in background.

### Sequence Diagram

```mermaid
sequenceDiagram
    participant UserA as Sender
    participant ClientA as Client App (Sender)
    participant Firestore as Firestore
    participant Backend as Backend Server
    participant FCM as FCM
    participant ClientB as Client App (Recipient)
    participant UserB as Recipient

    UserA->>ClientA: Type message and tap Send
    
    alt Has Images
        ClientA->>ClientA: Enqueue WorkManager job
        ClientA->>ClientA: Upload images to Storage
    end

    ClientA->>Firestore: Create message document
    Note over Firestore: chats/{chatId}/messages/{msgId}

    ClientA->>Backend: POST /api/notifications
    Note over Backend: {type: MESSAGE,<br/>actorId, actorName,<br/>targetUserId,<br/>previewText: message content}

    Backend->>Firestore: Get recipient's FCM token
    Backend->>FCM: Send push notification
    FCM->>ClientB: Deliver chat notification

    alt App in Background
        ClientB->>ClientB: Show system notification
        UserB->>ClientB: Tap notification
        ClientB->>ClientB: Open conversation
    else App in Foreground
        ClientB->>ClientB: Update chat UI in real-time
    end
```

### API Endpoints

| Endpoint | Method | Request Body | Response |
|----------|--------|--------------|----------|
| `/api/notifications` | POST | `{type: "MESSAGE", actorId, actorName, targetUserId, previewText}` | `"Notification triggered successfully"` |

### Firestore Changes

| Collection | Document | Action | Fields |
|------------|----------|--------|--------|
| `chats/{chatId}/messages` | `{messageId}` | CREATE | content, senderId, timestamp, type, imageUrls |

---

## Flow 9: Share Post via Direct Message

**Apps Involved:** Client (Sender) → Firestore → Client (Recipient)

**Description:**  
User shares a post with another user through direct message. The shared post renders as a clickable preview card in the chat.

### Sequence Diagram

```mermaid
sequenceDiagram
    participant UserA as Sender
    participant ClientA as Client App (A)
    participant Firestore as Firestore
    participant Backend as Backend Server
    participant FCM as FCM
    participant ClientB as Client App (B)
    participant UserB as Recipient

    UserA->>ClientA: Tap Share on a post
    ClientA->>ClientA: Show Share Dialog
    ClientA->>Firestore: Load potential recipients
    ClientA->>ClientA: Display recipient list
    UserA->>ClientA: Select recipient(s)
    UserA->>ClientA: Tap Send

    loop For each recipient
        ClientA->>ClientA: Generate share URL
        Note over ClientA: forumus://post/{postId}
        ClientA->>Firestore: Get/Create chat document
        ClientA->>Firestore: Send message with share link
        Note over Firestore: content: "forumus://post/{postId}"
        ClientA->>Backend: POST /api/notifications
    end

    Backend->>FCM: Send push notification
    FCM->>ClientB: Deliver notification
    
    UserB->>ClientB: Open conversation
    ClientB->>ClientB: Detect share link pattern
    ClientB->>Firestore: Fetch post data
    ClientB->>ClientB: Render post preview card
    UserB->>ClientB: Tap preview card
    ClientB->>ClientB: Navigate to Post Detail
```

### Firestore Changes

| Collection | Document | Action | Fields |
|------------|----------|--------|--------|
| `chats/{chatId}/messages` | `{messageId}` | CREATE | content (share URL), senderId, timestamp, type: TEXT |

---

# Phase 4: Content Moderation

## Flow 10: User Reports Post → Admin Reviews

**Apps Involved:** Client → Firestore → Admin

**Description:**  
User reports a post for violating community guidelines. The report appears in the Admin app's Reported Posts screen for review.

### Sequence Diagram

```mermaid
sequenceDiagram
    participant User as User
    participant Client as Client App
    participant Firestore as Firestore
    participant Admin as Admin App
    participant AdminUser as Administrator

    User->>Client: Tap Menu → Report Post
    Client->>Client: Show Report Dialog
    User->>Client: Select violation type
    User->>Client: Tap Submit

    Client->>Firestore: Create report document
    Note over Firestore: reports/{reportId}
    Client->>Firestore: Increment post.reportCount
    Client->>Client: Show confirmation toast

    Note over Firestore,Admin: Admin opens Reported Posts screen
    AdminUser->>Admin: Navigate to Reported Posts
    Admin->>Firestore: Query posts where reportCount > 0
    Firestore-->>Admin: List of reported posts
    Admin->>Admin: Display with report badges

    AdminUser->>Admin: Tap Report Badge
    Admin->>Firestore: Fetch reports for post
    Admin->>Admin: Show Report Details Dialog
```

### Firestore Changes

| Collection | Document | Action | Fields |
|------------|----------|--------|--------|
| `reports` | `{reportId}` | CREATE | postId, authorId, nameViolation, descriptionViolation |
| `posts` | `{postId}` | UPDATE | reportCount (increment) |

---

## Flow 11: Admin AI Override (Approve/Reject Post)

**Apps Involved:** Admin → Backend → FCM → Client

**Description:**  
Administrator reviews AI-moderated posts and overrides the AI decision. Actions trigger notifications to the post author.

### Sequence Diagram

```mermaid
sequenceDiagram
    participant AdminUser as Administrator
    participant Admin as Admin App
    participant Firestore as Firestore
    participant Backend as Backend Server
    participant FCM as FCM
    participant Client as Client App
    participant User as Post Author

    AdminUser->>Admin: Open AI Moderation screen
    Admin->>Firestore: Query posts by status
    Admin->>Admin: Display AI Approved / AI Rejected tabs

    alt Override AI Rejection (Approve)
        AdminUser->>Admin: Tap Approve on rejected post
        Admin->>Admin: Show confirmation dialog
        AdminUser->>Admin: Confirm
        Admin->>Firestore: Update status: APPROVED
        Admin->>Backend: POST /api/notifications
        Note over Backend: {type: POST_APPROVED,<br/>targetUserId, previewText}
        Backend->>FCM: Send push notification
        FCM->>Client: Notification received
        Client->>Client: Post now visible in feed
    else Override AI Approval (Reject)
        AdminUser->>Admin: Tap Reject on approved post
        Admin->>Admin: Show confirmation dialog
        AdminUser->>Admin: Confirm
        Admin->>Firestore: Update status: REJECTED
        Admin->>Admin: Escalate user status
        Admin->>Backend: POST /api/notifications
        Note over Backend: {type: POST_REJECTED,<br/>targetUserId, rejectionReason}
        Admin->>Backend: POST /api/email/send-report
        Backend->>FCM: Send push notification
        FCM->>Client: Notification received
        User->>Client: View rejection reason
    end
```

### API Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/notifications` | POST | Send push notification to author |
| `/api/email/send-report` | POST | Send email about status change |

### Firestore Changes

| Collection | Document | Action | Fields Changed |
|------------|----------|--------|----------------|
| `posts` | `{postId}` | UPDATE | status |
| `users` | `{authorId}` | UPDATE | status (on rejection with escalation) |
| `users/{authorId}/notifications` | `{notificationId}` | CREATE | type, previewText, rejectionReason |

---

## Flow 12: Admin Deletes Reported Post & User Status Escalation

**Apps Involved:** Admin → Backend → Email + FCM → Client

**Description:**  
Administrator deletes a reported post. The system automatically escalates the author's account status and sends both email and push notifications.

### Sequence Diagram

```mermaid
sequenceDiagram
    participant AdminUser as Administrator
    participant Admin as Admin App
    participant Firestore as Firestore
    participant Backend as Backend Server
    participant SMTP as SMTP Server
    participant FCM as FCM
    participant Client as Client App
    participant User as Post Author
    participant Email as User Email

    AdminUser->>Admin: View Reported Posts
    AdminUser->>Admin: Tap Delete on post
    Admin->>Admin: Show confirmation dialog
    AdminUser->>Admin: Confirm deletion

    Admin->>Firestore: Delete post document
    Admin->>Admin: Calculate next status level
    Note over Admin: NORMAL → REMINDED → WARNED → BANNED

    Admin->>Firestore: Update user status
    Note over Firestore: users/{authorId}.status

    par Send Email Notification
        Admin->>Backend: POST /api/email/send-report
        Note over Backend: {recipientEmail, userName,<br/>userStatus, reportedPosts}
        Backend->>SMTP: Send status email
        SMTP->>Email: Deliver to user
    and Send Push Notification
        Admin->>Backend: POST /api/notifications
        Note over Backend: {type: POST_DELETED,<br/>targetUserId, previewText}
        Backend->>Firestore: Save notification
        Backend->>FCM: Send push
        FCM->>Client: Deliver notification
    end

    Client->>Client: Update notification badge
    User->>Client: View notification
    User->>Email: View status email

    alt Status = BANNED
        Note over Client: User blocked from app on next login
        User->>Client: Attempt login
        Client->>Client: Navigate to Banned Screen
    end
```

### API Endpoints

| Endpoint | Method | Request Body | Purpose |
|----------|--------|--------------|---------|
| `/api/email/send-report` | POST | `{recipientEmail, userName, userStatus, reportedPosts}` | Send status change email |
| `/api/notifications` | POST | `{type: POST_DELETED, targetUserId, previewText}` | Send push notification |

### Status Escalation Levels

| Current Status | Next Status | Description |
|----------------|-------------|-------------|
| NORMAL | REMINDED | First violation - reminder issued |
| REMINDED | WARNED | Second violation - formal warning |
| WARNED | BANNED | Third violation - account suspended |
| BANNED | BANNED | Already maximum level |

### Firestore Changes

| Collection | Document | Action | Fields Changed |
|------------|----------|--------|----------------|
| `posts` | `{postId}` | DELETE | - |
| `users` | `{authorId}` | UPDATE | status |
| `users/{authorId}/notifications` | `{notificationId}` | CREATE | type, previewText |

---

# Phase 5: User Account Management

## Flow 13: Admin Updates User Status (Blacklist Management)

**Apps Involved:** Admin → Backend → Email + FCM → Client

**Description:**  
Administrator manually changes a user's account status from the Blacklist Management screen. Both email and push notifications are sent to inform the user.

### Sequence Diagram

```mermaid
sequenceDiagram
    participant AdminUser as Administrator
    participant Admin as Admin App
    participant Firestore as Firestore
    participant Backend as Backend Server
    participant SMTP as SMTP Server
    participant FCM as FCM
    participant Client as Client App
    participant User as Target User
    participant Email as User Email

    AdminUser->>Admin: Open Blacklist Management
    Admin->>Firestore: Query users with status ≠ NORMAL
    Admin->>Admin: Display blacklisted users

    AdminUser->>Admin: Tap status badge on user
    Admin->>Admin: Show status action menu
    AdminUser->>Admin: Select new status
    Admin->>Admin: Show confirmation dialog
    AdminUser->>Admin: Confirm

    Admin->>Firestore: Update user.status
    Admin->>Admin: Determine escalation/de-escalation

    alt Escalation (Status Increased)
        Admin->>Backend: POST /api/email/send-report
        Note over Backend: Escalation email with reason
    else De-escalation (Status Decreased)
        Admin->>Backend: POST /api/email/send-report
        Note over Backend: Congratulatory email
    end

    Backend->>SMTP: Send email
    SMTP->>Email: Deliver to user

    Admin->>Backend: POST /api/notifications
    Note over Backend: {type: STATUS_CHANGED, previewText}
    Backend->>Firestore: Save notification
    Backend->>FCM: Send push
    FCM->>Client: Deliver notification

    User->>Client: View notification
    User->>Email: View status email

    alt New Status = NORMAL
        Admin->>Admin: Remove user from blacklist view
    end
```

### API Endpoints

| Endpoint | Method | Request Body | Purpose |
|----------|--------|--------------|---------|
| `/api/email/send-report` | POST | `{recipientEmail, userName, userStatus, reportedPosts}` | Send status notification email |
| `/api/notifications` | POST | `{type: STATUS_CHANGED, targetUserId, previewText}` | Send push notification |

### Firestore Changes

| Collection | Document | Action | Fields Changed |
|------------|----------|--------|----------------|
| `users` | `{userId}` | UPDATE | status |
| `users/{userId}/notifications` | `{notificationId}` | CREATE | type, previewText, createdAt |

---

# Summary: Cross-App Flow Index

## By User Journey Phase

| Phase | Flow # | Flow Name | Apps |
|-------|--------|-----------|------|
| **Onboarding** | 1 | Registration with OTP | Client → Backend → Email |
| | 2 | Welcome Email | Client → Backend → Email |
| | 3 | Forgot Password | Client → Backend → Firebase Auth |
| **Content Creation** | 4 | AI Topic Suggestions | Client → Backend (AI) |
| | 5 | Post Validation | Client → Firestore → Backend (Listener + AI) → Client |
| | 6 | AI Post Summary | Client → Backend (AI + Cache) |
| **Interactions** | 7 | Upvote/Comment/Reply Notifications | Client → Backend → FCM → Client |
| | 8 | Chat Push Notification | Client → Backend → FCM → Client |
| | 9 | Share Post via DM | Client → Firestore → Client |
| **Moderation** | 10 | User Reports Post | Client → Firestore → Admin |
| | 11 | Admin AI Override | Admin → Backend → FCM → Client |
| | 12 | Admin Deletes Post + Escalation | Admin → Backend → Email + FCM → Client |
| **Account Management** | 13 | Admin Updates User Status | Admin → Backend → Email + FCM → Client |

## By Trigger Source

| Trigger | Flows |
|---------|-------|
| **User Action (Client)** | 1, 2, 3, 4, 6, 7, 8, 9, 10 |
| **Automatic (Backend Listener)** | 5 |
| **Admin Action (Admin App)** | 11, 12, 13 |

## By Notification Type

| Notification | Flows | Delivery Method |
|--------------|-------|-----------------|
| OTP Code | 1, 3 | Email |
| Welcome | 2 | Email |
| Status Report | 11, 12, 13 | Email + Push |
| Post Approved | 5, 11 | Push |
| Post Rejected | 5, 11 | Push |
| Post Deleted | 12 | Push |
| Upvote/Comment/Reply | 7 | Push |
| Chat Message | 8 | Push |

---

## Technology Stack Summary

| Component | Technology | Used In |
|-----------|------------|---------|
| **Client App** | Android Kotlin, MVVM, Firebase | All user-facing flows |
| **Admin App** | Android Kotlin, MVVM, Firebase | Moderation flows (10-13) |
| **Backend Server** | Spring Boot, Java 17 | AI, Email, Notifications |
| **AI Service** | Google Gemini 2.5 Flash | Flows 4, 5, 6 |
| **Email** | JavaMailSender, SMTP | Flows 1, 2, 3, 11, 12, 13 |
| **Push Notifications** | Firebase Cloud Messaging | Flows 5, 7, 8, 11, 12, 13 |
| **Database** | Cloud Firestore | All flows |
| **Authentication** | Firebase Auth | Flows 1, 3 |
| **Storage** | Firebase Storage | Flow 5 (media upload) |

---

**Document Version:** 1.0  
**Last Updated:** January 2026  
**Total Cross-App Flows:** 13
