# AI Summary Feature - Test Checklist

This document provides comprehensive test cases for the AI Summary feature as part of Phase 6: Testing & Quality Assurance.

## Task 6.1: Backend Endpoint Testing

### Test Cases for `/api/posts/summarize`

| # | Test Case | Input | Expected Result | Status |
|---|-----------|-------|-----------------|--------|
| 1 | Valid post ID | `{"postId": "<valid_post_id>"}` | `{"success": true, "summary": "<text>", "cached": false}` | [ ] |
| 2 | Invalid/non-existent post ID | `{"postId": "invalid-123"}` | `{"success": false, "errorMessage": "Post not found"}` | [ ] |
| 3 | Empty post ID | `{"postId": ""}` | `{"success": false, "errorMessage": "Post not found"}` | [ ] |
| 4 | Post with empty content | `{"postId": "<empty_content_post>"}` | Returns appropriate summary or graceful message | [ ] |
| 5 | Post with very long content (5000+ chars) | `{"postId": "<long_post_id>"}` | Summary generated without timeout, content truncated internally | [ ] |
| 6 | Rapid consecutive requests (same post) | 5 requests in 2 seconds | All requests complete, no crashes | [ ] |
| 7 | Unicode content post | `{"postId": "<unicode_post_id>"}` | Summary in appropriate language | [ ] |

### Performance Criteria
- [ ] Response time under 30 seconds for all test cases
- [ ] No server exceptions in logs
- [ ] Memory usage stable after multiple requests

### How to Test Backend
```bash
# Start the Spring Boot server
cd Forumus-server
./mvnw spring-boot:run

# Test with curl
curl -X POST http://localhost:8080/api/posts/summarize \
  -H "Content-Type: application/json" \
  -d '{"postId": "your-post-id"}'
```

---

## Task 6.2: Android API Integration Testing

### Prerequisites
- Backend server running and accessible
- Android emulator or physical device connected
- Correct API base URL configured

### Test Cases

| # | Test Case | Steps | Expected Result | Status |
|---|-----------|-------|-----------------|--------|
| 1 | API connectivity | Open app, tap summary button | Network request sent, response received | [ ] |
| 2 | Successful summary response | Tap summary on post with content | Dialog shows summary text | [ ] |
| 3 | Error response handling | Disconnect server, tap summary | Error toast shown | [ ] |
| 4 | JSON parsing | Check Logcat for parsing errors | No Moshi/JSON exceptions | [ ] |

### Logcat Filters
```
# Filter for API and repository logs
adb logcat -s "PostRepository" "OkHttp" "Retrofit"
```

---

## Task 6.3: UI Flow End-to-End Testing

### Home Feed Tests

| # | Test Case | Steps | Expected Result | Status |
|---|-----------|-------|-----------------|--------|
| 1 | Summary button tap → Loading → Dialog | Tap sparkle button on any post | Loading spinner shows, then dialog with summary | [ ] |
| 2 | Close button dismisses dialog | Tap "Close" in summary dialog | Dialog closes smoothly | [ ] |
| 3 | Summary on different post | Tap summary on Post A, close, tap on Post B | Correct summary for each post | [ ] |
| 4 | Network error shows toast | Disable network, tap summary | "Failed to get summary" toast appears | [ ] |
| 5 | Multiple rapid taps | Tap summary button 5 times quickly | Only one request made, no crashes | [ ] |
| 6 | Rotate during loading | Tap summary, rotate device | No crash, state preserved or graceful recovery | [ ] |

### Post Detail Tests

| # | Test Case | Steps | Expected Result | Status |
|---|-----------|-------|-----------------|--------|
| 1 | Summary in detail view | Open post detail, tap summary | Same behavior as home feed | [ ] |
| 2 | Navigate back during loading | Tap summary, press back | No crash, operation cancelled gracefully | [ ] |
| 3 | Scroll during loading | Tap summary, scroll comments | UI remains responsive | [ ] |

### Acceptance Criteria
- [ ] All scenarios work as expected
- [ ] No ANRs (Application Not Responding)
- [ ] No crashes (check Logcat)
- [ ] UI remains responsive during loading

---

## Task 6.4: Device Configuration Testing

### Screen Sizes

