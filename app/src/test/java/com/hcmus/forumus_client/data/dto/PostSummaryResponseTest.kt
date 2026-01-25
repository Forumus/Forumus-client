package com.hcmus.forumus_client.data.dto

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for PostSummaryResponse DTO.
 * Verifies data class behavior, field values, and edge cases.
 */
class PostSummaryResponseTest {

    @Test
    fun `success response has correct values`() {
        val response = PostSummaryResponse(
            success = true,
            summary = "Test summary",
            errorMessage = null,
            cached = false
        )
        
        assertTrue(response.success)
        assertEquals("Test summary", response.summary)
        assertNull(response.errorMessage)
        assertFalse(response.cached)
    }

    @Test
    fun `error response has correct values`() {
        val response = PostSummaryResponse(
            success = false,
            summary = null,
            errorMessage = "Post not found",
            cached = false
        )
        
        assertFalse(response.success)
        assertNull(response.summary)
        assertEquals("Post not found", response.errorMessage)
        assertFalse(response.cached)
    }

    @Test
    fun `cached response has cached flag true`() {
        val response = PostSummaryResponse(
            success = true,
            summary = "Cached summary",
            errorMessage = null,
            cached = true
        )
        
        assertTrue(response.success)
        assertTrue(response.cached)
    }

    @Test
    fun `default cached value is false`() {
        val response = PostSummaryResponse(
            success = true,
            summary = "Test",
            errorMessage = null
        )
        
        assertFalse(response.cached)
    }

    @Test
    fun `empty summary is allowed`() {
        val response = PostSummaryResponse(
            success = true,
            summary = "",
            errorMessage = null,
            cached = false
        )
        
        assertTrue(response.success)
        assertEquals("", response.summary)
    }

    @Test
    fun `long summary is handled`() {
        val longSummary = "This is a detailed summary. ".repeat(100)
        
        val response = PostSummaryResponse(
            success = true,
            summary = longSummary,
            errorMessage = null,
            cached = false
        )
        
        assertEquals(longSummary, response.summary)
    }

    @Test
    fun `unicode summary is handled`() {
        val unicodeSummary = "这是一个测试摘要 🎉 с русскими буквами"
        
        val response = PostSummaryResponse(
            success = true,
            summary = unicodeSummary,
            errorMessage = null,
            cached = false
        )
        
        assertEquals(unicodeSummary, response.summary)
    }

    @Test
    fun `copy function works correctly`() {
        val original = PostSummaryResponse(
            success = true,
            summary = "Original",
            errorMessage = null,
            cached = false
        )
        
        val copied = original.copy(summary = "Copied")
        
        assertEquals("Original", original.summary)
        assertEquals("Copied", copied.summary)
    }

    @Test
    fun `equals function compares correctly`() {
        val response1 = PostSummaryResponse(true, "Summary", null, false)
        val response2 = PostSummaryResponse(true, "Summary", null, false)
        val response3 = PostSummaryResponse(true, "Different", null, false)
        
        assertEquals(response1, response2)
        assertNotEquals(response1, response3)
    }

    @Test
    fun `both summary and error can be present`() {
        // Edge case - shouldn't happen normally but should be handled
        val response = PostSummaryResponse(
            success = false,
            summary = "Partial summary",
            errorMessage = "Processing interrupted",
            cached = false
        )
        
        assertFalse(response.success)
        assertEquals("Partial summary", response.summary)
        assertEquals("Processing interrupted", response.errorMessage)
    }

    @Test
    fun `toString contains all relevant fields`() {
        val response = PostSummaryResponse(true, "Test", null, true)
        
        val stringRepresentation = response.toString()
        
        assertTrue(stringRepresentation.contains("true"))
        assertTrue(stringRepresentation.contains("Test"))
    }
}
