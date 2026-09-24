package com.smarthome.gitops.data.repository

import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.smarthome.gitops.BuildConfig
import com.smarthome.gitops.data.model.IssueComment
import com.smarthome.gitops.data.model.UpdateFileRequest
import com.smarthome.gitops.data.remote.RetrofitClient

private const val TAG = "GitHubRepository"
private const val HOUSE_CONFIG_PATH = "house_config.json"

/**
 * Data layer repository — the single point of contact between the ViewModel
 * and the network. All heavy I/O runs on the caller's coroutine dispatcher
 * (MainViewModel calls this on Dispatchers.IO).
 *
 * ─── Lab 1 Responsibilities (Read-only) ─────────────────────────────────────
 *   1. Fetch all open Pull Requests for the configured repo.
 *   2. For each PR, fetch its comments.
 *   3. Return a flat list of [CommentWithPrInfo] for analysis.
 *
 * ─── Lab 2 Responsibilities (Write operations) ───────────────────────────────
 *   4. Close a Pull Request (Force Reject).
 *   5. Read → modify → write house_config.json, then close PR (Force Merge).
 *
 * All methods throw on network/API failure — the ViewModel catches and
 * updates UI state / emits an OperationResult.Failure event.
 */
class GitHubRepository {

    private val api = RetrofitClient.githubApiService
    private val owner = BuildConfig.GITHUB_OWNER
    private val repo = BuildConfig.GITHUB_REPO

    // ─── Lab 1: Read Operations ──────────────────────────────────────────────

    /**
     * Returns all comments from all currently-open Pull Requests.
     * Throws on network failure — the ViewModel catches and updates UI state.
     */
    suspend fun fetchAllOpenPrComments(): List<CommentWithPrInfo> {
        Log.d(TAG, "Fetching open PRs for $owner/$repo")
        val pullRequests = api.getOpenPullRequests(owner = owner, repo = repo)
        Log.d(TAG, "Found ${pullRequests.size} open PR(s)")

        val allComments = mutableListOf<CommentWithPrInfo>()

        for (pr in pullRequests) {
            Log.d(TAG, "Fetching comments for PR #${pr.number}: ${pr.title}")
            val comments = api.getPullRequestComments(
                owner = owner,
                repo = repo,
                pullNumber = pr.number
            )
            Log.d(TAG, "PR #${pr.number} has ${comments.size} comment(s)")

            comments.forEach { comment ->
                allComments.add(
                    CommentWithPrInfo(
                        comment = comment,
                        prNumber = pr.number,
                        prTitle = pr.title
                    )
                )
            }
        }

        return allComments
    }

    /**
     * Returns the count of currently open Pull Requests (for UI display).
     */
    suspend fun fetchOpenPrCount(): Int {
        return api.getOpenPullRequests(owner = owner, repo = repo).size
    }

    // ─── Lab 2: Write Operations ─────────────────────────────────────────────

    /**
     * Force Reject — closes the PR without merging.
     *
     * GitHub API: PATCH /repos/{owner}/{repo}/pulls/{pull_number}
     * Body: { "state": "closed" }
     *
     * @param prNumber The Pull Request number to close.
     * @throws Exception on network failure or HTTP error — caught by ViewModel.
     */
    suspend fun closePullRequest(prNumber: Int) {
        Log.d(TAG, "Force Reject: closing PR #$prNumber")
        api.closePullRequest(owner = owner, repo = repo, pullNumber = prNumber)
        Log.d(TAG, "PR #$prNumber closed successfully (Force Reject)")
    }

    /**
     * Force Merge — full GitOps write sequence:
     *
     *   Step 1: GET house_config.json → retrieve current content + blob SHA
     *   Step 2: Base64-decode → parse JSON → apply operator changes
     *           (target_temperature = 17.0, last_updated_by = "Android-Operator")
     *   Step 3: Re-encode to Base64 (NO_WRAP — no newlines, required by GitHub)
     *   Step 4: PUT house_config.json → direct commit to main branch
     *   Step 5: PATCH PR → close it (state = "closed")
     *
     * This implements the closed GitOps loop:
     *   Mobile app → GitHub API → Repository state changes → Background poll
     *   detects no open alarming PRs → ViewModel resets to Normal → UI goes green.
     *
     * @param prNumber The Pull Request number to merge and close.
     * @throws Exception on any step failure — caught and surfaced by ViewModel.
     */
    suspend fun updateFileAndClosePr(prNumber: Int) {
        Log.d(TAG, "Force Merge: starting sequence for PR #$prNumber")

        // ── Step 1: Fetch current file content + blob SHA ────────────────────
        val fileContent = api.getFileContent(
            owner = owner,
            repo = repo,
            path = HOUSE_CONFIG_PATH
        )
        Log.d(TAG, "Fetched $HOUSE_CONFIG_PATH — blob SHA: ${fileContent.sha}")

        // ── Step 2: Decode Base64 → parse JSON → modify fields ───────────────
        // GitHub wraps content in newlines every 60 chars — strip them first.
        val rawBase64 = fileContent.content.replace("\n", "").replace("\r", "")
        val decodedBytes = Base64.decode(rawBase64, Base64.DEFAULT)
        val currentJson = String(decodedBytes, Charsets.UTF_8)
        Log.d(TAG, "Current $HOUSE_CONFIG_PATH content: $currentJson")

        val gson = Gson()
        val jsonObject = gson.fromJson(currentJson, JsonObject::class.java)
        jsonObject.addProperty("target_temperature", 17.0)
        jsonObject.addProperty("last_updated_by", "Android-Operator")

        val updatedJson = gson.toJson(jsonObject)
        Log.d(TAG, "Updated $HOUSE_CONFIG_PATH content: $updatedJson")

        // ── Step 3: Re-encode to Base64 (NO_WRAP avoids line-break issues) ───
        val encodedContent = Base64.encodeToString(
            updatedJson.toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP
        )

        // ── Step 4: PUT updated file directly to main branch ─────────────────
        val updateRequest = UpdateFileRequest(
            message = "Force Merge: Android-Operator approved temperature change to 17.0°C [PR #$prNumber]",
            content = encodedContent,
            sha = fileContent.sha,
            branch = "main"
        )
        api.updateFile(
            owner = owner,
            repo = repo,
            path = HOUSE_CONFIG_PATH,
            body = updateRequest
        )
        Log.d(TAG, "$HOUSE_CONFIG_PATH committed to main branch successfully")

        // ── Step 5: Close the Pull Request ───────────────────────────────────
        api.closePullRequest(owner = owner, repo = repo, pullNumber = prNumber)
        Log.d(TAG, "PR #$prNumber closed — Force Merge sequence complete. GitOps loop closed.")
    }
}

/**
 * Wraps an [IssueComment] with the Pull Request context it belongs to,
 * so the UI can show which PR triggered the alert.
 */
data class CommentWithPrInfo(
    val comment: IssueComment,
    val prNumber: Int,
    val prTitle: String
)
