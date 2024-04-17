package com.sqsong.opengllib.filters

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES30
import android.util.Log
import com.sqsong.opengllib.common.BitmapTexture
import com.sqsong.opengllib.common.Program
import com.sqsong.opengllib.common.Texture

class LUTImageFilter(
    context: Context,
    private val lutBitmap: Bitmap,
    private var intensity: Float = 1.0f,
) : BaseImageFilter(context, fragmentAssets = "shader/lut_filter_frag.frag") {

    private var lutTexture: Texture? = null

    override fun onInitialized(program: Program) {
        lutTexture = BitmapTexture(lutBitmap)
    }

    override fun onPreDraw(program: Program) {
        Log.d("BaseImageFilter", "onPreDraw, textureId: ${lutTexture?.textureId}")
        program.getUniformLocation("uLookupTexture").let {
            Log.w("BaseImageFilter", "onPreDraw, uLookupTexture location: $it")
            GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
            lutTexture?.bindTexture()
            GLES30.glUniform1i(it, 1)
        }

        program.getUniformLocation("intensity").let {
            Log.w("BaseImageFilter", "onPreDraw, intensity location: $it, intensity: $intensity")
            GLES30.glUniform1f(it, intensity)
        }
    }

    override fun onAfterDraw() {
        lutTexture?.unbindTexture()
    }

    override fun setProgress(progress: Float) {
        intensity = progress
    }

    override fun onDestroy() {
        super.onDestroy()
        lutTexture?.delete()
        lutTexture = null
    }
}