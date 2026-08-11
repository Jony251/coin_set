package com.example.coinset.api

import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class AuthRepository(private val api: CoinsetApi = RetrofitClient.api) {
    
    suspend fun register(username: String, email: String, password: String): Result<UserResponse> {
        return try {
            val request = RegisterRequest(username, email, password)
            val response = api.register(request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun login(username: String, password: String): Result<TokenResponse> {
        return try {
            val response = api.login(username, password)
            TokenManager.saveTokens(response.accessToken, response.refreshToken)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getCurrentUser(): Result<UserResponse> {
        return try {
            val response = api.getCurrentUser()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class CatalogRepository(private val api: CoinsetApi = RetrofitClient.api) {
    
    suspend fun getCountries(include: String? = null): Result<List<CountryResponse>> {
        return try {
            val response = api.getCountries(include = include)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getCountryWithRulers(id: Int): Result<CountryWithRulers> {
        return try {
            val response = api.getCountryWithRulers(id)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getRulerWithCoins(id: Int): Result<RulerWithCoins> {
        return try {
            val response = api.getRulerWithCoins(id)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Fire-and-forget: records that a searched-for country isn't in the catalog. */
    suspend fun logCountrySearchMiss(query: String) {
        try {
            api.logCountrySearchMiss(CountrySearchMissRequest(query))
        } catch (e: Exception) {
            // Best-effort telemetry — a failed log call shouldn't affect the UI.
        }
    }

    suspend fun getCoins(rulerId: Int? = null, metalType: String? = null): Result<List<CoinResponse>> {
        return try {
            val response = api.getCoins(rulerId = rulerId, metalType = metalType)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCoin(id: Int): Result<CoinResponse> {
        return try {
            val response = api.getCoin(id)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class CollectionRepository(private val api: CoinsetApi = RetrofitClient.api) {
    
    suspend fun getUserCoins(): Result<List<UserCoinResponse>> {
        return try {
            val response = api.getUserCoins()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun addCoinToCollection(coinId: Int, condition: String, price: Double? = null): Result<UserCoinResponse> {
        return try {
            val request = UserCoinCreate(
                coinId = coinId,
                condition = condition,
                purchasePrice = price
            )
            val response = api.addUserCoin(request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getCollectionStats(): Result<CollectionStats> {
        return try {
            val response = api.getCollectionStats()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
