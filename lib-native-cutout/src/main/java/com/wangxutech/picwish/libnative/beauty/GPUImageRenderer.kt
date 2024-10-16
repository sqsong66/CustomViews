package com.wangxutech.picwish.libnative.beauty

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.wangxutech.picwish.libnative.beauty.filter.GPUImageFilter
import com.wangxutech.picwish.libnative.utils.OpenGlUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.roundToInt

class GPUImageRenderer(private var filter: GPUImageFilter) : GLSurfaceView.Renderer {

    private var glTextureId: Int = NO_IMAGE
    private val runOnDraw = LinkedList<Runnable>()
    private val runOnDrawEnd = LinkedList<Runnable>()

    private var imageWidth: Int = 0
    private var imageHeight: Int = 0

    var outputWidth: Int = 0
        private set

    var outputHeight: Int = 0
        private set

    private val glCubeBuffer: FloatBuffer by lazy {
        ByteBuffer.allocateDirect(vertexCoor.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(vertexCoor)
                position(0)
            }
    }

    private val glTextureBuffer: FloatBuffer by lazy {
        ByteBuffer.allocateDirect(textureCoor.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(textureCoor)
                position(0)
            }
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig?) {
        // GLES20.glClearColor(247f / 255f, 248f / 255f, 250f / 255f, 1.0f)
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
//        GLES20.glEnable(GLES20.GL_BLEND)
//        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
//        GLES20.glEnable(GL10.GL_BLEND)
//        GLES20.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA)
        filter.ifNeedInit()
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        outputWidth = width
        outputHeight = height
        GLES20.glViewport(0, 0, width, height)
        GLES20.glUseProgram(filter.glProgId)
        filter.onOutputSizeChanged(width, height)
        adjustImageScaling()
    }

    override fun onDrawFrame(gl: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        runAll(runOnDraw)
        filter.onDraw(glTextureId, glCubeBuffer, glTextureBuffer)
        runAll(runOnDrawEnd)
    }

    private fun runAll(queue: Queue<Runnable>) {
        synchronized(queue) {
            while (!queue.isEmpty()) {
                queue.poll()?.run()
            }
        }
    }

    fun setFilter(filter: GPUImageFilter) {
        runOnDraw {
            val oldFilter = this@GPUImageRenderer.filter
            this@GPUImageRenderer.filter = filter
            oldFilter.destroy()
            this@GPUImageRenderer.filter.ifNeedInit()
            GLES20.glUseProgram(this@GPUImageRenderer.filter.glProgId)
            this@GPUImageRenderer.filter.onOutputSizeChanged(outputWidth, outputHeight)
        }
    }

    fun setImageBitmap(bitmap: Bitmap?) {
        if (bitmap == null) return
        runOnDraw {
            glTextureId = OpenGlUtils.loadTexture(bitmap, glTextureId, false)
            imageWidth = bitmap.width
            imageHeight = bitmap.height
            adjustImageScaling()
        }
    }

    fun deleteImage() {
        runOnDraw {
            GLES20.glDeleteTextures(1, intArrayOf(glTextureId), 0)
            glTextureId = NO_IMAGE
        }
    }

    private fun adjustImageScaling() {
        if (imageWidth == 0 || imageHeight == 0) return

        val outputWidth: Float = this.outputWidth.toFloat()
        val outputHeight: Float = this.outputHeight.toFloat()

        val ratio1: Float = outputWidth / imageWidth
        val ratio2: Float = outputHeight / imageHeight
        val ratioMax = ratio1.coerceAtLeast(ratio2)
        val imageWidthNew = (imageWidth * ratioMax).roundToInt()
        val imageHeightNew = (imageHeight * ratioMax).roundToInt()
        val ratioWidth = imageWidthNew / outputWidth
        val ratioHeight = imageHeightNew / outputHeight
        val cube = floatArrayOf(
            vertexCoor[0] / ratioHeight, vertexCoor[1] / ratioWidth,
            vertexCoor[2] / ratioHeight, vertexCoor[3] / ratioWidth,
            vertexCoor[4] / ratioHeight, vertexCoor[5] / ratioWidth,
            vertexCoor[6] / ratioHeight, vertexCoor[7] / ratioWidth
        )
        glCubeBuffer.clear()
        glCubeBuffer.put(cube).position(0)
        glTextureBuffer.clear()
        glTextureBuffer.put(textureCoor).position(0)
    }

    fun runOnDraw(runnable: Runnable) {
        synchronized(runOnDraw) { runOnDraw.add(runnable) }
    }

    fun runOnDrawEnd(runnable: Runnable) {
        synchronized(runOnDrawEnd) { runOnDrawEnd.add(runnable) }
    }

    companion object {
        private const val NO_IMAGE = -1

        val vertexCoor = floatArrayOf(
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f
        )

        val textureCoor = floatArrayOf(
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f
        )
    }
}