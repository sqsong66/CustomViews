package com.sqsong.opengllib

import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import android.util.AttributeSet

class MultiTextureGLSurface(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    init {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        holder.setFormat(PixelFormat.TRANSLUCENT)
        setZOrderOnTop(true)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

}