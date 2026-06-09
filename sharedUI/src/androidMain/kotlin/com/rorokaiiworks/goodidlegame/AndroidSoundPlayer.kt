package com.rorokaiiworks.goodidlegame

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.rorokaiiworks.goodidlegame.core.ISoundPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AndroidSoundPlayer(private val context: Context) : ISoundPlayer {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(10)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundMap = mutableMapOf<String, Int>()
    private var currentSoundsVolume: Float = 1.0f

    private var mediaPlayer: MediaPlayer? = null
    private var currentMusicVolume: Float = 1.0f
    private var currentMusicName: String? = null

    override fun setSoundsVolume(volume: Float) {
        currentSoundsVolume = volume.coerceIn(0f, 1f)
    }

    override fun setMusicVolume(volume: Float) {
        currentMusicVolume = volume.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(currentMusicVolume, currentMusicVolume)
    }

    override fun playSound(soundName: String) {
        val soundId = soundMap[soundName]
        if (soundId != null) {
            soundPool.play(soundId, currentSoundsVolume, currentSoundsVolume, 1, 0, 1.0f)
        } else {
            scope.launch {
                try {
                    val assetDescriptor = context.assets.openFd("files/sounds/$soundName.ogg")
                    val id = soundPool.load(assetDescriptor, 1)
                    soundMap[soundName] = id

                    soundPool.setOnLoadCompleteListener { _, loadedId, _ ->
                        if (loadedId == id) {
                            soundPool.play(id, currentSoundsVolume, currentSoundsVolume, 1, 0, 1.0f)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun playMusic(musicName: String, loop: Boolean) {
        if (currentMusicName == musicName && mediaPlayer?.isPlaying == true) return

        stopMusic()

        mediaPlayer = MediaPlayer().apply {
            try {
                val assetDescriptor = context.assets.openFd("files/sounds/$musicName.ogg")
                setDataSource(assetDescriptor.fileDescriptor, assetDescriptor.startOffset, assetDescriptor.length)
                isLooping = loop
                setVolume(currentMusicVolume, currentMusicVolume)
                prepare()
                start()
                currentMusicName = musicName
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun stopMusic() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
        currentMusicName = null
    }

    fun release() {
        soundPool.release()
        stopMusic()
    }
}