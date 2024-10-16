package com.sqsong.opengllib.filters

import android.content.Context
import android.opengl.GLES30
import com.sqsong.opengllib.common.Program
import com.sqsong.opengllib.common.Texture

class HighlightShadowFilter(
    context: Context,
    private var highlight: Float = 1f,
    private var shadow: Float = 1f,
    initOutputBuffer: Boolean = true
) : BaseImageFilter(context, fragmentAssets = "shader/frag_highlight_shadow_new.glsl", initOutputBuffer = initOutputBuffer) {

    override fun onPreDraw(program: Program, texture: Texture) {
        program.getUniformLocation("highlights").let {
            GLES30.glUniform1f(it, highlight)
        }

        program.getUniformLocation("shadows").let {
            GLES30.glUniform1f(it, shadow)
        }
    }

    override fun setProgress(progress: Float, extraType: Int) {
        when (extraType) {
            FilterMode.FILTER_HIGHLIGHT -> {
                val value = range(progress, 0.2f, 1.8f)
                highlight = value
            }

            FilterMode.FILTER_SHADOW -> {
                val value = range(progress, 0.6f, 1.4f)
                shadow = value
            }
        }
    }

}