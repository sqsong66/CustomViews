package com.sqsong.opengllib.record

import android.graphics.Bitmap
import android.opengl.EGL14
import android.opengl.GLES30
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.Surface
import com.sqsong.opengllib.common.BitmapTexture
import com.sqsong.opengllib.common.Texture
import com.sqsong.opengllib.data.RecordConfig
import com.sqsong.opengllib.egl.EGLHelper.Companion.FLAG_RECORDABLE
import com.sqsong.opengllib.egl.EGLHelper.Companion.FLAG_TRY_GLES3
import com.sqsong.opengllib.egl.RecordDrawer
import com.sqsong.opengllib.filters.MultiTextureFilter
import com.sqsong.opengllib.gles.EglCore
import com.sqsong.opengllib.gles.WindowSurface
import java.lang.ref.WeakReference

class RenderThreadHelper(
    private val recordConfig: RecordConfig,
    private val renderSurface: Surface,
    private val refreshPeriodNs: Long,
    private val onVideoRecordDone: () -> Unit
) {

    private val lock = Object()
    private var isReady = false
    private var isRecordEnabled = false
    private var eglCore: EglCore? = null
    private val textures = mutableListOf<Texture>()
    private var windowSurface: WindowSurface? = null
    private var renderHandler: RenderHandler? = null
    private var textureBitmaps: List<Bitmap>? = null
    private var inputWindowSurface: WindowSurface? = null
    private var handlerThread: EncoderHandlerThread? = null
    private val multiTextureFilter by lazy { MultiTextureFilter(recordConfig.context) }

    private var videoMuxer: VideoMuxer? = null
    private var recordDrawer: RecordDrawer? = null
    private var videoEncoder: TextureVideoEncoder? = null

    fun prepare() {
        synchronized(lock) {
            handlerThread = EncoderHandlerThread("RenderThread", lock).apply { start() }
            while (handlerThread?.isReady != true) {
                try {
                    lock.wait()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
            isReady = true
            renderHandler = RenderHandler(handlerThread!!.looper, this)
        }
    }

    fun setImageBitmaps(bitmaps: List<Bitmap>) {
        Log.d("songmao", "setImageBitmaps...")
        if (bitmaps.isEmpty()) return
        textureBitmaps = bitmaps
        renderHandler?.obtainMessage(MSG_TEXTURE_AVAILABLE, bitmaps)?.let { renderHandler?.sendMessage(it) }
    }

    fun sendSurfaceCreated() {
        Log.w("sqsong", "sendSurfaceCreated, textureBitmaps size: ${textureBitmaps?.size}")
        renderHandler?.obtainMessage(MSG_SURFACE_CREATED)?.let { renderHandler?.sendMessage(it) }
        if (!textureBitmaps.isNullOrEmpty()) {
            renderHandler?.obtainMessage(MSG_TEXTURE_AVAILABLE, textureBitmaps)?.let { renderHandler?.sendMessage(it) }
        }
    }

    fun sendSurfaceChanged(width: Int, height: Int) {
        Log.w("sqsong", "sendSurfaceChanged, width: $width, height: $height")
        renderHandler?.obtainMessage(MSG_SURFACE_CHANGED, width, height)?.let { renderHandler?.sendMessage(it) }
    }

    fun sendDoFrame(frameTimeNanos: Long) {
        renderHandler?.obtainMessage(MSG_DO_FRAME, frameTimeNanos/*(frameTimeNanos shr 32).toInt(), frameTimeNanos.toInt()*/)?.let { renderHandler?.sendMessage(it) }
    }

    fun sendRecordingEnabled(enabled: Boolean, videoPath: String?) {
        renderHandler?.obtainMessage(MSG_RECORDING_ENABLED, if (enabled) 1 else 0, 0, videoPath)?.let { renderHandler?.sendMessage(it) }
    }

    fun sendShutdown() {
        renderHandler?.obtainMessage(MSG_SHUTDOWN)?.let { renderHandler?.sendMessage(it) }
    }

    private fun clearTextures() {
        textures.forEach { texture -> texture.delete() }
        textures.clear()
    }

    inner class RenderHandler(
        looper: Looper,
        renderThread: RenderThreadHelper
    ) : Handler(looper) {

        private val weakThreadHelper = WeakReference(renderThread)
        override fun handleMessage(msg: Message) {
            val threadHelper = weakThreadHelper.get() ?: return
            when (msg.what) {
                MSG_SURFACE_CREATED -> {
                    threadHelper.handleSurfaceCreated()
                }

                MSG_SURFACE_CHANGED -> {
                    threadHelper.handleSurfaceChanged(msg.arg1, msg.arg2)
                }

                MSG_DO_FRAME -> {
                    // val timeStampNanos = msg.arg1.toLong() shl 32 or (msg.arg2.toLong() and 0xffffffffL)
                    threadHelper.handleDoFrame(msg.obj as Long)
                }

                MSG_RECORDING_ENABLED -> {
                    val enabled = msg.arg1 == 1
                    val videoPath = msg.obj as? String
                    threadHelper.handleRecordingEnabled(enabled, videoPath)
                }

                MSG_SHUTDOWN -> {
                    threadHelper.handleShutdown()
                }

                MSG_TEXTURE_AVAILABLE -> {
                    val bitmaps = msg.obj as List<*>
                    threadHelper.handleTextureAvailable(bitmaps as List<Bitmap>)
                }
            }
        }
    }

    private fun handleTextureAvailable(bitmaps: List<Bitmap>) {
        if (bitmaps.isEmpty()) return
        clearTextures()
        bitmaps.forEach { bitmap ->
            textures.add(BitmapTexture(bitmap))
        }
        if (textures.isEmpty()) return
        multiTextureFilter.onInputTextureLoaded(textures[0].textureWidth, textures[0].textureHeight)
        Log.d("sqsong", "handleTextureAvailable... bitmaps size: ${bitmaps.size}, textures size: ${textures.size}, thread: ${Thread.currentThread().name}")
    }

    private fun handleSurfaceCreated() {
        Log.d("sqsong", "handleSurfaceCreated, thread: ${Thread.currentThread().name}")
        eglCore = EglCore(null, FLAG_RECORDABLE or FLAG_TRY_GLES3)
        windowSurface = WindowSurface(eglCore, renderSurface, false)
        windowSurface?.makeCurrent()
        multiTextureFilter.ifNeedInit()
    }

    private fun handleSurfaceChanged(width: Int, height: Int) {
        Log.d("sqsong", "handleSurfaceChanged, width: $width, height: $height")
        multiTextureFilter.onViewSizeChanged(width, height)
    }

    private fun handleDoFrame(timeStampNanos: Long) {
        if (!isReady) return

        val diff = System.nanoTime() - timeStampNanos
        val max: Long = refreshPeriodNs - 2000000
        if (diff > max) {
            // too much, drop a frame
            Log.d(TAG, "diff is ${diff / 1000000.0} ms, max ${max / 1000000.0}, skipping render, textures size: ${textures.size}")
            return
        }

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        if (textures.isNotEmpty()) {
            multiTextureFilter.onDrawFrame(textures)
        }
        val swapResult = windowSurface?.swapBuffers()
        Log.d(TAG, "swap result is $swapResult")

        if (isRecordEnabled) {
            val offlineTextureId = multiTextureFilter.getFrameBufferTexture()?.textureId
            Log.d("sqsong", "offlineTextureId: $offlineTextureId, eglContext: ${EGL14.eglGetCurrentContext()}")
            videoEncoder?.frameAvailableSoon()
            inputWindowSurface?.makeCurrent()
            Log.w("sqsong", "videoEGLHelper?.makeCurrent(), eglContext: ${EGL14.eglGetCurrentContext()}")
            GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT or GLES30.GL_COLOR_BUFFER_BIT)
            GLES30.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
            GLES30.glViewport(0, 0, recordConfig.videoWidth, recordConfig.videoHeight)
            recordDrawer?.onDrawFrame(offlineTextureId ?: 0)
            inputWindowSurface?.setPresentationTime(timeStampNanos)
            inputWindowSurface?.swapBuffers()

            // 视频录制帧处理完成后，将EGL环境切换回到主渲染线程
            windowSurface?.makeCurrent()
        }
    }

    private fun handleRecordingEnabled(enabled: Boolean, videoPath: String?) {
        if (enabled == isRecordEnabled) return
        if (enabled) {
            startEncoder(videoPath)
        } else {
            stopEncoder()
        }
        isRecordEnabled = enabled
    }

    private fun startEncoder(videoPath: String?) {
        videoMuxer = VideoMuxer(recordConfig.context, recordConfig.audioAssetPath, videoPath ?: "", recordConfig.videoWidth, recordConfig.videoHeight)
        videoMuxer?.inputSurface?.let { surface ->
            inputWindowSurface = WindowSurface(eglCore, surface, true)
            Log.d("sqsong", "handleStartRecord, surface: $surface， eglContext: ${EGL14.eglGetCurrentContext()}, videoEGLHelper: $inputWindowSurface")
        }
        videoEncoder = TextureVideoEncoder(videoMuxer, onStopRecord = {
            onVideoRecordDone()
        })
        recordDrawer = RecordDrawer(recordConfig.context).apply { ifNeedInit() }
    }

    private fun stopEncoder() {
        videoEncoder?.stopRecord()
        videoEncoder = null
        videoMuxer = null
        recordDrawer?.onDestroy()
        recordDrawer = null
        inputWindowSurface?.release()
        inputWindowSurface = null
    }

    private fun handleShutdown() {
        stopEncoder()
        handlerThread?.quitThreadSafely()
        handlerThread = null
        eglCore?.release()
        eglCore = null
        windowSurface?.release()
        windowSurface = null
        multiTextureFilter.onDestroy()
    }

    companion object {
        const val MSG_SURFACE_CREATED = 0
        const val MSG_SURFACE_CHANGED = 1
        const val MSG_DO_FRAME = 2
        const val MSG_RECORDING_ENABLED = 3
        const val MSG_SHUTDOWN = 4
        const val MSG_TEXTURE_AVAILABLE = 5

        private const val TAG = "RenderThreadHelper"
    }

}