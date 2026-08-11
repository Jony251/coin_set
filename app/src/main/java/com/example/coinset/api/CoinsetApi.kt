package com.example.coinset.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface CoinsetApi {
    
    // Authentication
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): UserResponse
    
    @FormUrlEncoded
    @POST("api/auth/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): TokenResponse
    
    @GET("api/auth/me")
    suspend fun getCurrentUser(): UserResponse
    
    @POST("api/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): TokenResponse
    
    // Countries
    @GET("api/countries")
    suspend fun getCountries(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100,
        @Query("include") include: String? = null
    ): List<CountryResponse>
    
    @GET("api/countries/{id}")
    suspend fun getCountry(
        @Path("id") id: Int,
        @Query("include") include: String? = null
    ): CountryResponse
    
    @GET("api/countries/{id}")
    suspend fun getCountryWithRulers(
        @Path("id") id: Int,
        @Query("include") include: String = "rulers"
    ): CountryWithRulers
    
    @GET("api/countries/{id}")
    suspend fun getCountryWithRulersAndCoins(
        @Path("id") id: Int,
        @Query("include") include: String = "rulers.coins"
    ): CountryWithRulersAndCoins

    @POST("api/countries/search-log")
    suspend fun logCountrySearchMiss(@Body request: CountrySearchMissRequest): Response<Unit>

    // Rulers
    @GET("api/rulers")
    suspend fun getRulers(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100,
        @Query("country_id") countryId: Int? = null,
        @Query("period_start") periodStart: Int? = null,
        @Query("period_end") periodEnd: Int? = null,
        @Query("include") include: String? = null
    ): List<RulerResponse>
    
    @GET("api/rulers/{id}")
    suspend fun getRuler(
        @Path("id") id: Int,
        @Query("include") include: String? = null
    ): RulerResponse
    
    @GET("api/rulers/{id}")
    suspend fun getRulerWithCoins(
        @Path("id") id: Int,
        @Query("include") include: String = "coins"
    ): RulerWithCoins
    
    // Coins
    @GET("api/coins")
    suspend fun getCoins(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100,
        @Query("ruler_id") rulerId: Int? = null,
        @Query("country_id") countryId: Int? = null,
        @Query("metal_type") metalType: String? = null,
        @Query("year") year: Int? = null,
        @Query("rarity") rarity: String? = null,
        @Query("search") search: String? = null
    ): List<CoinResponse>
    
    @GET("api/coins/{id}")
    suspend fun getCoin(@Path("id") id: Int): CoinResponse
    
    // User Coins (Collection)
    @GET("api/user-coins")
    suspend fun getUserCoins(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100,
        @Query("condition") condition: String? = null
    ): List<UserCoinResponse>
    
    @POST("api/user-coins")
    suspend fun addUserCoin(@Body coin: UserCoinCreate): UserCoinResponse
    
    @PUT("api/user-coins/{id}")
    suspend fun updateUserCoin(
        @Path("id") id: Int,
        @Body coin: UserCoinUpdate
    ): UserCoinResponse
    
    @DELETE("api/user-coins/{id}")
    suspend fun deleteUserCoin(@Path("id") id: Int): Response<Unit>
    
    @Multipart
    @POST("api/user-coins/{id}/upload-image")
    suspend fun uploadCoinImage(
        @Path("id") id: Int,
        @Part file: MultipartBody.Part
    ): UserCoinResponse
    
    @GET("api/user-coins/stats/summary")
    suspend fun getCollectionStats(): CollectionStats
    
    // VIP
    @GET("api/vip/status")
    suspend fun getVipStatus(): VipStatus
    
    @POST("api/vip/activate")
    suspend fun activateVip(@Body request: VipActivateRequest): VipStatus
}
