package com.smarthome.gitops.data.model

import com.google.gson.annotations.SerializedName

/**
 * Response model for: GET /repos/{owner}/{repo}/contents/{path}
 * Docs: https://docs.github.com/en/rest/repos/contents#get-repository-content
 *
 * GitHub returns the file content Base64-encoded. We need:
 *   [sha]     — the current blob SHA required for any subsequent PUT update
 *              (GitHub uses this for optimistic concurrency / race prevention).
 *   [content] — the Base64-encoded file content to decode locally.
 */
data class GitHubFileContent(
    @SerializedName("sha")
    val sha: String,

    @SerializedName("content")
    val content: String,          // Base64-encoded file bytes (may contain \n line breaks)

    @SerializedName("encoding")
    val encoding: String,         // Always "base64" for files

    @SerializedName("path")
    val path: String,

    @SerializedName("name")
    val name: String
)
