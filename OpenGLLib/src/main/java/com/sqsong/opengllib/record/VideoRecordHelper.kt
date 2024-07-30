package com.sqsong.opengllib.record

import android.opengl.GLES30
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.sqsong.opengllib.data.RecordConfig
import com.sqsong.opengllib.egl.EGLHelper
import com.sqsong.opengllib.egl.RecordDrawer
import java.lang.ref.WeakReference

class VideoRecordHelper {

    private val lock = Object()
    private var isRecording = false
    private var recordTextureId: Int = -1
    private var eglHelper: EGLHelper? = null
    private var videoMuxer: VideoMuxer? = null
    private var recordConfig: RecordConfig? = null
    private var recordDrawer: RecordDrawer? = null
    private var encoderHandler: EncoderHandler? = null
    private var encoderHandlerThread: EncoderHandlerThread? = null

    fun isInRecord(): Boolean {
        synchronized(lock) {
            return isRecording
        }
    }

    fun startRecord(recordConfig: RecordConfig) {
        synchronized(lock) {
            if (isRecording) return
            isRecording = true
            encoderHandlerThread = EncoderHandlerThread("EncoderHandlerThread", lock).apply { start() }
            while (encoderHandlerThread?.isReady == false) {
                try {
                    lock.wait()
                } catch (ie: InterruptedException) {
                    ie.printStackTrace()
                }
            }
            encoderHandler = EncoderHandler(encoderHandlerThread!!.looper, this)
        }
        encoderHandler?.obtainMessage(MSG_START_RECORD, recordConfig)?.let { encoderHandler?.sendMessage(it) }
    }

    private fun handleStartRecord(recordConfig: RecordConfig) {
        this.recordConfig = recordConfig
        videoMuxer = VideoMuxer(recordConfig.videoPath, recordConfig.videoWidth, recordConfig.videoHeight)
        videoMuxer?.inputSurface?.let { surface ->
            Log.d("sqsong", "handleStartRecord, surface: $surface")
            eglHelper = EGLHelper(surface, sharedEGLContext = recordConfig.sharedEGLContext)
        }
        recordDrawer = RecordDrawer(recordConfig.context).apply { ifNeedInit() }
        Log.w("songmao", "handleStartRecord: inputSurface: ${videoMuxer?.inputSurface}, isRecording: ${isInRecord()}")
    }

    fun stopRecord() {
        Log.d("songmao", "stopRecord...")
        encoderHandler?.obtainMessage(MSG_STOP_RECORD)?.let { encoderHandler?.sendMessage(it) }
    }

    private fun handleStopRecord() {
        Log.w("songmao", "handleStopRecord...")
        videoMuxer?.drainVideoEncoder(true)
        videoMuxer?.release()
        videoMuxer = null
        encoderHandlerThread?.quitThreadSafely()
        encoderHandlerThread = null
        encoderHandler?.removeCallbacksAndMessages(null)
        encoderHandler = null
        recordDrawer?.onDestroy()
        recordDrawer = null
        eglHelper?.release()
        eglHelper = null
        isRecording = false
    }

    fun setTextureId(textureId: Int) {
        encoderHandler?.obtainMessage(MSG_SET_TEXTURE_ID, textureId)?.let { encoderHandler?.sendMessage(it) }
    }

    private fun handleTextureId(textureId: Int) {
        Log.w("sqsong", "handleTextureId: $textureId")
        recordTextureId = textureId
    }

    fun onFrameAvailable(timestamp: Long) {
        synchronized(lock) {
            if (encoderHandlerThread?.isReady == false) {
                return
            }
        }
        encoderHandler?.removeMessages(MSG_FRAME_AVAILABLE)
        encoderHandler?.obtainMessage(MSG_FRAME_AVAILABLE, timestamp)?.let { encoderHandler?.sendMessage(it) }
    }

    private fun handleFrameAvailable(timestamp: Long) {
        Log.e("songmao", "handleFrameAvailable, recordTextureId: $recordTextureId, recordConfig: $recordConfig, Thread: ${Thread.currentThread().name}")
        videoMuxer?.drainVideoEncoder(false)

        val makeCurrent = eglHelper?.makeCurrent()
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT or GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        val width = recordConfig?.videoWidth ?: 0
        val height = recordConfig?.videoHeight ?: 0
        Log.w("sqsong", "handleFrameAvailable, makeCurrent: $makeCurrent, width: $width, height: $height")
        GLES30.glViewport(0, 0, width, height)
        recordDrawer?.onDrawFrame(recordTextureId)

        eglHelper?.setPresentationTime(timestamp)
        eglHelper?.swapBuffers()
    }

    inner class EncoderHandler(looper: Looper, helper: VideoRecordHelper) : Handler(looper) {

        private val recordHelper = WeakReference(helper)

        override fun handleMessage(msg: Message) {
            val helper = recordHelper.get() ?: return
            when (msg.what) {
                MSG_START_RECORD -> {
                    helper.handleStartRecord(msg.obj as RecordConfig)
                }

                MSG_STOP_RECORD -> {
                    helper.handleStopRecord()
                }

                MSG_FRAME_AVAILABLE -> {
                    helper.handleFrameAvailable(msg.obj as Long)
                }

                MSG_SET_TEXTURE_ID -> {
                    helper.handleTextureId(msg.obj as Int)
                }
            }
        }

    }

    companion object {
        private const val MSG_START_RECORD = 0x01
        private const val MSG_STOP_RECORD = 0x02
        private const val MSG_FRAME_AVAILABLE = 0x03
        private const val MSG_SET_TEXTURE_ID = 0x04
    }

}