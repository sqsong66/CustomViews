package com.sqsong.opengllib.record

import android.graphics.Bitmap
import android.opengl.GLES30
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.sqsong.opengllib.common.BitmapTexture
import com.sqsong.opengllib.common.Texture
import com.sqsong.opengllib.data.RecordConfig
import com.sqsong.opengllib.egl.EGLHelper
import com.sqsong.opengllib.egl.EGLHelper.Companion.FLAG_RECORDABLE
import com.sqsong.opengllib.egl.EGLHelper.Companion.FLAG_TRY_GLES3
import com.sqsong.opengllib.filters.MultiTextureFilter
import java.lang.ref.WeakReference

class RenderThreadHelper(
    private val recordConfig: RecordConfig,
    private val renderSurface: Any?,
    private val refreshPeriodNs: Long,
) {

    private val lock = Object()
    private var isReady = false
    private var eglHelper: EGLHelper? = null
    private val textures = mutableListOf<Texture>()
    private var renderHandler: RenderHandler? = null
    private val textureBitmaps = mutableListOf<Bitmap>()
    private var handlerThread: EncoderHandlerThread? = null
    private val multiTextureFilter by lazy { MultiTextureFilter(recordConfig.context) }

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
        renderHandler?.obtainMessage(MSG_TEXTURE_AVAILABLE, bitmaps)?.let { renderHandler?.sendMessage(it) }
    }

    fun sendSurfaceCreated() {
        Log.w("sqsong", "sendSurfaceCreated")
        renderHandler?.obtainMessage(MSG_SURFACE_CREATED)?.let { renderHandler?.sendMessage(it) }
    }

    fun sendSurfaceChanged(width: Int, height: Int) {
        Log.w("sqsong", "sendSurfaceChanged, width: $width, height: $height")
        renderHandler?.obtainMessage(MSG_SURFACE_CHANGED, width, height)?.let { renderHandler?.sendMessage(it) }
    }

    fun sendDoFrame(frameTimeNanos: Long) {
        renderHandler?.obtainMessage(MSG_DO_FRAME, (frameTimeNanos shr 32).toInt(), frameTimeNanos.toInt())?.let { renderHandler?.sendMessage(it) }
    }

    fun sendRecordingEnabled(enabled: Boolean) {
        renderHandler?.obtainMessage(MSG_RECORDING_ENABLED, enabled)?.let { renderHandler?.sendMessage(it) }
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
                    val timeStampNanos = msg.arg1.toLong() shl 32 or (msg.arg2.toLong() and 0xffffffffL)
                    threadHelper.handleDoFrame(timeStampNanos)
                }

                MSG_RECORDING_ENABLED -> {
                    threadHelper.handleRecordingEnabled(msg.obj as Boolean)
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
        Log.d("songmao", "handleTextureAvailable...")
        textureBitmaps.clear()
        textureBitmaps.addAll(bitmaps)
        clearTextures()
        bitmaps.forEach { bitmap ->
            textures.add(BitmapTexture(bitmap))
        }
        multiTextureFilter.onInputTextureLoaded(textures[0].textureWidth, textures[0].textureHeight)
    }

    private fun handleSurfaceCreated() {
        Log.d("sqsong", "handleSurfaceCreated, thread: ${Thread.currentThread().name}")
        eglHelper = EGLHelper(surface = renderSurface, flags = FLAG_RECORDABLE or FLAG_TRY_GLES3, sharedEGLContext = null)
        multiTextureFilter.ifNeedInit()
    }

    private fun handleSurfaceChanged(width: Int, height: Int) {
        Log.d("sqsong", "handleSurfaceChanged, width: $width, height: $height")
        multiTextureFilter.onViewSizeChanged(width, height)
    }

    private fun handleDoFrame(timeStampNanos: Long) {
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
        val swapResult = eglHelper?.swapBuffers()
        Log.d(TAG, "swap result is $swapResult")
    }

    private fun handleRecordingEnabled(enabled: Boolean) {

    }

    private fun handleShutdown() {
        handlerThread?.quitThreadSafely()
        handlerThread = null
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