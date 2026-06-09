package com.rorokaiiworks.goodidlegame.core

import androidx.compose.ui.graphics.Color

object Constants {
    const val ChannelsCount = 4

    val GameVersion = KotlinVersion(1, 101, 3)

    val primaryColor = Color(0xFF647A63)
    val errorColor = Color(0xFFB9707E)

    val darkBackground = Color(0x88030303)

    val RarityAlwaysColorLight = Color(0xFF6B6B6B)
    val RarityCommonColorLight = Color(0xFF7F6A6A)
    val RarityUnCommonColorLight = Color(0xFF4F7A50)
    val RarityRareColorLight = Color(0xFF3A5B7D)
    val RarityLegendaryColorLight = Color(0xFF9B3B3B)

    val RarityAlwaysColorDark = Color(0xFFBDBDBD)
    val RarityCommonColorDark = Color(0xFFD4A5A5)
    val RarityUnCommonColorDark = Color(0xFF92D050)
    val RarityRareColorDark = Color(0xFF7CB9E8)
    val RarityLegendaryColorDark = Color(0xFFFF6B6B)

    val NoticeColor = Color(0xFFFF6D6D)

//    val SaveKey = generateKeyFromStringHash("GoodIdleGameSaveKey")
}

//private fun generateKeyFromStringHash(input: String, keySize: Int = 256): ByteArray {
//    require(keySize == 128 || keySize == 192 || keySize == 256) {
//        "Key size must be 128, 192, or 256"
//    }
//
//    val digest = MessageDigest.getInstance("SHA-256")
//    val hash = digest.digest(input.toByteArray())
//
//    return when (keySize) {
//        128 -> hash.copyOf(16)  // AES-128: 16 bytes
//        192 -> hash.copyOf(24)  // AES-192: 24 bytes
//        256 -> hash             // AES-256: 32 bytes
//        else -> throw IllegalArgumentException("Invalid key size")
//    }
//}