| Device Type | Screen Size | Test Items | Status |
|-------------|-------------|------------|--------|
| Small phone | 5" (720x1280) | Button visible, dialog fits screen | [ ] |
| Medium phone | 6" (1080x1920) | Standard behavior | [ ] |
| Large phone | 6.5" (1080x2340) | No layout overflow | [ ] |
| Tablet | 10" (1920x1200) | Dialog appropriately sized | [ ] |

### Theme Testing

| Mode | Test Items | Status |
|------|------------|--------|
| Light mode | Button icon visible, dialog colors correct | [ ] |
| Dark mode | Button icon visible, dialog uses dark theme colors | [ ] |

### Accessibility Testing

| Setting | Test Items | Status |
|---------|------------|--------|
| Default font | Normal display | [ ] |
| Large font (150%) | Text readable, no overflow | [ ] |
| Extra large font (200%) | Dialog scrollable if needed | [ ] |

### How to Test Different Configurations
1. **Screen sizes**: Use Android Emulator with different device profiles
2. **Dark mode**: Settings → Display → Dark theme
3. **Font sizes**: Settings → Display → Font size

---

## Task 6.5: Performance Testing

### Network Conditions

| Condition | Test Case | Expected Result | Status |
|-----------|-----------|-----------------|--------|
| Good WiFi | Tap summary | Response in < 5 seconds | [ ] |
| 3G/Slow network | Tap summary | Loading shows, response in < 30 seconds | [ ] |
| Timeout (>60s) | Simulate very slow response | Graceful timeout message | [ ] |
| No network | Tap summary | Error toast immediately | [ ] |

### How to Throttle Network
```bash
# Using Android Emulator Extended Controls
# Click ... → Settings → Cellular → Network type → 3G

# Or use Charles Proxy / Network Link Conditioner
```

### Memory Testing

| Test Case | Steps | Expected Result | Status |
|-----------|-------|-----------------|--------|
| Memory usage baseline | Open app, note memory usage | Baseline established | [ ] |
| After 10 summary requests | Request 10 summaries, check memory | No significant increase | [ ] |
| Dialog memory leak | Open/close dialog 20 times | Memory returns to baseline | [ ] |
| ViewModel memory | Rotate device 10 times during loading | No leak in Android Profiler | [ ] |

### How to Use Android Profiler
1. Open Android Studio → View → Tool Windows → Profiler
2. Select your app process
3. Click "Memory" tab
4. Perform actions and observe memory graph
5. Click "Record" to capture heap dump if needed

### Stress Testing

| Test Case | Steps | Expected Result | Status |
|-----------|-------|-----------------|--------|
| Rapid navigation | Navigate between screens while loading | No crashes | [ ] |
| Continuous usage | Use summary 50 times in session | Consistent behavior | [ ] |
| Background/foreground | Put app in background during loading | Resumes correctly | [ ] |

---

## Test Execution Log

### Date: _______________
### Tester: _______________

| Task | Pass | Fail | Notes |
|------|------|------|-------|
| 6.1 Backend | [ ] | [ ] | |
| 6.2 API Integration | [ ] | [ ] | |
| 6.3 UI Flow | [ ] | [ ] | |
| 6.4 Device Config | [ ] | [ ] | |
| 6.5 Performance | [ ] | [ ] | |

### Issues Found
| Issue # | Description | Severity | Status |
|---------|-------------|----------|--------|
| 1 | | | |
| 2 | | | |
| 3 | | | |

---

## Automated Tests Summary

### Backend Tests (JUnit 5)
- `PostSummaryRequestTest` - 7 tests for request DTO
- `PostSummaryResponseTest` - 12 tests for response DTO
- `PostControllerSummarizeTest` - 7 tests for controller endpoint

### Android Tests (JUnit 4)
- `PostSummaryRequestTest` - 8 tests for request DTO
- `PostSummaryResponseTest` - 12 tests for response DTO

### Running Tests

**Backend:**
```bash
cd Forumus-server
./mvnw test
```

**Android:**
```bash
cd Forumus-client
./gradlew test
```

---

## Sign-off

| Role | Name | Date | Signature |
|------|------|------|-----------|
| Developer | | | |
| QA Tester | | | |
| Tech Lead | | | |
