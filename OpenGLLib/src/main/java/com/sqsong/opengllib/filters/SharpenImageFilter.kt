package com.sqsong.opengllib.filters

import android.content.Context
import android.opengl.GLES30
import android.util.Log
import com.sqsong.opengllib.common.Program

class SharpenImageFilter(
    context: Context,
    private var sharpen: Float = 0f,
    initOutputBuffer: Boolean = true
) : BaseImageFilter(context, fragmentAssets = "shader/sharpen_filter_frag.frag", initOutputBuffer = initOutputBuffer) {

    override fun onPreDraw(program: Program) {
        program.getUniformLocation("strength").let {
            Log.d("songmao", "ContrastImageFilter onPreDraw: sharpen location: $it")
            GLES30.glUniform1f(it, sharpen)
        }
    }

    override fun setProgress(progress: Float, extraType: Int) {
        sharpen = range(progress, 0f, 1f)
        Log.d("songmao", "BrightnessImageFilter setProgress: $progress, sharpen: $sharpen")
    }
}