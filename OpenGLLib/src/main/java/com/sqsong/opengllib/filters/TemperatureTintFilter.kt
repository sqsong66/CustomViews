package com.sqsong.opengllib.filters

import android.content.Context
import android.opengl.GLES30
import android.util.Log
import com.sqsong.opengllib.common.Program
import com.sqsong.opengllib.common.Texture

class TemperatureTintFilter(
    context: Context,
    private var temperature: Float = 0f,
    private var tint: Float = 0f,
    initOutputBuffer: Boolean = true
) : BaseImageFilter(context, fragmentAssets = "shader/frag_temperature_tint.glsl", initOutputBuffer = initOutputBuffer) {

    override fun onPreDraw(program: Program, texture: Texture) {
        program.getUniformLocation("temperature").let {
            GLES30.glUniform1f(it, temperature)
        }

        program.getUniformLocation("tint").let {
            GLES30.glUniform1f(it, tint)
        }
    }

    override fun setProgress(progress: Float, extraType: Int) {
        when (extraType) {
            FilterMode.FILTER_TEMPERATURE -> {
                temperature = range(progress, -0.3f, 0.3f)
                Log.d("songmao", "TemperatureTintFilter setProgress: $progress, temperature: $temperature")
            }
            FilterMode.FILTER_TINT -> {
                tint =  range(progress, -0.5f, 0.5f)
                Log.d("songmao", "TemperatureTintFilter setProgress: $progress, tint: $tint")
            }
        }
    }


}