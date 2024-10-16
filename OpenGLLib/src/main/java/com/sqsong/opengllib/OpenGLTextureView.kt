package com.sqsong.opengllib

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.util.Log
import com.sqsong.opengllib.filters.BaseImageFilter

class OpenGLTextureView(
    context: Context,
    attrs: AttributeSet? = null
) : GLTextureView(context, attrs) {

    private var imageFilter: BaseImageFilter = BaseImageFilter(context)

    private val render by lazy {
        OpenGLRender(imageFilter)
    }

    init {
        setEGLContextClientVersion(3)
        isOpaque = false
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        setRenderer(render)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun setImageBitmap(bitmap: Bitmap) {
        render.setImageBitmap(bitmap)
        requestRender()
    }

    fun setFilter(filter: BaseImageFilter, progress: Float = Float.MIN_VALUE) {
        render.setFilter(filter, progress)
        requestRender()
    }

    fun setProgress(progress: Float, extraType: Int = 0) {
        render.setProgress(progress, extraType)
        requestRender()
    }

    override fun onPause() {
        super.onPause()
        render.onDestroy()
    }

    fun onDestroy() {
        render.onDestroy()
    }

    fun getRenderedBitmap(): Bitmap? {
        return render.getRenderedBitmap()
    }

    fun setGlBackgroundColor(color: Int) {
        render.setGlBackgroundColor(color)
        requestRender()
    }

}