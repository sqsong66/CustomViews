package com.sqsong.opengllib.record

import android.media.MediaMuxer
import android.view.Surface

class VideoMuxer(
    videoPath: String,
    private val videoWidth: Int,
    private val videoHeight: Int,
) {

    private var startTime = 0L
    private var videoTrackIndex = -1
    private var videoPresentationTimeUs = 0L
    private var mediaMuxer: MediaMuxer = MediaMuxer(videoPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

    private val videoRecorder by lazy {
        VideoRecorder(videoWidth, videoHeight, onVideoFormatChanged = {
            videoTrackIndex = mediaMuxer.addTrack(it)
            mediaMuxer.start()
        }, onVideoBufferDataArrived = { byteBuffer, bufferInfo ->
            if (videoTrackIndex == -1) return@VideoRecorder
            if (startTime == 0L) {
                videoPresentationTimeUs = 0L
                startTime = System.nanoTime()
            } else {
                videoPresentationTimeUs = (System.nanoTime() - startTime) / 1000
            }
            bufferInfo.presentationTimeUs = videoPresentationTimeUs
            mediaMuxer.writeSampleData(videoTrackIndex, byteBuffer, bufferInfo)
        })
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
    }

    fun release() {
        inputSurface = null
        videoTrackIndex = -1
        startTime = 0L
        videoPresentationTimeUs = 0L
        mediaMuxer.stop()
        mediaMuxer.release()
        videoRecorder.release()
    }
}
