package com.smarthome.gitops.data.model

import com.google.gson.annotations.SerializedName

/**
 * Request body for: PATCH /repos/{owner}/{repo}/pulls/{pull_number}
 * Docs: https://docs.github.com/en/rest/pulls/pulls#update-a-pull-request
 *
 * Setting [state] to "closed" closes the Pull Request without merging it.
 * Used by both Force Reject (close only) and Force Merge (close after file update).
 */
data class ClosePrRequest(
    @SerializedName("state")
    val state: String = "closed"
)
