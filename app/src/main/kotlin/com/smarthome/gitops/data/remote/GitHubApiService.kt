package com.smarthome.gitops.data.remote

import com.smarthome.gitops.data.model.IssueComment
import com.smarthome.gitops.data.model.PullRequest
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit service interface for the GitHub REST API.
 *
 * Base URL: https://api.github.com/
 *
 * The Authorization header is injected by RetrofitClient's OkHttp interceptor —
 * no token ever appears here in source code.
 */
interface GitHubApiService {

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
}
