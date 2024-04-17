package com.sqsong.opengllib

import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLES31
import android.opengl.GLSurfaceView
import android.util.Log
import com.sqsong.opengllib.filters.BaseImageFilter
import java.util.LinkedList
import java.util.Queue
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class OpenGLRender(
    private var imageFilter: BaseImageFilter,
) : GLSurfaceView.Renderer {

    private var viewWidth = 0
    private var viewHeight = 0
    private var imageBitmap: Bitmap? = null
    private val runOnDraw = LinkedList<Runnable>()

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig?) {
        Log.d("BaseImageFilter", "onSurfaceCreated")
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        imageFilter.ifNeedInit()
        imageBitmap?.let {
            imageFilter.setImageBitmap(it)
        }
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        Log.d("BaseImageFilter", "onSurfaceChanged")
        GLES30.glViewport(0, 0, width, height)
        imageFilter.onViewSizeChanged(width, height)
    }

    override fun onDrawFrame(gl: GL10) {
        Log.d("BaseImageFilter", "onDrawFrame")
        GLES30.glClear(GLES31.GL_COLOR_BUFFER_BIT or GLES31.GL_DEPTH_BUFFER_BIT)
        runAll(runOnDraw)
        imageFilter.onDrawFrame()
    }

    fun setImageBitmap(bitmap: Bitmap) {
        this.imageBitmap = bitmap
        runOnDraw {
            imageFilter.setImageBitmap(bitmap)
        }
    }

    fun setFilter(filter: BaseImageFilter, progress: Float) {
        runOnDraw {
            val oldFilter = imageFilter
            this.imageFilter = filter
            this.imageFilter.setProgress(progress)
            oldFilter.onDestroy()
            imageFilter.ifNeedInit()
            imageBitmap?.let {
                imageFilter.setImageBitmap(it)
            }
            imageFilter.onViewSizeChanged(viewWidth, viewHeight)
        }
    }

    fun onDestroy() {
        imageFilter.onDestroy()
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

    fun onPause() {
        imageFilter.onDestroy()
    }

    fun setProgress(progress: Float) {
        imageFilter.setProgress(progress)
    }

    fun getRenderedBitmap(): Bitmap? {
        return imageFilter.getRenderedBitmap()
    }

}