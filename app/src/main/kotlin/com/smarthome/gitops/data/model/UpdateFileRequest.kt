package com.smarthome.gitops.data.model

import com.google.gson.annotations.SerializedName

/**
 * Request body for: PUT /repos/{owner}/{repo}/contents/{path}
 * Docs: https://docs.github.com/en/rest/repos/contents#create-or-update-file-contents
 *
 * Used by Force Merge to write the updated house_config.json directly to main.
 *
 * IMPORTANT:
 *   [content] must be Base64-encoded (NO line wrapping — use Base64.NO_WRAP).
 *   [sha]     must equal the current blob SHA returned by the prior GET contents
 *             call. GitHub rejects the PUT if the SHA doesn't match (race guard).
 */
data class UpdateFileRequest(
    @SerializedName("message")
    val message: String,          // Git commit message for this change

    @SerializedName("content")
    val content: String,          // NEW file content — Base64-encoded, no newlines

    @SerializedName("sha")
    val sha: String,              // Current blob SHA from GET contents response

    @SerializedName("branch")
    val branch: String = "main"   // Target branch to commit directly into
)
