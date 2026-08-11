package com.example.coinset.api

import com.google.gson.annotations.SerializedName

// Authentication Models
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("token_type") val tokenType: String
)

data class RefreshTokenRequest(
    @SerializedName("refresh_token") val refreshToken: String
)

data class UserResponse(
    val id: Int,
    val username: String,
    val email: String,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("is_admin") val isAdmin: Boolean
)

// Catalog Models
data class CountryResponse(
    val id: Int,
    val name: String,
    val code: String,
    val description: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("rulers_count") val rulersCount: Int = 0
)

data class CountryWithRulers(
    val id: Int,
    val name: String,
    val code: String,
    val description: String?,
    @SerializedName("created_at") val createdAt: String,
    val rulers: List<RulerResponse> = emptyList()
)

data class CountryWithRulersAndCoins(
    val id: Int,
    val name: String,
    val code: String,
    val description: String?,
    @SerializedName("created_at") val createdAt: String,
    val rulers: List<RulerWithCoins> = emptyList()
)

data class CountrySearchMissRequest(
    @SerializedName("search_query") val searchQuery: String
)

data class RulerResponse(
    val id: Int,
    val name: String,
    @SerializedName("country_id") val countryId: Int,
    @SerializedName("period_start") val periodStart: Int,
    @SerializedName("period_end") val periodEnd: Int,
    val description: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("country_name") val countryName: String?,
    @SerializedName("coins_count") val coinsCount: Int = 0
)

data class RulerWithCoins(
    val id: Int,
    val name: String,
    @SerializedName("country_id") val countryId: Int,
    @SerializedName("period_start") val periodStart: Int,
    @SerializedName("period_end") val periodEnd: Int,
    val description: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("country_name") val countryName: String?,
    @SerializedName("coins_count") val coinsCount: Int = 0,
    val coins: List<CoinResponse> = emptyList()
)

data class CoinResponse(
    val id: Int,
    val name: String,
    @SerializedName("ruler_id") val rulerId: Int,
    @SerializedName("metal_type") val metalType: String,
    val denomination: String?,
    val year: Int?,
    val weight: Double?,
    val diameter: Double?,
    val description: String?,
    @SerializedName("image_url") val imageUrl: String?,
    val rarity: String,
    val series: String?,
    @SerializedName("rarity_code") val rarityCode: String?,
    @SerializedName("mintage_spmd") val mintageSpmd: String?,
    @SerializedName("mintage_mmd") val mintageMmd: String?,
    @SerializedName("price_estimate") val priceEstimate: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("ruler_name") val rulerName: String?,
    @SerializedName("country_name") val countryName: String?
)

// User Collection Models
data class UserCoinResponse(
    val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("coin_id") val coinId: Int,
    val condition: String,
    @SerializedName("purchase_price") val purchasePrice: Double?,
    @SerializedName("purchase_date") val purchaseDate: String?,
    @SerializedName("selling_price") val sellingPrice: Double?,
    @SerializedName("current_weight") val currentWeight: Double?,
    val notes: String?,
    val images: List<String> = emptyList(),
    @SerializedName("custom_fields") val customFields: Map<String, Any>?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("coin_name") val coinName: String?,
    @SerializedName("coin_year") val coinYear: Int?,
    @SerializedName("coin_metal_type") val coinMetalType: String?,
    @SerializedName("ruler_name") val rulerName: String?,
    @SerializedName("country_name") val countryName: String?
)

data class UserCoinCreate(
    @SerializedName("coin_id") val coinId: Int,
    val condition: String = "UNC",
    @SerializedName("purchase_price") val purchasePrice: Double? = null,
    @SerializedName("purchase_date") val purchaseDate: String? = null,
    val notes: String? = null,
    @SerializedName("custom_fields") val customFields: Map<String, Any>? = null
)

data class UserCoinUpdate(
    val condition: String? = null,
    @SerializedName("purchase_price") val purchasePrice: Double? = null,
    @SerializedName("purchase_date") val purchaseDate: String? = null,
    @SerializedName("selling_price") val sellingPrice: Double? = null,
    @SerializedName("current_weight") val currentWeight: Double? = null,
    val notes: String? = null,
    @SerializedName("custom_fields") val customFields: Map<String, Any>? = null
)

data class CollectionStats(
    @SerializedName("total_coins") val totalCoins: Int,
    @SerializedName("total_purchase_value") val totalPurchaseValue: Double,
    @SerializedName("total_selling_value") val totalSellingValue: Double,
    @SerializedName("coins_by_condition") val coinsByCondition: Map<String, Int>,
    @SerializedName("coins_by_metal") val coinsByMetal: Map<String, Int>
)

// VIP Models
data class VipStatus(
    @SerializedName("is_vip") val isVip: Boolean,
    @SerializedName("vip_activated_at") val vipActivatedAt: String?,
    @SerializedName("vip_expires_at") val vipExpiresAt: String?,
    @SerializedName("days_remaining") val daysRemaining: Int?
)

data class VipActivateRequest(
    @SerializedName("payment_token") val paymentToken: String? = null
)

