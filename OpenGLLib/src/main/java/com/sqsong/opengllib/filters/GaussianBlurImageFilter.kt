package com.sqsong.opengllib.filters

import android.content.Context
import android.opengl.GLES30
import android.util.Log
import com.sqsong.opengllib.common.FrameBuffer
import com.sqsong.opengllib.common.GLVertexLinker
import com.sqsong.opengllib.common.Program
import com.sqsong.opengllib.common.Texture

class GaussianBlurImageFilter(
    context: Context,
    private var blurRadius: Int = 0,
    initOutputBuffer: Boolean = true,
) : BaseImageFilter(context, fragmentAssets = "shader/gaussian_blur_filter_frag.frag", initOutputBuffer = initOutputBuffer) {

    private var horizontalBlurFbo: FrameBuffer? = null

    override fun onInputTextureLoaded(textureWidth: Int, textureHeight: Int) {
        super.onInputTextureLoaded(textureWidth, textureHeight)
        horizontalBlurFbo?.delete()
        horizontalBlurFbo = null
        horizontalBlurFbo = FrameBuffer(textureWidth, textureHeight)
    }

    override fun onBeforeFrameBufferDraw(inputTexture: Texture, fboProgram: Program?, defaultFboGLVertexLinker: GLVertexLinker): Texture? {
        // horizontal blur
        horizontalBlurFbo?.bindFrameBuffer()
        val width = horizontalBlurFbo?.width ?: 0
        val height = horizontalBlurFbo?.height ?: 0
        GLES30.glViewport(0, 0, width, height)
        fboProgram?.getUniformLocation("uTexture")?.let {
            Log.d("GaussianBlurImageFilter", "onBeforeFrameBufferDraw, textureId: ${inputTexture.textureId}, uTexture location: $it")
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            inputTexture.bindTexture()
            GLES30.glUniform1i(it, 0)
        }
        fboProgram?.let { program ->
            setBlurParams(program, isHorizontal = true)
        }
        defaultFboGLVertexLinker.draw()
        inputTexture.unbindTexture()
        return horizontalBlurFbo?.texture
    }

    override fun onPreDraw(program: Program) {
        setBlurParams(program, isHorizontal = false)
    }

    private fun setBlurParams(program: Program, isHorizontal: Boolean) {
        program.getUniformLocation("uRadius").let {
            GLES30.glUniform1i(it, blurRadius)
        }
        val width = horizontalBlurFbo?.width?.toFloat() ?: 0f
        val height = horizontalBlurFbo?.height?.toFloat() ?: 0f
        program.getUniformLocation("uWidthOffset").let {
            GLES30.glUniform1f(it, if (isHorizontal) 0f else 1f / width)
        }
        program.getUniformLocation("uHeightOffset").let {
            GLES30.glUniform1f(it, if (isHorizontal) 1f / height else 0f)
        }
    }

    override fun setProgress(progress: Float, extraType: Int) {
        blurRadius = (progress * 50).toInt()
    }
}