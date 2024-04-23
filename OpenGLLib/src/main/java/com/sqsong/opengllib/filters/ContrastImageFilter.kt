package com.sqsong.opengllib.filters

import android.content.Context
import android.opengl.GLES30
import android.util.Log
import com.sqsong.opengllib.common.Program

class ContrastImageFilter(
    context: Context,
    private var contrast: Float = 1f,
    initOutputBuffer: Boolean = true
) : BaseImageFilter(context, fragmentAssets = "shader/contrast_filter_frag.frag", initOutputBuffer = initOutputBuffer) {

    override fun onPreDraw(program: Program) {
        program.getUniformLocation("contrast").let {
            Log.d("songmao", "ContrastImageFilter onPreDraw: contrast location: $it")
            GLES30.glUniform1f(it, contrast)
        }
    }

    override fun setProgress(progress: Float, extraType: Int) {
        contrast = range(progress, 0.75f, 1.25f)
        Log.d("songmao", "BrightnessImageFilter setProgress: $progress, brightness: $contrast")
    }

}