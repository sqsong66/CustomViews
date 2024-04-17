package com.sqsong.opengl.processor

import android.graphics.Bitmap
import android.opengl.GLES20
import android.util.Log
import com.sqsong.opengl.processor.utils.getLutFilterFragmentShader
import com.sqsong.opengl.processor.utils.getLutFilterFragmentShader1

class LutFilterRender : BaseBitmapRenderer() {

    // 颜色混合比例
    private var intensity = 1.0f
    // LUT纹理
    private var lutTexture: Texture? = null

    // 获取滤镜片元着色器代码
    override fun getFragmentShaderCode(): String {
        return getLutFilterFragmentShader1()
    }

    override fun draw(blurContext: BlurContext) {
        try {
            GLES20.glUseProgram(mProgram.id())

            // 设置顶点参数
            val positionId = GLES20.glGetAttribLocation(mProgram.id(), "aPosition")
            GLES20.glEnableVertexAttribArray(positionId)
            GLES20.glVertexAttribPointer(positionId, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, VERTEX_STRIDE, mVertexBuffer)

            // 设置纹理坐标参数
            val texCoordId = GLES20.glGetAttribLocation(mProgram.id(), "aTexCoord")
            GLES20.glEnableVertexAttribArray(texCoordId)
            GLES20.glVertexAttribPointer(texCoordId, 2, GLES20.GL_FLOAT, false, 0, mTexCoordBuffer)

            // 设置目标纹理
            val textureUniformId = GLES20.glGetUniformLocation(mProgram.id(), "u_Texture")
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, blurContext.inputTexture.id())
            GLES20.glUniform1i(textureUniformId, 0)

            // 设置LUT纹理
            val lutTextureUniformId = GLES20.glGetUniformLocation(mProgram.id(), "lookupTexture")
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
            Log.w("songmao", "lutTexture: ${lutTexture?.id()}, inputTexture: ${blurContext.inputTexture.id()}")
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, lutTexture?.id() ?: 0)
            GLES20.glUniform1i(lutTextureUniformId, 1)

            // 设置混合比例
            val intensityUniformId = GLES20.glGetUniformLocation(mProgram.id(), "intensity")
            GLES20.glUniform1f(intensityUniformId, intensity)

            // lookupDimension float
            val lookupDimensionUniformId = GLES20.glGetUniformLocation(mProgram.id(), "lookupDimension")
            GLES20.glUniform1f(lookupDimensionUniformId, 64f)

            // 绘制
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.size, GLES20.GL_UNSIGNED_SHORT, mDrawListBuffer)

            GLES20.glDisableVertexAttribArray(positionId)
            GLES20.glDisableVertexAttribArray(texCoordId)
        } finally {
            resetAllBuffer()
        }
    }

    fun setLutTextureBitmap(bitmap: Bitmap, intensity: Float = 1.0f) {
        this.intensity = intensity
        this.lutTexture = Texture.create(bitmap)
    }
}