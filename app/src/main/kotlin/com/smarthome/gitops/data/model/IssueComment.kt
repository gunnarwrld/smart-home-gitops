package com.smarthome.gitops.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents a single comment on a GitHub Issue / Pull Request, returned by:
 * GET /repos/{owner}/{repo}/issues/{pull_number}/comments
 *
 * Pull Request comments share the Issues API endpoint in GitHub's REST API.
 */
data class IssueComment(
    @SerializedName("id")
    val id: Long,

    @SerializedName("user")
    val user: GitHubUser,

    @SerializedName("body")
    val body: String,            // The raw comment text — fed to DeceptionDetector

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("html_url")
    val htmlUrl: String
)
