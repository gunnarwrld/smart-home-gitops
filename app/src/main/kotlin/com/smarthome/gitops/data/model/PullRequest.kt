package com.smarthome.gitops.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents a single Pull Request returned by:
 * GET /repos/{owner}/{repo}/pulls
 *
 * Only the fields we need are mapped; Gson ignores extras.
 */
data class PullRequest(
    @SerializedName("number")
    val number: Int,

    @SerializedName("title")
    val title: String,

    @SerializedName("state")
    val state: String,           // "open" | "closed"

    @SerializedName("user")
    val user: GitHubUser,

    @SerializedName("body")
    val body: String?,           // PR description (may itself be adversarial)

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("html_url")
    val htmlUrl: String
)

data class GitHubUser(
    @SerializedName("login")
    val login: String
)
