package com.wangxutech.picwish.libnative.beauty.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import com.wangxutech.picwish.libnative.NativeLib
import com.wangxutech.picwish.libnative.beauty.GPUImage
import com.wangxutech.picwish.libnative.beauty.filter.GPUImageFilter
import java.util.concurrent.Semaphore

class GPUImageGLSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    private val gpuImage = GPUImage(context)

    init {
        gpuImage.setGLSurfaceView(this)
    }

    fun setImageBitmap(bitmap: Bitmap) {
        var newWidth: Int = measuredWidth
        var newHeight: Int = newWidth * bitmap.height / bitmap.width
        if (newHeight > measuredHeight) {
            newHeight = measuredHeight
            newWidth = newHeight * bitmap.width / bitmap.height
        }
        layoutParams.apply {
            width = newWidth
            height = newHeight
            layoutParams = this
        }
        gpuImage.setImage(bitmap)
    }

    fun setFilter(filter: GPUImageFilter) {
        gpuImage.setFilter(filter)
        requestRender()
    }

    fun requestNewRender() = gpuImage.requestRender()

    fun getBitmapWithFilterApplied(): Bitmap? = gpuImage.getBitmapWithFilterApplied()

    fun getResultBitmap(bitmap: Bitmap): Bitmap? {
        return gpuImage.getBitmapWithFilterApplied(bitmap)
    }

    fun getFilteredBitmap(bitmap: Bitmap): Bitmap? {
        return gpuImage.getBitmapWithFilterApplied(bitmap)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return true
    }

    @Throws(InterruptedException::class)
    fun capture(): Bitmap? {
        val waiter = Semaphore(0)
        val width = measuredWidth
        val height = measuredHeight
        // Take picture on OpenGL thread
        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        gpuImage.runOnGLThread {
            NativeLib.adjustBitmap(resultBitmap)
            waiter.release()
        }
        requestRender()
        waiter.acquire()
        return resultBitmap
    }
}