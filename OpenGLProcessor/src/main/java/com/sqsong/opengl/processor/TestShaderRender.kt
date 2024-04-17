package com.sqsong.opengl.processor

import android.opengl.GLES20

class TestShaderRender: BaseBitmapRenderer() {
    override fun getFragmentShaderCode(): String {
        return """
            precision highp float;
            varying vec2 vTexCoord;
            uniform sampler2D vTexture;
            const float tmp = 100.5/255.0;
            float sRGBline(float x) {
            	float res =  x <  0.04045 ? 0.07739938 * x : pow((x + 0.055) * 0.94786730, 2.4);
                return clamp(res, 0.0, 1.0);
            }
            float rcplineP3(float x) {
                  float res = x < 0.0030186 ? x * 12.92 : pow(x, 0.41666667)* 1.055 - 0.055;
                   return clamp(res+tmp, 0.0, 1.0);
             }
            vec4 srgbToP3(vec4 tempvalue) {
                vec3 linearrgb = vec3(sRGBline(tempvalue.r), sRGBline(tempvalue.g), sRGBline(tempvalue.b));
                mat3 rgb2xyz2rgb = mat3(0.82246214, 0.17753820, 0, 0.03319423, 0.96680593, 0, 0.01708263, 0.07239738, 0.91052002);
                vec3 linearP3error = linearrgb * rgb2xyz2rgb;
                return vec4(rcplineP3(linearP3error.r), rcplineP3(linearP3error.g), rcplineP3(linearP3error.b), tempvalue.a);
            }
            void main() {
                vec4 color = texture2D(vTexture, vTexCoord);
                gl_FragColor = srgbToP3(color);
           }

        """.trimIndent()
    }

    override fun draw(blurContext: BlurContext) {
        GLES20.glUseProgram(mProgram.id())

        val positionId = GLES20.glGetAttribLocation(mProgram.id(), "aPosition")
        GLES20.glEnableVertexAttribArray(positionId)
        GLES20.glVertexAttribPointer(positionId, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, VERTEX_STRIDE, mVertexBuffer)

        val texCoordId = GLES20.glGetAttribLocation(mProgram.id(), "aTexCoord")
        GLES20.glEnableVertexAttribArray(texCoordId)
        GLES20.glVertexAttribPointer(texCoordId, 2, GLES20.GL_FLOAT, false, 0, mTexCoordBuffer)

        val textureUniformId = GLES20.glGetUniformLocation(mProgram.id(), "vTexture")
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, blurContext.inputTexture.id())
        GLES20.glUniform1i(textureUniformId, 0)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.size, GLES20.GL_UNSIGNED_SHORT, mDrawListBuffer)

        GLES20.glDisableVertexAttribArray(positionId)
        GLES20.glDisableVertexAttribArray(texCoordId)
    }
}