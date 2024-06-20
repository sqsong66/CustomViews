package com.sqsong.opengllib.filters

import android.content.Context
import android.opengl.GLES30
import com.sqsong.opengllib.common.Program
import com.sqsong.opengllib.common.Texture

class HexagonMosaicFilter(
    context: Context,
    private var horizontalHexagons: Float = 100f,
    initOutputBuffer: Boolean = true
) : BaseImageFilter(context, fragmentAssets = "shader/hexagon_mosaic_frag.frag", initOutputBuffer = initOutputBuffer) {

    override fun onPreDraw(program: Program, texture: Texture) {
        program.getUniformLocation("horizontalHexagons").let {
            GLES30.glUniform1f(it, horizontalHexagons)
        }
        program.getUniformLocation("textureRatio").let {
            val ratio = texture.textureWidth.toFloat() / texture.textureHeight.toFloat()
            GLES30.glUniform1f(it, ratio)
        }
    }

    override fun setProgress(progress: Float, extraType: Int) {
        horizontalHexagons = 100f - progress * 90f
    }

}