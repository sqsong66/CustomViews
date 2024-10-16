package com.sqsong.opengllib.record

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import android.view.Surface
import java.nio.ByteBuffer

class VideoRecorder(
    private val context: Context,
    private val audioAssetPath: String,
    private val videoWidth: Int,
    private val videoHeight: Int,
    private val onVideoFormatChanged: (MediaFormat) -> Unit,
    private val onVideoBufferDataArrived: (ByteBuffer, MediaCodec.BufferInfo) -> Unit,
    private val onAudioTrackIndexChanged: (MediaFormat) -> Unit,
    private val onAudioBufferDataArrived: (ByteBuffer, MediaCodec.BufferInfo, Int, Int) -> Unit,
) {

    private var audioTrackIndex = -1
    private var totalFrames: Long = 0
    private var lastTimestampUs: Long = 0
    private var videoCodec: MediaCodec? = null
    private val videoBufferInfo by lazy { MediaCodec.BufferInfo() }
    private val audioBufferInfo by lazy { MediaCodec.BufferInfo() }
    private var audioMediaExtractor: MediaExtractor? = null

    private var isAudioEOS = false
    private var audioSampleRate = 44100 // 音频采样率
    private var audioChannelCount = 1 // 音频通道数
    private var bytesPerSample = 2 * audioChannelCount // 每个音频样本占用字节数
    private var audioPresentationTimeUs = 0L
    private val audioBuffer = ByteBuffer.allocate(1024 * 1024)

    var videoInputSurface: Surface? = null
        private set

    init {
        val mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, videoWidth, videoHeight).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, 8000000)
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

        audioMediaExtractor = MediaExtractor().apply {
            context.assets.openFd(audioAssetPath).let { assetFileDescriptor ->
                setDataSource(assetFileDescriptor.fileDescriptor, assetFileDescriptor.startOffset, assetFileDescriptor.length)
            }
            for (i in 0 until trackCount) {
                val format = getTrackFormat(i)
                if (format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    selectTrack(i)
                    audioTrackIndex = i
                    break
                }
            }

            val audioMediaFormat = getTrackFormat(audioTrackIndex).let { audioFormat ->
                audioSampleRate = audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                audioChannelCount = audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                bytesPerSample = 2 * audioChannelCount
                audioFormat
            }
            onAudioTrackIndexChanged(audioMediaFormat)
        }
        Log.w("songmao", "VideoRecorder init done, videoInputSurface: $videoInputSurface, audioTrackIndex: $audioTrackIndex, audioSampleRate: $audioSampleRate, audioChannelCount: $audioChannelCount, bytesPerSample: $bytesPerSample")
    }

    fun drainAudioEncoder() {
        val audioExtractor = audioMediaExtractor ?: return
        if (isAudioEOS) {
            audioExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            isAudioEOS = false
        }

        audioBuffer.clear()
        val audioFrameSize = (audioSampleRate / 30) * bytesPerSample
        var sampleSize: Int

        while (audioBuffer.remaining() >= audioFrameSize && !isAudioEOS) {
            sampleSize = audioExtractor.readSampleData(audioBuffer, audioBuffer.position())
            Log.d("sqsong", "Audio sampleSize: $sampleSize")
            if (sampleSize > 0) {
                audioBuffer.position(audioBuffer.position() + sampleSize)
                audioExtractor.advance()
            } else {
                isAudioEOS = true
                audioExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            }
        }

        Log.w("sqsong", "audioBuffer.position(): ${audioBuffer.position()}, audioBuffer.limit(): ${audioBuffer.limit()}")
        if (audioBuffer.position() > 0) {
            audioBuffer.flip()
            audioBufferInfo.offset = 0
            audioBufferInfo.size = audioBuffer.limit()
            audioBufferInfo.presentationTimeUs = audioPresentationTimeUs
            audioBufferInfo.flags = convertSampleFlags(audioExtractor.sampleFlags)
            onAudioBufferDataArrived(audioBuffer, audioBufferInfo, bytesPerSample, audioSampleRate)
            audioPresentationTimeUs += (audioBufferInfo.size / bytesPerSample) * 1_000_000L / audioSampleRate
        }
    }

    private fun convertSampleFlags(sampleFlags: Int): Int {
        var codecFlags = 0
        if (sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
            codecFlags = codecFlags or MediaCodec.BUFFER_FLAG_SYNC_FRAME
        }
        if (sampleFlags and MediaExtractor.SAMPLE_FLAG_ENCRYPTED != 0) {
            codecFlags = codecFlags or MediaCodec.BUFFER_FLAG_CODEC_CONFIG
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (sampleFlags and MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME != 0) {
                codecFlags = codecFlags or MediaCodec.BUFFER_FLAG_PARTIAL_FRAME
            }
        }
        return codecFlags
    }


    fun drainVideoEncoder(endOfStream: Boolean) {
        if (endOfStream) videoCodec?.signalEndOfInputStream()

        while (true) {
            val outputBufferIndex = videoCodec?.dequeueOutputBuffer(videoBufferInfo, 10000L) ?: -1
            Log.w("sqsong", "Video outputBufferIndex: $outputBufferIndex")
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
                        if (videoBufferInfo.size != 0) {
                            encodeData.position(videoBufferInfo.offset)
                            encodeData.limit(videoBufferInfo.offset + videoBufferInfo.size)
                            onVideoBufferDataArrived(encodeData, videoBufferInfo)

                            // Calculate frame rate
                            val timestampUs = videoBufferInfo.presentationTimeUs
                            val frameIntervalUs = timestampUs - lastTimestampUs
                            lastTimestampUs = timestampUs
                            totalFrames++
                            val frameRate = totalFrames * 1_000_000.0 / timestampUs
                            Log.d("VideoRecorder", "Frame interval: $frameIntervalUs us, Frame rate: $frameRate fps")
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
        audioPresentationTimeUs = 0L
        audioMediaExtractor?.release()
        audioMediaExtractor = null
    }

}