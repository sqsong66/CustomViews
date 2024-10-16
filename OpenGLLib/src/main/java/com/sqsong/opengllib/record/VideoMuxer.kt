package com.sqsong.opengllib.record

import android.content.Context
import android.media.MediaCodec
import android.media.MediaMuxer
import android.util.Log
import android.view.Surface

class VideoMuxer(
    context: Context,
    audioAssetPath: String,
    videoPath: String,
    private val videoWidth: Int,
    private val videoHeight: Int,
) {

    private var startTime = 0L
    private var videoTrackIndex = -1
    private var audioTrackIndex = -1
    private var isStartRecord = false
    private var videoPresentationTimeUs = 0L
    private var mediaMuxer: MediaMuxer = MediaMuxer(videoPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

    private val videoRecorder by lazy {
        VideoRecorder(context, audioAssetPath, videoWidth, videoHeight, onVideoFormatChanged = {
            videoTrackIndex = mediaMuxer.addTrack(it)
            startRecord()
        }, onVideoBufferDataArrived = { byteBuffer, bufferInfo ->
            if (!isStartRecord) return@VideoRecorder
            if (startTime == 0L) {
                videoPresentationTimeUs = 0L
                startTime = System.nanoTime()
            } else {
                videoPresentationTimeUs = (System.nanoTime() - startTime) / 1000
            }
            bufferInfo.presentationTimeUs = videoPresentationTimeUs
            mediaMuxer.writeSampleData(videoTrackIndex, byteBuffer, bufferInfo)
        }, onAudioTrackIndexChanged = { mediaFormat ->
            audioTrackIndex = mediaMuxer.addTrack(mediaFormat)
            startRecord()
        }, onAudioBufferDataArrived = { byteBuffer, bufferInfo, bytesPerSample, audioSampleRate ->
            if (!isStartRecord) return@VideoRecorder
             mediaMuxer.writeSampleData(audioTrackIndex, byteBuffer, bufferInfo)
        })
    }

    private fun startRecord() {
        if (videoTrackIndex == -1 || audioTrackIndex == -1) return
        mediaMuxer.start()
        isStartRecord = true
        Log.d("songmao", "startRecord: videoTrackIndex = $videoTrackIndex, audioTrackIndex = $audioTrackIndex")
    }

    var inputSurface: Surface? = null
        private set
        get() {
            if (field == null) {
                field = videoRecorder.videoInputSurface
            }
            return field
        }


    fun drainVideoEncoder(endOfStream: Boolean) {
        videoRecorder.drainVideoEncoder(endOfStream)
        videoRecorder.drainAudioEncoder()
    }

    fun release() {
        inputSurface = null
        videoTrackIndex = -1
        startTime = 0L
        videoPresentationTimeUs = 0L
        videoRecorder.release()
        mediaMuxer.stop()
        mediaMuxer.release()
        isStartRecord = false
    }
}
