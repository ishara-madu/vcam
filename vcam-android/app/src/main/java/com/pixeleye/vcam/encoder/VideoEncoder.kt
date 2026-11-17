package com.pixeleye.vcam.encoder

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Bundle
import android.util.Log
import android.view.Surface
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class VideoEncoder(
    private val config: EncoderConfig,
    private val onEncodedFrame: (isKey: Boolean, ptsUs: Long, data: ByteArray) -> Unit,
    private val onConfigAvailable: (configData: ByteArray) -> Unit
) {
    companion object {
        private const val TAG = "VCamEncoder"
    }

    private var codec: MediaCodec? = null
    var inputSurface: Surface? = null
        private set

    private val isRunning = AtomicBoolean(false)
    private var drainThread: Thread? = null

    fun start(): Surface {
        val format = MediaFormat.createVideoFormat(config.mimeType, config.width, config.height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, config.bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, config.fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, config.iFrameInterval)
            setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR)

            // Ultra-low latency tuning
            setInteger(MediaFormat.KEY_LATENCY, 0)
            setInteger(MediaFormat.KEY_PRIORITY, 0) // Realtime priority
            try {
                setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline)
                setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel41)
            } catch (e: Exception) {
                Log.w(TAG, "Baseline profile configuration warning: ${e.message}")
            }
        }

        val encoder = MediaCodec.createEncoderByType(config.mimeType)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val surface = encoder.createInputSurface()
        encoder.start()

        this.codec = encoder
        this.inputSurface = surface
        this.isRunning.set(true)

        drainThread = Thread({
            drainEncoder()
        }, "VCamEncoderDrain").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }

        return surface
    }

    private fun drainEncoder() {
        val bufferInfo = MediaCodec.BufferInfo()
        val encoder = codec ?: return

        while (isRunning.get()) {
            try {
                val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000) // 10ms timeout

                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val newFormat = encoder.outputFormat
                    Log.d(TAG, "Encoder output format changed: $newFormat")

                    // Extract SPS / PPS configuration buffers
                    val csd0 = newFormat.getByteBuffer("csd-0") // SPS
                    val csd1 = newFormat.getByteBuffer("csd-1") // PPS

                    if (csd0 != null && csd1 != null) {
                        val spsBytes = ByteArray(csd0.remaining()).also { csd0.get(it) }
                        val ppsBytes = ByteArray(csd1.remaining()).also { csd1.get(it) }
                        val configPayload = spsBytes + ppsBytes
                        onConfigAvailable(configPayload)
                    }
                } else if (outputBufferIndex >= 0) {
                    val encodedBuffer = encoder.getOutputBuffer(outputBufferIndex)
                    if (encodedBuffer != null && bufferInfo.size > 0) {
                        encodedBuffer.position(bufferInfo.offset)
                        encodedBuffer.limit(bufferInfo.offset + bufferInfo.size)

                        val isConfig = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0
                        val isKey = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0

                        val outBytes = ByteArray(bufferInfo.size)
                        encodedBuffer.get(outBytes)

                        if (isConfig) {
                            onConfigAvailable(outBytes)
                        } else {
                            onEncodedFrame(isKey, bufferInfo.presentationTimeUs, outBytes)
                        }
                    }

                    encoder.releaseOutputBuffer(outputBufferIndex, false)
                }
            } catch (e: Exception) {
                if (isRunning.get()) {
                    Log.e(TAG, "Error during encoder drain: ${e.message}", e)
                }
                break
            }
        }
    }

    fun requestKeyFrame() {
        val params = Bundle().apply {
            putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0)
        }
        codec?.setParameters(params)
    }

    fun setBitrate(newBitrateBps: Int) {
        val params = Bundle().apply {
            putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, newBitrateBps)
        }
        codec?.setParameters(params)
    }

    fun stop() {
        isRunning.set(false)
        drainThread?.interrupt()
        try {
            drainThread?.join(500)
        } catch (_: Exception) {}

        try {
            codec?.stop()
            codec?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing encoder: ${e.message}")
        } finally {
            codec = null
            inputSurface?.release()
            inputSurface = null
        }
    }
}
