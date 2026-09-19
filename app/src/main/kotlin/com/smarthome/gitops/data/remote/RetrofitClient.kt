package com.smarthome.gitops.data.remote

import com.smarthome.gitops.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton Retrofit client for the GitHub REST API.
 *
 * ─── SECURITY NOTE ────────────────────────────────────────────────────────────
 * The GitHub token is read EXCLUSIVELY from BuildConfig.GITHUB_TOKEN, which is
 * populated at build time from local.properties. It is NEVER hardcoded here or
 * anywhere in the Kotlin source files.
 * ─────────────────────────────────────────────────────────────────────────────
 */
object RetrofitClient {

    private const val BASE_URL = "https://api.github.com/"

    /**
     * OkHttp interceptor that attaches the Authorization header to every request.
     * Token is read from BuildConfig (injected at compile-time from local.properties).
     */
    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val request = original.newBuilder()
            .header("Authorization", "token ${BuildConfig.GITHUB_TOKEN}")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .method(original.method, original.body)
            .build()
        chain.proceed(request)
    }

    /**
     * HTTP logging interceptor — logs request/response details in Logcat.
     * Set to BODY for development; reduce to NONE for production.
     */
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val githubApiService: GitHubApiService = retrofit.create(GitHubApiService::class.java)
}
