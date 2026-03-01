package com.example.coinset

data class Country(
    val id: String = "",
    val name: String = "",
    val flagUrl: String = ""
)

data class Period(
    val id: String = "",
    val name: String = "",
    val countryId: String = ""
)

data class Ruler(
    val id: String = "",
    val name: String = "",
    val nameEn: String = "",
    val periodId: String = "",
    val startYear: Int = 0,
    val endYear: Int = 0,
    val imageUrl: String = "",
    val title: String = ""
)

data class Coin(
    val id: String = "",
    val name: String = "",
    val denomination: String = "",
    val denominationValue: Int = 0,
    val diameter: Double = 0.0,
    val estimatedValueMax: Long = 0,
    val estimatedValueMin: Long = 0,
    val metal: String = "",
    val mint: String = "",
    val rarity: String = "",
    val rarityScore: Int = 0,
    val rulerId: String = "",
    val category: String = "",
    val year: Int = 0,
    val weight: Double = 0.0
)

data class UserCoin(
    val userId: String = "",
    val coinId: String = "",
    val count: Int = 1,
    val condition: String = "UNC",
    val purchasePrice: Double = 0.0,
    val sellingPrice: Double = 0.0
)
