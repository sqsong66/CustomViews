package com.sqsong.opengllib.filters

import android.content.Context
import android.opengl.GLES30
import android.util.Log
import com.sqsong.opengllib.common.Program

class ExposureImageFilter(
    context: Context,
    private var exposure: Float = 0f,
    initOutputBuffer: Boolean = true
) : BaseImageFilter(context, fragmentAssets = "shader/exposure_filter_frag.frag", initOutputBuffer = initOutputBuffer) {

    override fun onPreDraw(program: Program) {
        program.getUniformLocation("exposure").let {
            Log.d("songmao", "ContrastImageFilter onPreDraw: exposure location: $it")
            GLES30.glUniform1f(it, exposure)
        }
    }

    override fun setProgress(progress: Float, extraType: Int) {
        exposure = range(progress, -1f, 0.6f)
        Log.d("songmao", "BrightnessImageFilter setProgress: $progress, exposure: $exposure")
    }

}