package com.smarthome.gitops.data.remote

import com.smarthome.gitops.data.model.ClosePrRequest
import com.smarthome.gitops.data.model.GitHubFileContent
import com.smarthome.gitops.data.model.IssueComment
import com.smarthome.gitops.data.model.PullRequest
import com.smarthome.gitops.data.model.UpdateFileRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit service interface for the GitHub REST API.
 *
 * Base URL: https://api.github.com/
 *
 * The Authorization header is injected by RetrofitClient's OkHttp interceptor —
 * no token ever appears here in source code.
 *
 * ─── Lab 1 (Read-only) ───────────────────────────────────────────────────────
 *   getOpenPullRequests   GET  /repos/{owner}/{repo}/pulls
 *   getPullRequestComments GET /repos/{owner}/{repo}/issues/{pr}/comments
 *
 * ─── Lab 2 (Write operations) ────────────────────────────────────────────────
 *   closePullRequest      PATCH /repos/{owner}/{repo}/pulls/{pr}
 *   getFileContent        GET   /repos/{owner}/{repo}/contents/{path}
 *   updateFile            PUT   /repos/{owner}/{repo}/contents/{path}
 */
interface GitHubApiService {

    // ─── Lab 1: Read Endpoints ───────────────────────────────────────────────

    /**
     * Fetches all open Pull Requests for the configured repository.
     *
     * GitHub API: GET /repos/{owner}/{repo}/pulls
     * Docs: https://docs.github.com/en/rest/pulls/pulls#list-pull-requests
     */
    @GET("repos/{owner}/{repo}/pulls")
    suspend fun getOpenPullRequests(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "open",
        @Query("per_page") perPage: Int = 100
    ): List<PullRequest>

    /**
     * Fetches all comments on a specific Pull Request.
     *
     * GitHub API: GET /repos/{owner}/{repo}/issues/{pull_number}/comments
     * Note: GitHub routes PR comments through the Issues Comments API endpoint.
     */
    @GET("repos/{owner}/{repo}/issues/{pull_number}/comments")
    suspend fun getPullRequestComments(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("pull_number") pullNumber: Int,
        @Query("per_page") perPage: Int = 100
    ): List<IssueComment>

    // ─── Lab 2: Write Endpoints ──────────────────────────────────────────────

    /**
     * Closes (or reopens) a Pull Request by patching its state field.
     *
     * GitHub API: PATCH /repos/{owner}/{repo}/pulls/{pull_number}
     * Docs: https://docs.github.com/en/rest/pulls/pulls#update-a-pull-request
     *
     * Used by:
     *   • Force Reject — close PR without merging (body: state="closed")
     *   • Force Merge  — final step after writing house_config.json to main
     *
     * Requires token scope: repo (full control)
     * Returns: the updated PullRequest object (state will be "closed")
     */
    @PATCH("repos/{owner}/{repo}/pulls/{pull_number}")
    suspend fun closePullRequest(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("pull_number") pullNumber: Int,
        @Body body: ClosePrRequest = ClosePrRequest()
    ): PullRequest

    /**
     * Fetches the raw content and metadata of a single repository file.
     *
     * GitHub API: GET /repos/{owner}/{repo}/contents/{path}
     * Docs: https://docs.github.com/en/rest/repos/contents#get-repository-content
     *
     * Used by Force Merge to read house_config.json before modifying it.
     * The returned [GitHubFileContent.sha] is REQUIRED for the subsequent PUT.
     */
    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getFileContent(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String,
        @Query("ref") ref: String = "main"
    ): GitHubFileContent

    /**
     * Creates or updates a file in the repository via a direct commit.
     *
     * GitHub API: PUT /repos/{owner}/{repo}/contents/{path}
     * Docs: https://docs.github.com/en/rest/repos/contents#create-or-update-file-contents
     *
     * Used by Force Merge to overwrite house_config.json on the main branch
     * with updated temperature and operator attribution.
     *
     * Requires token scope: repo (full control)
     * Branch protection: "Require a pull request before merging" must be OFF
     */
    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun updateFile(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String,
        @Body body: UpdateFileRequest
    ): Any   // Response body not needed — HTTP 200/201 signals success
}
