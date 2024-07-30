package com.sqsong.opengllib

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import android.util.AttributeSet

class MultiTextureGLSurfaceView(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    private val textureRender by lazy { MultiTextureRender(context) }

    init {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        holder.setFormat(PixelFormat.TRANSLUCENT)
        setZOrderOnTop(true)
        setRenderer(textureRender)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun setImageBitmaps(bitmapList: List<Bitmap>) {
        textureRender.setImageBitmaps(bitmapList)
    }

    override fun onPause() {
        textureRender.onDestroy()
        super.onPause()
    }

    fun onDestroy() {
        textureRender.onDestroy()
    }

    fun startRecord() {
        textureRender.startRecord()
    }

    fun stopRecord() {
        textureRender.stopRecord()
    }
}