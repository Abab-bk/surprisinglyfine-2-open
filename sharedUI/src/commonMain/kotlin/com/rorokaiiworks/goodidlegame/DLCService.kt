package com.rorokaiiworks.goodidlegame

enum class DLC {
    Societal
}

interface DLCService {
    fun unlocked(dlc: DLC): Boolean
    fun enabled(dlc: DLC): Boolean
    fun goToDlcShop(dlc: DLC): Boolean
}