package com.rorokaiiworks.goodidlegame.dlcSocietal.game.populations

enum class PopulationTier(
    val id: String,
    val title: String,
    val needConsumeBucksMultiplier: Float,
) {
    Farmer("farmer", "Farmer", 1.0f),
    Worker("worker", "Worker", 1.35f),
    Astrologer("astrologer", "Astrologer", 1.8f),
    Alchemist("alchemist", "Alchemist", 2.4f)
}
