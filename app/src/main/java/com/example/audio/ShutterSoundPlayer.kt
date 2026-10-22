package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.exp

class ShutterSoundPlayer {
    private val sampleRate = 44100
    private val durationMs = 90
    private val numSamples = (sampleRate * durationMs / 1000)
    private val pcmData: ShortArray = ShortArray(numSamples)

    init {
        // Synthesize 1100Hz square wave with exponential decay matching prototype
        val freq = 1100.0
        val period = sampleRate / freq
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            // Square wave: positive for first half of period, negative for second half
            val phase = (i % period.toInt()) / period
            val square = if (phase < 0.5) 1.0 else -1.0

            // Exponential decay envelope from 1.0 to ~0.01
            val decay = exp(-t * 45.0)
            val amplitude = square * decay * 0.4 * Short.MAX_VALUE
            pcmData[i] = amplitude.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    fun play() {
        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(pcmData.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(pcmData, 0, pcmData.size)
            audioTrack.play()
            // Release after completion
            Thread {
                Thread.sleep(durationMs.toLong() + 50)
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (_: Exception) {}
            }.start()
        } catch (_: Exception) {
            // Audio failure safe ignore
        }
    }
}
