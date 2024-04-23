package com.sqsong.opengllib.filters

import android.content.Context
import android.opengl.GLES30
import android.util.Log
import com.sqsong.opengllib.common.Program

class BrightnessImageFilter(
    context: Context,
    private var brightness: Float = 0f,
    initOutputBuffer: Boolean = true
) : BaseImageFilter(context, fragmentAssets = "shader/brightness_filter_frag.frag", initOutputBuffer = initOutputBuffer) {

    override fun onPreDraw(program: Program) {
        program.getUniformLocation("brightness").let {
            Log.w("BaseImageFilter", "BrightnessImageFilter onPreDraw: brightness: $brightness")
            GLES30.glUniform1f(it, brightness)
        }
    }

    override fun setProgress(progress: Float, extraType: Int) {
        brightness = range(progress, -0.15f, 0.15f)
        Log.d("BaseImageFilter", "BrightnessImageFilter setProgress: $progress, brightness: $brightness")
    }

}