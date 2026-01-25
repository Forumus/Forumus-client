package com.hcmus.forumus_client.data.dto

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for PostSummaryRequest DTO.
 * Verifies data class behavior and field values.
 */
class PostSummaryRequestTest {

    @Test
    fun `constructor sets postId correctly`() {
        val postId = "test-post-123"
        
        val request = PostSummaryRequest(postId)
        
        assertEquals(postId, request.postId)
    }

    @Test
    fun `empty postId is allowed`() {
        val request = PostSummaryRequest("")
        
        assertEquals("", request.postId)
    }

    @Test
    fun `long postId is handled`() {
        val longId = "a".repeat(1000)
        
        val request = PostSummaryRequest(longId)
        
        assertEquals(longId, request.postId)
        assertEquals(1000, request.postId.length)
    }

    @Test
    fun `special characters in postId`() {
        val specialId = "post-123_abc@#\$%"
        
        val request = PostSummaryRequest(specialId)
        
        assertEquals(specialId, request.postId)
    }

    @Test
    fun `copy function works correctly`() {
        val original = PostSummaryRequest("original-id")
        
        val copied = original.copy(postId = "new-id")
        
        assertEquals("original-id", original.postId)
        assertEquals("new-id", copied.postId)
    }

    @Test
    fun `equals function compares correctly`() {
        val request1 = PostSummaryRequest("same-id")
        val request2 = PostSummaryRequest("same-id")
        val request3 = PostSummaryRequest("different-id")
        
        assertEquals(request1, request2)
        assertNotEquals(request1, request3)
    }

    @Test
    fun `hashCode is consistent for equal objects`() {
        val request1 = PostSummaryRequest("test-id")
        val request2 = PostSummaryRequest("test-id")
        
        assertEquals(request1.hashCode(), request2.hashCode())
    }

    @Test
    fun `toString contains postId`() {
        val postId = "test-post-123"
        val request = PostSummaryRequest(postId)
        
        val stringRepresentation = request.toString()
        
        assertTrue(stringRepresentation.contains(postId))
    }
}
