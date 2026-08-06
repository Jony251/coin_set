package com.example.coinset.api

import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.util.concurrent.TimeUnit

/**
 * Refreshes the access token on a 401 and retries the original request.
 * Uses its own OkHttpClient (no interceptors/authenticator) for the refresh
 * call itself, to avoid recursing back into this authenticator.
 */
object TokenAuthenticator : Authenticator {

    private val gson = Gson()

    private val refreshClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null

        val failedToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            ?: return null

        synchronized(this) {
            val currentToken = TokenManager.getAccessToken()
            if (currentToken != null && currentToken != failedToken) {
                // Another request already refreshed the token while we were waiting.
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val refreshToken = TokenManager.getRefreshToken() ?: return null
            val newTokens = refreshTokens(refreshToken)
            if (newTokens == null) {
                runBlocking { TokenManager.clearTokens() }
                return null
            }

            runBlocking { TokenManager.saveTokens(newTokens.accessToken, newTokens.refreshToken) }

            return response.request.newBuilder()
                .header("Authorization", "Bearer ${newTokens.accessToken}")
                .build()
        }
    }

    private fun refreshTokens(refreshToken: String): TokenResponse? {
        return try {
            val body = gson.toJson(RefreshTokenRequest(refreshToken))
                .toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(RetrofitClient.BASE_URL + "api/auth/refresh")
                .post(body)
                .build()
            refreshClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) null
                else resp.body?.string()?.let { gson.fromJson(it, TokenResponse::class.java) }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var priorResponse = response.priorResponse
        while (priorResponse != null) {
            result++
            priorResponse = priorResponse.priorResponse
        }
        return result
    }
}
