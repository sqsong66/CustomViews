package com.wangxutech.picwish.libnative.beauty

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import com.wangxutech.picwish.libnative.beauty.filter.GPUImageFilter

class GPUImage(context: Context, private var filter: GPUImageFilter = GPUImageFilter(context)) {

    private val obj = Object()
    private var currentBitmap: Bitmap? = null
    private val renderer = GPUImageRenderer(filter)
    private var glSurfaceView: GLSurfaceView? = null

    init {
        check(supportsOpenGLES2(context)) { "OpenGL ES 2.0 is not supported on this phone." }
    }

    private fun supportsOpenGLES2(context: Context): Boolean {
        val configurationInfo = (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).deviceConfigurationInfo
        return configurationInfo.reqGlEsVersion >= 0x20000
    }

    fun setGLSurfaceView(surfaceView: GLSurfaceView) {
        surfaceView.apply {
            glSurfaceView = this
            setEGLContextClientVersion(2)
            setZOrderOnTop(true)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            // holder.setFormat(PixelFormat.RGBA_8888)
            holder.setFormat(PixelFormat.TRANSLUCENT)

            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
            requestRender()
        }
    }

    fun requestRender() = glSurfaceView?.requestRender()

    fun setFilter(filter: GPUImageFilter) {
        this.filter = filter
        renderer.setFilter(this.filter)
        requestRender()
    }

    fun setImage(bitmap: Bitmap) {
        currentBitmap = bitmap
        renderer.setImageBitmap(bitmap)
        requestRender()
    }

    fun deleteImage() {
        currentBitmap = null
        renderer.deleteImage()
        requestRender()
    }

    fun runOnGLThread(runnable: Runnable) {
        renderer.runOnDrawEnd(runnable)
    }

    fun getBitmapWithFilterApplied(): Bitmap? {
        return currentBitmap?.let { getBitmapWithFilterApplied(it) }
    }

    fun getBitmapWithFilterApplied(bitmap: Bitmap): Bitmap? {
        if (glSurfaceView != null) {
            renderer.deleteImage()
            renderer.runOnDraw {
                synchronized(obj) {
                    filter.destroy()
                    obj.notify()
                }
            }
            synchronized(obj) {
                requestRender()
                try {
                    obj.wait()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }

        val renderer = GPUImageRenderer(filter)
        val buffer = PixelBuffer(bitmap.width, bitmap.height)
        buffer.setRenderer(renderer)
        renderer.setImageBitmap(bitmap)
        val result = buffer.getBitmap()
        filter.destroy()
        renderer.deleteImage()
        buffer.destroy()

        this.renderer.setFilter(filter)
        if (currentBitmap != null) {
            this.renderer.setImageBitmap(currentBitmap)
        }
        requestRender()
        return result
    }

    fun getFilteredBitmap(bitmap: Bitmap): Bitmap? {
        val renderer = GPUImageRenderer(filter)
        val buffer = PixelBuffer(bitmap.width, bitmap.height)
        buffer.setRenderer(renderer)
        renderer.setImageBitmap(bitmap)
        val result = buffer.getBitmap()
        filter.destroy()
        renderer.deleteImage()
        buffer.destroy()
        return result
    }
}