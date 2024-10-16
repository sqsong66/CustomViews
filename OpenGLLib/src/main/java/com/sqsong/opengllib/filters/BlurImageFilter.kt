package com.sqsong.opengllib.filters

import android.content.Context
import android.opengl.GLES30
import com.sqsong.opengllib.common.Program
import com.sqsong.opengllib.common.Texture
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sqrt

class BlurImageFilter(
    context: Context,
    private var blurRadius: Int = 1,
    private var blurOffset: Float = 0.05f,
    initOutputBuffer: Boolean = true
) : BaseImageFilter(context, fragmentAssets = "shader/test.glsl", initOutputBuffer = initOutputBuffer) {

    override fun onPreDraw(program: Program, texture: Texture) {
        val sigma = blurRadius / 3.0f
        val sumWeight = (0 until blurRadius).sumByDouble { i ->
            val weight = (1.0 / sqrt(2.0 * Math.PI * sigma * sigma)) * exp(-i * i / (2.0 * sigma * sigma))
            weight
        }.toFloat()

        program.getUniformLocation("uBlurRadius").let {
            GLES30.glUniform1i(it, blurRadius)
        }

        program.getUniformLocation("uBlurOffset").let {
            GLES30.glUniform1f(it, blurOffset)
        }

        program.getUniformLocation("uSumWeight").let {
            GLES30.glUniform1f(it, sumWeight)
        }
    }

    override fun setProgress(progress: Float, extraType: Int) {
        blurRadius = range(progress, 1f, 45f).roundToInt()
    }
}