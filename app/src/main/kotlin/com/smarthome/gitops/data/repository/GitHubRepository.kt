package com.smarthome.gitops.data.repository

import android.util.Log
import com.smarthome.gitops.BuildConfig
import com.smarthome.gitops.data.model.IssueComment
import com.smarthome.gitops.data.remote.RetrofitClient

private const val TAG = "GitHubRepository"

/**
 * Data layer repository — the single point of contact between the ViewModel
 * and the network. All heavy I/O runs on the caller's coroutine dispatcher
 * (MainViewModel calls this on Dispatchers.IO).
 *
 * Responsibilities:
 *   1. Fetch all open Pull Requests for the configured repo.
 *   2. For each PR, fetch its comments.
 *   3. Return a flat list of [CommentWithPrInfo] for analysis.
 */
class GitHubRepository {

    private val api = RetrofitClient.githubApiService
    private val owner = BuildConfig.GITHUB_OWNER
    private val repo = BuildConfig.GITHUB_REPO

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
