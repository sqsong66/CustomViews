package com.wangxutech.picwish.libnative.beauty.filter

import android.content.Context
import android.opengl.GLES20
import com.wangxutech.picwish.libnative.R
import com.wangxutech.picwish.libnative.utils.OpenGlUtils.readGlShader

class GPUImageBeautyFilter(context: Context) : GPUImageFilter(
    context,
    readGlShader(context, R.raw.vertex_no_filter),
    readGlShader(context, R.raw.frag_beauty)
) {
    private var opacity = 0f
    private var brightness = 0f

    private var widthHandle = 0
    private var heightHandle = 0
    private var levelHandle = 0
    private var brightnessHandle = 0

    override fun onInit() {
        super.onInit()
        widthHandle = GLES20.glGetUniformLocation(glProgId, "width")
        heightHandle = GLES20.glGetUniformLocation(glProgId, "height")
        levelHandle = GLES20.glGetUniformLocation(glProgId, "opacity")
        brightnessHandle = GLES20.glGetUniformLocation(glProgId, "brightness")
    }

    override fun onOutputSizeChanged(width: Int, height: Int) {
        super.onOutputSizeChanged(width, height)
        setInteger(widthHandle, width)
        setInteger(heightHandle, height)
        setFloat(levelHandle, opacity)
    }

    fun setBeautyLevel(percent: Float) {
        opacity = percent
    }

    fun setBrightnessLevel(percent: Float) {
        val brightness = 0.2f * percent
        this.brightness = brightness
    }

    override fun onDrawArraysPre() {
        super.onDrawArraysPre()
        GLES20.glUniform1f(levelHandle, opacity)
        GLES20.glUniform1f(brightnessHandle, brightness)
    }
}