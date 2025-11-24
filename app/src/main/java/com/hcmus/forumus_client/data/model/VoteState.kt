package com.hcmus.forumus_client.data.model

/**
 * Represents the current user's vote on a post.
 * NONE  -> no vote
 * UP    -> user upvoted
 * DOWN  -> user downvoted
 */
enum class VoteState { NONE, UP, DOWN }