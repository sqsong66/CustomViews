package com.wangxutech.picwish.libnative.beauty.filter

import android.content.Context
import android.opengl.GLES20
import com.wangxutech.picwish.libnative.R
import com.wangxutech.picwish.libnative.utils.OpenGlUtils

class GPUImageSaturationFilter(context: Context) : GPUImageFilter(
    context,
    OpenGlUtils.readGlShader(context, R.raw.vertex_no_filter),
    OpenGlUtils.readGlShader(context, R.raw.frag_saturation)
) {
    private var saturation: Float = 1.0f // 0f~2f
    private var saturationHandle = 0

    override fun onInit() {
        super.onInit()
        saturationHandle = GLES20.glGetUniformLocation(glProgId, "saturation")
    }

    override fun onOutputSizeChanged(width: Int, height: Int) {
        super.onOutputSizeChanged(width, height)
        setFloat(saturationHandle, saturation)
    }

    override fun onDrawArraysPre() {
        super.onDrawArraysPre()
        GLES20.glUniform1f(saturationHandle, saturation)
    }

    fun setSaturationLevel(saturation: Float) { // -1~1
        this.saturation = 1 + saturation
    }
}