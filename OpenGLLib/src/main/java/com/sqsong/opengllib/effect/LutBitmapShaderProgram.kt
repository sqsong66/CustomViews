package com.sqsong.opengllib.effect

import android.content.Context
import android.opengl.GLES20
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.GlUtil.GlException
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BaseGlShaderProgram

private const val VERTEX_SHADER_PATH = "shader/vertex_shader_transformation_es2.glsl"
private const val FRAGMENT_SHADER_PATH = "shader/fragment_shader_lut_es2.glsl"

@UnstableApi
class LutBitmapShaderProgram(context: Context, private val lutTexId: Int) : BaseGlShaderProgram(false, 1) {

    private var intensity = 1f
    private var glProgram: GlProgram? = null

    init {
        try {
            // 初始化是加载shader，并设置顶点坐标以及纹理坐标
            glProgram = GlProgram(context, VERTEX_SHADER_PATH, FRAGMENT_SHADER_PATH).also { glProgram ->
                glProgram.setBufferAttribute(
                    "aFramePosition",
                    GlUtil.getNormalizedCoordinateBounds(),
                    GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE
                )
                val identityMatrix = GlUtil.create4x4IdentityMatrix()
                glProgram.setFloatsUniform("uTransformationMatrix", identityMatrix)
                glProgram.setFloatsUniform("uTexTransformationMatrix", identityMatrix)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        return Size(inputWidth, inputHeight)
    }

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        try {
            glProgram?.let { glProgram ->
                glProgram.use()
                glProgram.setSamplerTexIdUniform("uTexSampler", inputTexId, 0)
                glProgram.setSamplerTexIdUniform("uColorLut", lutTexId, 1)
                glProgram.setFloatUniform("intensity", intensity)
                glProgram.bindAttributesAndUniforms()
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun setIntensity(intensity: Float) {
        this.intensity = intensity
    }

    override fun release() {
        super.release()
        try {
            // colorLut.release()
            glProgram?.delete()
        } catch (e: GlException) {
            throw VideoFrameProcessingException(e)
        }
    }
}