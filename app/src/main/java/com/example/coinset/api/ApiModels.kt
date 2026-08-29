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
/**
 * periods arrives only with ?include=periods, and the backend then *replaces*
 * periods_count with the list itself rather than sending both - so a country
 * fetched without the include has an empty list and a zero count, and any
 * screen reading one has to cope with the other being absent.
 */
data class CountryResponse(
    val id: Int,
    val name: String,
    val code: String,
    val description: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("periods_count") val periodsCount: Int = 0,
    val periods: List<PeriodResponse> = emptyList()
)

data class CountrySearchMissRequest(
    @SerializedName("search_query") val searchQuery: String
)

/**
 * imageUrl is the era's own historical flag - the imperial black-yellow-white,
 * the Soviet red banner, the modern tricolour - served as an absolute URL from
 * our server, same shape as Coin.image_url. It is null until those files are
 * in place, and null for most countries after that, so the period picker never
 * assumes it: an era with no flag falls back to a monogram disc of the same
 * size and the row of circles stays a row of circles.
 */
data class PeriodResponse(
    val id: Int,
    @SerializedName("country_id") val countryId: Int,
    val name: String,
    val code: String,
    @SerializedName("period_start") val periodStart: Int,
    @SerializedName("period_end") val periodEnd: Int?,
    val description: String?,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("country_name") val countryName: String?,
    @SerializedName("rulers_count") val rulersCount: Int = 0
)

data class PeriodWithRulers(
    val id: Int,
    @SerializedName("country_id") val countryId: Int,
    val name: String,
    val code: String,
    @SerializedName("period_start") val periodStart: Int,
    @SerializedName("period_end") val periodEnd: Int?,
    val description: String?,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("country_name") val countryName: String?,
    @SerializedName("rulers_count") val rulersCount: Int = 0,
    val rulers: List<RulerResponse> = emptyList()
)

/**
 * imageUrl is an absolute URL to a portrait on our own server, same shape as
 * Coin.image_url, and is null for every ruler outside the 14 Russian emperors
 * that have been filled in - the screens fall back to a monogram rather than a
 * broken image.
 *
 * description now carries 3-5 real sentences (280-460 chars), so nothing may
 * assume it fits on one line.
 *
 * Chronological ordering is the server's job (period_start, then period_end) -
 * the client renders the list in the order it arrives and must not re-sort.
 */
data class RulerResponse(
    val id: Int,
    val name: String,
    @SerializedName("period_id") val periodId: Int,
    @SerializedName("period_start") val periodStart: Int,
    @SerializedName("period_end") val periodEnd: Int,
    val description: String?,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("period_name") val periodName: String?,
    @SerializedName("country_id") val countryId: Int?,
    @SerializedName("country_name") val countryName: String?,
    @SerializedName("coins_count") val coinsCount: Int = 0
)

data class RulerWithCoins(
    val id: Int,
    val name: String,
    @SerializedName("period_id") val periodId: Int,
    @SerializedName("period_start") val periodStart: Int,
    @SerializedName("period_end") val periodEnd: Int,
    val description: String?,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("period_name") val periodName: String?,
    @SerializedName("country_id") val countryId: Int?,
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
    // The last year of striking, for rows that stand for a whole type rather
    // than for one year. The catalog holds both shapes: the Empire's gold is
    // one row per year, while the silver rouble of Nicholas II is a single row
    // covering 1895-1915. Showing only `year` turned that one into "1895" and
    // the twenty years behind it vanished - a reader had every reason to
    // believe the catalog held a single 1895 coin. Null for a one-year row,
    // and null everywhere until the column ships, so the screens fall back to
    // the plain year on their own.
    @SerializedName("year_end") val yearEnd: Int? = null,
    val weight: Double?,
    val diameter: Double?,
    val description: String?,
    @SerializedName("image_url") val imageUrl: String?,
    // Both nullable and empty for most of the catalog: only the Empire's gold
    // has them filled in so far. mintMaster is what tells two otherwise
    // identical coins of the same year apart ("10 рублей" АГ vs ЭБ), so the
    // screens treat it as an identifier, not as one more spec.
    val edge: String?,
    @SerializedName("mint_master") val mintMaster: String?,
    val rarity: String,
    val series: String?,
    @SerializedName("rarity_code") val rarityCode: String?,
    // Which mint struck the coin, plus its run. The mintage columns are free
    // text, not numbers: the backend stores what the source actually says,
    // which can be "12,3 млн (1826–31)" or "не менее 555 510 192 (1897–1917)".
    // "не менее" is a precise claim - a year with no surviving figures was
    // left out of the sum - so the UI must show it whole, never rounded or
    // cut to a number. That is what makes these values long, and why the
    // spec grid gives an over-long value the full width of the screen.
    val mint: String?,
    @SerializedName("mintage_spmd") val mintageSpmd: String?,
    @SerializedName("mintage_mmd") val mintageMmd: String?,
    // Run of a mint that has no column of its own (Yekaterinburg, Suzun).
    @SerializedName("mintage_other") val mintageOther: String?,
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
    val status: String = "owned",
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
    // Catalog photo of the underlying coin. Requested from the backend so the
    // collection shelf has something to show when the user hasn't uploaded
    // their own photo; null until it ships, and the shelf falls back to a
    // metal-tinted disc.
    @SerializedName("coin_image_url") val coinImageUrl: String? = null,
    @SerializedName("ruler_name") val rulerName: String?,
    @SerializedName("country_name") val countryName: String?
)

data class UserCoinCreate(
    @SerializedName("coin_id") val coinId: Int,
    val condition: String = "UNC",
    val status: String = "owned",
    @SerializedName("purchase_price") val purchasePrice: Double? = null,
    @SerializedName("purchase_date") val purchaseDate: String? = null,
    val notes: String? = null,
    @SerializedName("custom_fields") val customFields: Map<String, Any>? = null
)

data class UserCoinUpdate(
    val condition: String? = null,
    val status: String? = null,
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

// News Models
data class NewsArticleResponse(
    val id: Int,
    val title: String,
    val source: String,
    val summary: String,
    val url: String?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("published_at") val publishedAt: String
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

