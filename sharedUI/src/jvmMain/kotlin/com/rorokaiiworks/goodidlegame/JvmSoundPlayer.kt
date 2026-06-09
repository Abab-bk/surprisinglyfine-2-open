package com.rorokaiiworks.goodidlegame

import com.rorokaiiworks.goodidlegame.core.ISoundPlayer
import goodidlegame.sharedui.generated.resources.Res
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.sound.sampled.*
import kotlin.math.ln

class JvmSoundPlayer : ISoundPlayer {
    private val clipCache = mutableMapOf<String, Clip>()
    private val scope = CoroutineScope(Dispatchers.IO)

    private var currentSoundsVolume: Float = 1.0f
    private var currentMusicVolume: Float = 1.0f

    private var backgroundMusicLine: SourceDataLine? = null
    private var isMusicPlaying = false

    override fun setSoundsVolume(volume: Float) {
        currentSoundsVolume = volume.coerceIn(0f, 1f)
        clipCache.values.forEach { applyVolumeToClip(it, currentSoundsVolume) }
    }

    override fun setMusicVolume(volume: Float) {
        currentMusicVolume = volume.coerceIn(0f, 1f)
        backgroundMusicLine?.let { applyVolumeToLine(it, currentMusicVolume) }
    }

    override fun playSound(soundName: String) {
        val cachedClip = clipCache[soundName]
        if (cachedClip != null) {
            trigger(cachedClip)
        } else {
            scope.launch {
                try {
                    val clip = loadClipFromBytes(Res.readBytes("files/sounds/$soundName.ogg"))
                    applyVolumeToClip(clip, currentSoundsVolume)
                    clipCache[soundName] = clip
                    trigger(clip)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun playMusic(musicName: String, loop: Boolean) {
        stopMusic()

        scope.launch {
            try {
                val baseStream = AudioSystem.getAudioInputStream(
                    Res.readBytes("files/sounds/$musicName.ogg").inputStream()
                )

                val baseFormat = baseStream.format
                val decodedFormat = AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.sampleRate, 16, baseFormat.channels,
                    baseFormat.channels * 2, baseFormat.sampleRate, false
                )

                val decodedStream = AudioSystem.getAudioInputStream(decodedFormat, baseStream)
                val info = DataLine.Info(SourceDataLine::class.java, decodedFormat)
                val line = AudioSystem.getLine(info) as SourceDataLine

                line.open(decodedFormat)
                line.start()
                backgroundMusicLine = line
                isMusicPlaying = true

                applyVolumeToLine(line, currentMusicVolume)

                val buffer = ByteArray(4096)
                var bytesRead = 0

                while (isMusicPlaying && decodedStream.read(buffer).also { bytesRead = it } != -1) {
                    line.write(buffer, 0, bytesRead)
                }

                if (isMusicPlaying && loop) {
                    playMusic(musicName, true)
                } else {
                    line.drain()
                    line.close()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun stopMusic() {
        isMusicPlaying = false
        backgroundMusicLine?.stop()
        backgroundMusicLine?.close()
        backgroundMusicLine = null
    }

    private fun applyVolumeToLine(line: Line, volume: Float) {
        try {
            if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                val gainControl = line.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                val dB = (ln(volume.toDouble().coerceAtLeast(0.0001)) / ln(10.0) * 20.0).toFloat()
                gainControl.value = dB.coerceIn(gainControl.minimum, gainControl.maximum)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun applyVolumeToClip(clip: Clip, volume: Float) {
        try {
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                val gainControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl

                // formula: dB = 20 * log10(volume)
                val dB = (ln(volume.toDouble().coerceAtLeast(0.0001)) / ln(10.0) * 20.0).toFloat()

                gainControl.value = dB.coerceIn(gainControl.minimum, gainControl.maximum)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadClipFromBytes(bytes: ByteArray): Clip {
        val baseStream = AudioSystem.getAudioInputStream(bytes.inputStream())

        val baseFormat = baseStream.format
        val decodedFormat = AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            baseFormat.sampleRate,
            16,                              // 16 bit
            baseFormat.channels,
            baseFormat.channels * 2,         // frameSize
            baseFormat.sampleRate,
            false                            // little-endian
        )

        val decodedStream = AudioSystem.getAudioInputStream(decodedFormat, baseStream)
        val clip = AudioSystem.getClip()

        clip.open(decodedStream)

        decodedStream.close()
        baseStream.close()

        return clip
    }

    private fun trigger(clip: Clip) {
        if (clip.isRunning) clip.stop()
        clip.framePosition = 0
        clip.start()
    }
}