package com.sqsong.opengllib.utils

import android.opengl.GLES30

fun checkGLError(tag: String) {
    GLES30.glGetError().let {
        if (it != GLES30.GL_NO_ERROR) {
            throw RuntimeException("$tag: glError $it")
        }
    }
}

fun loadShader(type: Int, shaderCode: String): Int {
    return GLES30.glCreateShader(type).also { shader ->
        GLES30.glShaderSource(shader, shaderCode)
        GLES30.glCompileShader(shader)
        val compileStatus = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            throw RuntimeException("Error compiling shader: ${GLES30.glGetShaderInfoLog(shader)}")
        }
    }
}