package com.sqsong.opengllib.filters

import android.content.Context
import android.opengl.GLES30
import com.sqsong.opengllib.common.Program
import com.sqsong.opengllib.common.Texture

class BeautyImageFilter(
    context: Context,
    private var beautyWhite: Float = 0f,
    private var beautyValue: Float = 0f,
    initOutputBuffer: Boolean = true
) : BaseImageFilter(context, fragmentAssets = "shader/frag_beauty_microdermabrasion.glsl", initOutputBuffer = initOutputBuffer) {

    override fun onPreDraw(program: Program, texture: Texture) {
        program.getUniformLocation("brightness").let {
            GLES30.glUniform1f(it, beautyWhite)
        }

        program.getUniformLocation("opacity").let {
            GLES30.glUniform1f(it, beautyValue)
        }

        program.getUniformLocation("width").let {
            GLES30.glUniform1i(it, texture.textureWidth)
        }

        program.getUniformLocation("height").let {
            GLES30.glUniform1i(it, texture.textureHeight)
        }
    }

    override fun setProgress(progress: Float, extraType: Int) {
        // val value = range(progress, 0f, 0.3f)
        beautyValue = progress
    }

}