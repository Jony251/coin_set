package com.example.coinset

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
    val rulerId: String = ""
)