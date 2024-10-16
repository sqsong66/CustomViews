package com.sqsong.opengllib

import android.content.Context
import android.graphics.Bitmap
import android.opengl.EGL14
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.util.Log
import com.sqsong.opengllib.common.BitmapTexture
import com.sqsong.opengllib.common.Texture
import com.sqsong.opengllib.data.RecordConfig
import com.sqsong.opengllib.filters.MultiTextureFilter
import com.sqsong.opengllib.record.VideoRecordHelper
import java.io.File
import java.util.LinkedList
import java.util.Queue
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MultiTextureRender(
    private val context: Context
) : GLSurfaceView.Renderer {

    private var isRecording = false
    private var isStartRecord = false
    private var startNanoTime: Long = 0
    private var imageBitmaps: List<Bitmap>? = null
    private val runOnDraw = LinkedList<Runnable>()
    private val textures = mutableListOf<Texture>()
    private val videoRecordHelper by lazy { VideoRecordHelper() }
    private val multiTextureFilter by lazy { MultiTextureFilter(context) }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0f, 0f, 0f, 0f)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        multiTextureFilter.ifNeedInit()

        imageBitmaps?.let { bitmaps ->
            clearTextures()
            bitmaps.forEach { bitmap ->
                textures.add(BitmapTexture(bitmap))
            }
            multiTextureFilter.onInputTextureLoaded(textures[0].textureWidth, textures[0].textureHeight)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        multiTextureFilter.onViewSizeChanged(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        runAll(runOnDraw)
        if (textures.isNotEmpty()) {
            multiTextureFilter.onDrawFrame(textures)
        }

        Log.e("sqsong", "onDrawFrame: isStartRecord: $isStartRecord, isRecording: $isRecording, thread: ${Thread.currentThread().name}")
        if (isStartRecord) {
            if (!isRecording) {
                val config = RecordConfig(context, getVideoPath(context), 720, 1280, "audio/music_desert_island.mp3", EGL14.eglGetCurrentContext())
                Log.d("sqsong", "Start record: $config")
                videoRecordHelper.startRecord(config)
                multiTextureFilter.getFrameBufferTexture()?.textureId?.let { textureId ->
                    videoRecordHelper.setTextureId(textureId)
                }
                isRecording = videoRecordHelper.isInRecord()
                startNanoTime = System.nanoTime()
            }
            val timestamp = System.nanoTime() - startNanoTime
            Log.w("songmao", "isStartRecord: $isStartRecord, timestamp: $timestamp, isRecording: $isRecording, Thread: ${Thread.currentThread().name}")
            videoRecordHelper.onFrameAvailable(timestamp)
        } else {
            if (isRecording) {
                videoRecordHelper.stopRecord()
                isRecording = false
            }
        }
    }

    private fun getVideoPath(context: Context): String {
        val externalDir: String = context.getExternalFilesDir(null)?.absolutePath ?: context.filesDir.absolutePath
        val dir = externalDir.plus(File.separator).plus("Video")
        val name = "video_${System.currentTimeMillis()}.mp4"
        val file = File(dir, name)
        if (file.parentFile?.exists() == false) file.parentFile?.mkdirs()
        return file.absolutePath
    }

    fun setImageBitmaps(bitmaps: List<Bitmap>) {
        if (bitmaps.isEmpty()) return
        imageBitmaps = bitmaps
        runOnDraw {
            clearTextures()
            bitmaps.forEach { bitmap ->
                textures.add(BitmapTexture(bitmap))
            }
            multiTextureFilter.onInputTextureLoaded(textures[0].textureWidth, textures[0].textureHeight)
        }
    }

    private fun clearTextures() {
        textures.forEach { texture -> texture.delete() }
        textures.clear()
    }

    private fun runOnDraw(runnable: Runnable) {
        synchronized(runOnDraw) {
            runOnDraw.add(runnable)
        }
    }

    private fun runAll(queue: Queue<Runnable>) {
        synchronized(queue) {
            while (queue.isNotEmpty()) {
                queue.poll()?.run()
            }
        }
    }

    fun startRecord() {
        isStartRecord = true
    }

    fun stopRecord() {
        isStartRecord = false
    }

    fun onDestroy() {
        clearTextures()
        multiTextureFilter.onDestroy()

        if (isRecording) {
            videoRecordHelper.stopRecord()
            isRecording = false
        }
    }
}