package com.sqsong.opengl.processor

import android.opengl.GLES20
import android.util.Log
import com.sqsong.opengl.processor.utils.getTriangleMosaicFragmentShader

class MosaicRender : BaseBitmapRenderer() {

    private var mosaicSize = 10f

    // 三角形马赛克
    override fun getFragmentShaderCode(): String {
        return  getTriangleMosaicFragmentShader()
    }

    override fun draw(blurContext: BlurContext) {
        try {
            GLES20.glUseProgram(mProgram.id())

            val positionId = GLES20.glGetAttribLocation(mProgram.id(), "aPosition")
            GLES20.glEnableVertexAttribArray(positionId)
            GLES20.glVertexAttribPointer(positionId, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, VERTEX_STRIDE, mVertexBuffer)

            val texCoordId = GLES20.glGetAttribLocation(mProgram.id(), "aTexCoord")
            GLES20.glEnableVertexAttribArray(texCoordId)
            GLES20.glVertexAttribPointer(texCoordId, 2, GLES20.GL_FLOAT, false, 0, mTexCoordBuffer)

            val textureUniformId = GLES20.glGetUniformLocation(mProgram.id(), "u_Texture")
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, blurContext.inputTexture.id())
            GLES20.glUniform1i(textureUniformId, 0)

            val mosaicSizeUniformId = GLES20.glGetUniformLocation(mProgram.id(), "u_MosaicSize")
            GLES20.glUniform1f(mosaicSizeUniformId, mosaicSize)

            val aspectRatioUniformId = GLES20.glGetUniformLocation(mProgram.id(), "u_AspectRatio")
            GLES20.glUniform1f(aspectRatioUniformId, blurContext.bitmap.width.toFloat() / blurContext.bitmap.height.toFloat())

            GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.size, GLES20.GL_UNSIGNED_SHORT, mDrawListBuffer)

            GLES20.glDisableVertexAttribArray(positionId)
            GLES20.glDisableVertexAttribArray(texCoordId)
        } finally {
            resetAllBuffer()
        }
    }

    fun setMosaicSize(size: Float) {
        Log.w("songmao", "MosaicSize: $size")
        this.mosaicSize = size
    }

}