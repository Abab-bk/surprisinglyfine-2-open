package com.rorokaiiworks.goodidlegame.core

interface ISoundPlayer {
    fun setSoundsVolume(volume: Float)
    fun setMusicVolume(volume: Float)

    fun playSound(soundName: String)

    fun playMusic(musicName: String, loop: Boolean = true)
    fun stopMusic()
}