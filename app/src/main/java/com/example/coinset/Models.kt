package com.example.coinset

import com.google.firebase.Timestamp

data class Country(
    val id: String = "",
    val name: String = "",
    val nameEn: String = "",
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

data class CoinCatalogs(
    val bitkin: String = "",
    val ilin: String = "",
    val petrov: String = "",
    val rarity: String = ""
)

data class Coin(
    val id: String = "",
    val name: String = "",
    val denominationName: String = "", // Изменено с denomination
    val denominationValue: Int = 0,
    val diameter: Double = 0.0,
    val estimatedValueMax: Long = 0,
    val estimatedValueMin: Long = 0,
    val composition: String = "", // Изменено с metal
    val mint: String = "",
    val category: String = "",
    val year: Int = 0,
    val weight: Double = 0.0,
    val description: String = "",
    val rulerId: String = "",
    val catalogs: CoinCatalogs = CoinCatalogs() // Новая вложенная структура
)

data class UserCoin(
    val userId: String = "",
    val coinId: String = "",
    val count: Int = 1,
    val condition: String = "UNC",
    val purchasePrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val notes: String = "",
    val photoUrl: String = ""
)
