package com.wangxutech.picwish.libnative.beauty.filter

import android.content.Context
import android.graphics.PointF
import android.opengl.GLES20
import com.wangxutech.picwish.libnative.R
import com.wangxutech.picwish.libnative.utils.OpenGlUtils
import java.nio.FloatBuffer
import java.util.*

open class GPUImageFilter(
    private val context: Context,
    private val vertexShader: String = OpenGlUtils.readGlShader(context, R.raw.vertex_no_filter),
    private val fragmentShader: String = OpenGlUtils.readGlShader(context, R.raw.frag_no_filter)
) {

    var glProgId: Int = 0
        private set

    var outputWidth: Int = 0
        private set

    var outputHeight: Int = 0
        private set

    var isInitialized = false
        private set
    private var glAttribPosition: Int = 0
    private var glUniformTexture: Int = 0
    private var glAttribTextureCoordinate: Int = 0
    private val runOnDraw = LinkedList<Runnable>()

    private fun init() {
        onInit()
        onInitialized()
    }

    open fun onInit() {
        glProgId = OpenGlUtils.loadProgram(vertexShader, fragmentShader)
        glAttribPosition = GLES20.glGetAttribLocation(glProgId, "position")
        glUniformTexture = GLES20.glGetUniformLocation(glProgId, "inputImageTexture")
        glAttribTextureCoordinate = GLES20.glGetAttribLocation(glProgId, "inputTextureCoordinate")
        isInitialized = true
    }

    open fun ifNeedInit() {
        if (!isInitialized) init()
    }

    open fun onDraw(textureId: Int, cubeBuffer: FloatBuffer, textureBuffer: FloatBuffer) {
        GLES20.glUseProgram(glProgId)
        runPendingOnDrawTasks()
        if (!isInitialized) return
        cubeBuffer.position(0)
        GLES20.glVertexAttribPointer(glAttribPosition, 2, GLES20.GL_FLOAT, false, 0, cubeBuffer)
        GLES20.glEnableVertexAttribArray(glAttribPosition)
        textureBuffer.position(0)
        GLES20.glVertexAttribPointer(glAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, textureBuffer)
        GLES20.glEnableVertexAttribArray(glAttribTextureCoordinate)
        if (textureId != OpenGlUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glUniform1i(glUniformTexture, 0)
        }
        onDrawArraysPre()
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(glAttribPosition)
        GLES20.glDisableVertexAttribArray(glAttribTextureCoordinate)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    protected open fun runPendingOnDrawTasks() {
        synchronized(runOnDraw) {
            while (!runOnDraw.isEmpty()) {
                runOnDraw.removeFirst().run()
            }
        }
    }

    open fun onOutputSizeChanged(width: Int, height: Int) {
        outputWidth = width
        outputHeight = height
    }

    fun destroy() {
        isInitialized = false
        GLES20.glDeleteProgram(glProgId)
        onDestroy()
    }

    protected open fun setInteger(location: Int, intValue: Int) {
        runOnDraw {
            ifNeedInit()
            GLES20.glUniform1i(location, intValue)
        }
    }

    protected open fun setFloat(location: Int, floatValue: Float) {
        runOnDraw {
            ifNeedInit()
            GLES20.glUniform1f(location, floatValue)
        }
    }

    protected open fun setFloatVec2(location: Int, arrayValue: FloatArray) {
        runOnDraw {
            ifNeedInit()
            GLES20.glUniform2fv(location, 1, FloatBuffer.wrap(arrayValue))
        }
    }

    protected open fun setFloatVec3(location: Int, arrayValue: FloatArray) {
        runOnDraw {
            ifNeedInit()
            GLES20.glUniform3fv(location, 1, FloatBuffer.wrap(arrayValue))
        }
    }

    protected open fun setFloatVec4(location: Int, arrayValue: FloatArray) {
        runOnDraw {
            ifNeedInit()
            GLES20.glUniform4fv(location, 1, FloatBuffer.wrap(arrayValue))
        }
    }

    protected open fun setFloatArray(location: Int, arrayValue: FloatArray) {
        runOnDraw {
            ifNeedInit()
            GLES20.glUniform1fv(location, arrayValue.size, FloatBuffer.wrap(arrayValue))
        }
    }

    protected open fun setPoint(location: Int, point: PointF) {
        runOnDraw {
            ifNeedInit()
            val vec2 = FloatArray(2)
            vec2[0] = point.x
            vec2[1] = point.y
            GLES20.glUniform2fv(location, 1, vec2, 0)
        }
    }

    protected open fun setUniformMatrix3f(location: Int, matrix: FloatArray) {
        runOnDraw {
            ifNeedInit()
            GLES20.glUniformMatrix3fv(location, 1, false, matrix, 0)
        }
    }

    protected open fun setUniformMatrix4f(location: Int, matrix: FloatArray) {
        runOnDraw {
            ifNeedInit()
            GLES20.glUniformMatrix4fv(location, 1, false, matrix, 0)
        }
    }

    protected open fun runOnDraw(runnable: Runnable) {
        synchronized(runOnDraw) {
            runOnDraw.addLast(runnable)
        }
    }

    protected open fun onInitialized() {}

    open fun onDrawArraysPre() {}

    protected open fun onDestroy() {}

}