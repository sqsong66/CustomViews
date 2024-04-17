package com.sqsong.opengl.processor;

import android.opengl.GLES20;

import com.sqsong.opengl.processor.anno.Mode;
import com.sqsong.opengl.processor.utils.ShaderUtil;

/**
 * Created by yuxfzju on 16/8/10.
 */

class OffScreenBlurRenderer extends BaseBitmapRenderer {

    private int mRadius;
    @Mode
    private int mMode;

    @Override
    String getFragmentShaderCode() {
        return ShaderUtil.getFragmentShaderCode(mMode);
    }

    @Override
    public void draw(BaseBitmapRenderer.BlurContext blurContext) {
        drawOneDimenBlur(blurContext, true);
        drawOneDimenBlur(blurContext, false);
    }

    private void drawOneDimenBlur(BlurContext blurContext, boolean isHorizontal) {
        try {
            GLES20.glUseProgram(mProgram.id());

            int positionId = GLES20.glGetAttribLocation(mProgram.id(), "aPosition");
            GLES20.glEnableVertexAttribArray(positionId);
            GLES20.glVertexAttribPointer(positionId, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, VERTEX_STRIDE, mVertexBuffer);

            int texCoordId = GLES20.glGetAttribLocation(mProgram.id(), "aTexCoord");
            GLES20.glEnableVertexAttribArray(texCoordId);
            GLES20.glVertexAttribPointer(texCoordId, 2, GLES20.GL_FLOAT, false, 0, mTexCoordBuffer);

            if (isHorizontal) {
                blurContext.getBlurFrameBuffer().bindSelf();
            }

            int textureUniformId = GLES20.glGetUniformLocation(mProgram.id(), "uTexture");
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, isHorizontal ? blurContext.getInputTexture().id() : blurContext.getHorizontalTexture().id());
            GLES20.glUniform1i(textureUniformId, 0);

            int radiusId = GLES20.glGetUniformLocation(mProgram.id(), "uRadius");
            int widthOffsetId = GLES20.glGetUniformLocation(mProgram.id(), "uWidthOffset");
            int heightOffsetId = GLES20.glGetUniformLocation(mProgram.id(), "uHeightOffset");
            GLES20.glUniform1i(radiusId, mRadius);
            GLES20.glUniform1f(widthOffsetId, isHorizontal ? 0 : 1f / blurContext.getBitmap().getWidth());
            GLES20.glUniform1f(heightOffsetId, isHorizontal ? 1f / blurContext.getBitmap().getHeight() : 0);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, mDrawListBuffer);
            if (!isHorizontal) {
                GLES20.glDisableVertexAttribArray(positionId);
                GLES20.glDisableVertexAttribArray(texCoordId);
            }
        } finally {
            resetAllBuffer();
        }
    }

    void setBlurMode(@Mode int mode) {
        if (mMode != mode) {
            mNeedRelink = true;
            deletePrograms();
        }
        mMode = mode;
    }

    void setBlurRadius(int radius) {
        mRadius = radius;
    }
}
