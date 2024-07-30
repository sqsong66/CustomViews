package com.sqsong.opengllib.record

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import java.nio.ByteBuffer

class VideoRecorder(
    private val videoWidth: Int,
    private val videoHeight: Int,
    private val onVideoFormatChanged: (MediaFormat) -> Unit,
    private val onVideoBufferDataArrived: (ByteBuffer, MediaCodec.BufferInfo) -> Unit
) {

    private var videoCodec: MediaCodec? = null
    private val videoBufferInfo by lazy { MediaCodec.BufferInfo() }
    var videoInputSurface: Surface? = null
        private set

    init {
        val mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, videoWidth, videoHeight).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, 1000000)
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5)
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_MAX_WIDTH, videoWidth)
            setInteger(MediaFormat.KEY_MAX_HEIGHT, videoHeight)
        }

        videoCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply {
            configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            videoInputSurface = createInputSurface()
            start()
        }
        Log.w("songmao", "VideoRecorder init done, videoInputSurface: $videoInputSurface")
    }

    fun drainVideoEncoder(endOfStream: Boolean) {
        if (endOfStream) videoCodec?.signalEndOfInputStream()

        while (true) {
            val outputBufferIndex = videoCodec?.dequeueOutputBuffer(videoBufferInfo, 10000L) ?: -1
            when {
                outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream) {
                        Log.d("sqsong", "Video no output available, out of while(true), thread: ${Thread.currentThread().name}, videoCodec: $videoCodec")
                         break
                    } else {
                        Log.d("sqsong", "Video no output available, spinning to await EOS")
                    }
                }

                outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val outputFormat = videoCodec?.outputFormat
                    Log.d("sqsong", "Video format changed: ${videoCodec?.outputFormat}")
                    outputFormat?.let { onVideoFormatChanged(it) }
                }

                outputBufferIndex < 0 -> {
                    Log.e("sqsong", "Unexpected result from video encoder.dequeueOutputBuffer: $outputBufferIndex")
                }

                else -> {
                    videoCodec?.getOutputBuffer(outputBufferIndex)?.let { encodeData ->
                        if ((videoBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            Log.d("sqsong", "ignoring Video BUFFER_FLAG_CODEC_CONFIG")
                            videoBufferInfo.size = 0
                        }
                        if (videoBufferInfo.size == 0) {
                            encodeData.position(videoBufferInfo.offset)
                            encodeData.limit(videoBufferInfo.offset + videoBufferInfo.size)
                            onVideoBufferDataArrived(encodeData, videoBufferInfo)
                        }
                    }

                    videoCodec?.releaseOutputBuffer(outputBufferIndex, false)

                    if ((videoBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.d("sqsong", "Video end of stream reached")
                        break
                    }
                }
            }
        }
    }

    fun release() {
        videoCodec?.stop()
        videoCodec?.release()
        videoCodec = null
    }

}