package com.sqsong.opengllib.filters

import android.content.Context
import android.opengl.GLES30
import android.util.Log
import com.sqsong.opengllib.common.Program

class ShadowImageFilter(
    context: Context,
    private var shadowStrength: Float = 0f,
    initOutputBuffer: Boolean = true
) : BaseImageFilter(context, fragmentAssets = "shader/shadow_filter_frag.frag", initOutputBuffer = initOutputBuffer) {

    override fun onPreDraw(program: Program) {
        program.getUniformLocation("shadowStrength").let {
            Log.d("songmao", "ContrastImageFilter onPreDraw: saturation location: $it")
            GLES30.glUniform1f(it, shadowStrength)
        }
    }

    override fun setProgress(progress: Float, extraType: Int) {
        shadowStrength =  -range(progress, -0.5f, 0.5f)
        Log.d("songmao", "BrightnessImageFilter setProgress: $progress, shadowStrength: $shadowStrength")
    }
}