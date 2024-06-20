package com.sqsong.opengllib.effect

import android.content.Context
import android.graphics.Bitmap
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.GlUtil.GlException
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.ColorLut
import androidx.media3.effect.GlShaderProgram

@UnstableApi
class BitmapColorLut(private val lutBitmap: Bitmap) : ColorLut {

    private var lutTextureId = 0
    private var shaderProgram: LutBitmapShaderProgram? = null

    override fun getLutTextureId(presentationTimeUs: Long): Int {
        return lutTextureId
    }

    override fun getLength(presentationTimeUs: Long): Int {
        return 0
    }

    override fun release() {
        GlUtil.deleteTexture(lutTextureId)
        shaderProgram?.release()
    }

    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram {
        try {
            lutTextureId = GlUtil.createTexture(lutBitmap)
        } catch (e: GlException) {
            throw VideoFrameProcessingException("Could not store the LUT as a texture.", e)
        }
        return LutBitmapShaderProgram(context, lutTextureId).apply {
            shaderProgram = this
        }
    }

    fun setIntensity(intensity: Float) {
        shaderProgram?.setIntensity(intensity)
    }
}