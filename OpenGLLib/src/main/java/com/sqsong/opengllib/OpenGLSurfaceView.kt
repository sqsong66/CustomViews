package com.sqsong.opengllib

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import com.sqsong.opengllib.filters.BaseImageFilter
import com.sqsong.opengllib.filters.LUTImageFilter

class OpenGLSurfaceView(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    private var imageFilter: BaseImageFilter = BaseImageFilter(context)

    private val render by lazy {
        OpenGLRender(imageFilter)
    }

    init {
        setEGLContextClientVersion(3)
        setRenderer(render)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun setImageBitmap(bitmap: Bitmap) {
        render.setImageBitmap(bitmap)
        requestRender()
    }

    fun setFilter(filter: BaseImageFilter, progress: Float = 0f) {
        render.setFilter(filter, progress)
        requestRender()
    }

    fun setProgress(progress: Float) {
        render.setProgress(progress)
        requestRender()
    }

    override fun onPause() {
        super.onPause()
        render.onDestroy()
    }

    fun getRenderedBitmap(): Bitmap? {
        return render.getRenderedBitmap()
    }

}