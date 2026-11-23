package com.example.emulator

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

class AtariAudioEngine {
    private val sampleRate = 22050
    private var audioTrack: AudioTrack? = null
    @Volatile private var running = false
    @Volatile var enabled: Boolean = true
    @Volatile var volume: Float = 0.85f

    // TIA 2-channel registers
    @Volatile private var audc0 = 0
    @Volatile private var audf0 = 0
    @Volatile private var audv0 = 0
    @Volatile private var audc1 = 0
    @Volatile private var audf1 = 0
    @Volatile private var audv1 = 0

    private var poly4_0 = 0x0F
    private var poly5_0 = 0x1F
    private var poly9_0 = 0x1FF
    private var div0 = 0
    private var toneFlip0 = 0
    private var lp0 = 0f

    private var poly4_1 = 0x0F
    private var poly5_1 = 0x1F
    private var poly9_1 = 0x1FF
    private var div1 = 0
    private var toneFlip1 = 0
    private var lp1 = 0f

    private var audioThread: Thread? = null

    fun setChannel0(audc: Int, audf: Int, audv: Int) {
        audc0 = audc and 0x0F
        audf0 = audf and 0x1F
        audv0 = audv and 0x0F
    }

    fun setChannel1(audc: Int, audf: Int, audv: Int) {
        audc1 = audc and 0x0F
        audf1 = audf and 0x1F
        audv1 = audv and 0x0F
    }

    fun start() {
        if (running) return
        running = true
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(1024)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
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
            .setBufferSizeInBytes(minBuf * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()

        audioThread = thread(start = true, name = "AtariAudioThread") {
            val chunk = ShortArray(368)
            while (running) {
                if (!enabled || (audv0 == 0 && audv1 == 0)) {
                    lp0 *= 0.9f
                    lp1 *= 0.9f
                    for (i in chunk.indices) {
                        lp0 *= 0.92f
                        chunk[i] = (lp0 * 8000f).toInt().coerceIn(-32767, 32767).toShort()
                    }
                } else {
                    val master = volume.coerceIn(0f, 1f)
                    for (i in chunk.indices) {
                        val s0 = sampleChannel0()
                        val s1 = sampleChannel1()
                        val mixed = ((s0 + s1) * 0.5f * master * 14000f).toInt().coerceIn(-32767, 32767)
                        chunk[i] = mixed.toShort()
                    }
                }
                try {
                    audioTrack?.write(chunk, 0, chunk.size)
                } catch (_: Throwable) {
                    break
                }
            }
        }
    }

    private fun sampleChannel0(): Float {
        if (audv0 == 0 || audc0 == 0) {
            lp0 *= 0.9f
            return lp0
        }
        div0++
        if (div0 > audf0) {
            div0 = 0
            toneFlip0 = toneFlip0 xor 1
            poly4_0 = ((poly4_0 shr 1) or (((poly4_0 xor (poly4_0 shr 1)) and 1) shl 3)) and 0x0F
            poly5_0 = ((poly5_0 shr 1) or (((poly5_0 xor (poly5_0 shr 2)) and 1) shl 4)) and 0x1F
            poly9_0 = ((poly9_0 shr 1) or (((poly9_0 xor (poly9_0 shr 4)) and 1) shl 8)) and 0x1FF
        }
        val bit = when (audc0) {
            1, 2 -> (poly4_0 and 1)
            3 -> if ((poly5_0 and 1) != 0) (poly4_0 and 1) else 0
            4, 5, 12, 13 -> toneFlip0
            6, 10 -> (poly5_0 and 1)
            7, 9 -> (poly5_0 and 1) xor (poly4_0 and 1)
            8 -> (poly9_0 and 1)
            11 -> 1
            else -> (poly4_0 and 1)
        }
        val amp = audv0 / 15f
        val target = if (bit != 0) amp else -amp
        lp0 += 0.35f * (target - lp0)
        return lp0
    }

    private fun sampleChannel1(): Float {
        if (audv1 == 0 || audc1 == 0) {
            lp1 *= 0.9f
            return lp1
        }
        div1++
        if (div1 > audf1) {
            div1 = 0
            toneFlip1 = toneFlip1 xor 1
            poly4_1 = ((poly4_1 shr 1) or (((poly4_1 xor (poly4_1 shr 1)) and 1) shl 3)) and 0x0F
            poly5_1 = ((poly5_1 shr 1) or (((poly5_1 xor (poly5_1 shr 2)) and 1) shl 4)) and 0x1F
            poly9_1 = ((poly9_1 shr 1) or (((poly9_1 xor (poly9_1 shr 4)) and 1) shl 8)) and 0x1FF
        }
        val bit = when (audc1) {
            1, 2 -> (poly4_1 and 1)
            3 -> if ((poly5_1 and 1) != 0) (poly4_1 and 1) else 0
            4, 5, 12, 13 -> toneFlip1
            6, 10 -> (poly5_1 and 1)
            7, 9 -> (poly5_1 and 1) xor (poly4_1 and 1)
            8 -> (poly9_1 and 1)
            11 -> 1
            else -> (poly4_1 and 1)
        }
        val amp = audv1 / 15f
        val target = if (bit != 0) amp else -amp
        lp1 += 0.35f * (target - lp1)
        return lp1
    }

    fun stop() {
        running = false
        audioThread?.interrupt()
        audioThread = null
        runCatching {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
        }
        audioTrack = null
    }

    companion object {
        fun playStartupChime() {
            thread(start = true, name = "AtariSplashChime") {
                try {
                    val sr = 22050
                    val notes = listOf(440.0 to 70, 554.37 to 70, 659.25 to 70, 880.0 to 160)
                    val totalSamples = notes.sumOf { (sr * it.second) / 1000 }
                    val pcm = ShortArray(totalSamples)
                    var idx = 0
                    for ((freq, ms) in notes) {
                        val count = (sr * ms) / 1000
                        for (i in 0 until count) {
                            val env = 1f - (i.toFloat() / count.toFloat()) * 0.55f
                            val wave = if (sin(2.0 * PI * freq * i / sr) >= 0) 1f else -1f
                            pcm[idx++] = (wave * env * 6000f).toInt().toShort()
                        }
                    }
                    val track = AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sr)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(pcm.size * 2)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build()
                    track.write(pcm, 0, pcm.size)
                    track.play()
                    Thread.sleep(420)
                    track.release()
                } catch (_: Throwable) {
                }
            }
        }
    }
}
