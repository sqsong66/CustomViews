package com.sqsong.opengllib.record

import android.os.Handler
import android.os.Looper
import android.os.Message
import java.lang.ref.WeakReference

class TextureVideoEncoder(
    muxer: VideoMuxer?,
    private val onStopRecord: () -> Unit
) {

    private val lock = Object()
    private var isReady = false
    private var isRunning = false
    private val weakVideoMuxer = WeakReference(muxer)
    private var encoderHandler: EncoderHandler? = null
    private var handlerThread: EncoderHandlerThread? = null

    init {
        prepare()
    }

    private fun prepare() {
        synchronized(lock) {
            if (isRunning) return
            isRunning = true
            handlerThread = EncoderHandlerThread("RenderThread", lock).apply { start() }
            while (handlerThread?.isReady != true) {
                try {
                    lock.wait()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
            isReady = true
            encoderHandler = EncoderHandler(handlerThread!!.looper, this)
        }
    }

    fun stopRecord() {
        encoderHandler?.sendEmptyMessage(MSG_STOP_RECORDING)
    }

    fun frameAvailableSoon() {
        synchronized(lock) {
            if (!isReady) return
        }
        encoderHandler?.sendEmptyMessage(MSG_FRAME_AVAILABLE)
    }

    inner class EncoderHandler(looper: Looper, encoder: TextureVideoEncoder) : Handler(looper) {
        private val weakEncoder = WeakReference(encoder)

        override fun handleMessage(msg: Message) {
            val encoder = weakEncoder.get() ?: return
            when (msg.what) {
                MSG_STOP_RECORDING -> {
                    encoder.handleStopRecording()
                }

                MSG_FRAME_AVAILABLE -> {
                    encoder.handleFrameAvailable()
                }

                else -> {}
            }
        }
    }

    private fun handleFrameAvailable() {
        weakVideoMuxer.get()?.drainVideoEncoder(false)
    }

    private fun handleStopRecording() {
        weakVideoMuxer.get()?.apply {
            drainVideoEncoder(true)
            release()
        }
        handlerThread?.quitThreadSafely()
        handlerThread = null
        onStopRecord()
    }

    companion object {
        const val MSG_STOP_RECORDING = 0
        const val MSG_FRAME_AVAILABLE = 1
    }

}