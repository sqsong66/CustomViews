package com.wangxutech.picwish.libnative.beauty.filter

import android.content.Context
import android.opengl.GLES20
import com.wangxutech.picwish.libnative.R
import com.wangxutech.picwish.libnative.utils.OpenGlUtils.readGlShader

class GPUImageSmoothFilter(context: Context) : GPUImageFilter(
    context,
    readGlShader(context, R.raw.vertex_no_filter),
    readGlShader(context, R.raw.frag_smooth)
) {
    private var widthHandle = 0
    private var heightHandle = 0
    private var levelHandle = 0
    private var opacity = 0f

    override fun onInit() {
        super.onInit()
        widthHandle = GLES20.glGetUniformLocation(glProgId, "width")
        heightHandle = GLES20.glGetUniformLocation(glProgId, "height")
        levelHandle = GLES20.glGetUniformLocation(glProgId, "opacity")
    }

    override fun onOutputSizeChanged(width: Int, height: Int) {
        super.onOutputSizeChanged(width, height)
        setInteger(widthHandle, width)
        setInteger(heightHandle, height)
        setFloat(levelHandle, opacity)
    }

    fun setSmoothOpacity(percent: Float) {
        opacity = percent
    }

    override fun onDrawArraysPre() {
        super.onDrawArraysPre()
        GLES20.glUniform1f(levelHandle, opacity)
    }
}