package com.sqsong.opengllib.filters

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.Matrix
import android.util.Log
import com.sqsong.opengllib.common.BitmapTexture
import com.sqsong.opengllib.common.FrameBuffer
import com.sqsong.opengllib.common.GLVertexLinker
import com.sqsong.opengllib.common.Program
import com.sqsong.opengllib.common.Texture
import com.sqsong.opengllib.utils.ext.readAssetsText

open class BaseImageFilter(
    private val context: Context,
    private val vertexAssets: String = "shader/no_filter_ver.vert",
    private val fragmentAssets: String = "shader/no_filter_frag.frag"
) {

    private var viewWidth = 0
    private var viewHeight = 0
    private var isInitialized = false
    private var inputTexture: Texture? = null
    private val mvpMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)

    private val vertTexCoords = floatArrayOf(
        -1f, 1f, 0.0f, 0.0f, 0.0f,  // top left
        -1f, -1f, 0.0f, 0.0f, 1.0f,  // bottom left
        1f, -1f, 0.0f, 1.0f, 1.0f,  // bottom right
        1f, 1f, 0.0f, 1.0f, 0.0f, // top right
    )

    private val fboVertTexCoords = floatArrayOf(
        -1f, 1f, 0.0f, 0.0f, 1.0f,  // top left
        -1f, -1f, 0.0f, 0.0f, 0.0f,  // bottom left
        1f, -1f, 0.0f, 1.0f, 0.0f,  // bottom right
        1f, 1f, 0.0f, 1.0f, 1.0f, // top right
    )

    private val vertIndices = shortArrayOf(0, 1, 2, 0, 2, 3)

    private var screenProgram: Program? = null
    private val screenVertexLinker by lazy {
        GLVertexLinker(vertTexCoords, vertIndices, 5 * 4)
    }

    private var fboProgram: Program? = null
    private var frameBuffer: FrameBuffer? = null
    private val fboVertexLinker by lazy {
        GLVertexLinker(fboVertTexCoords, vertIndices, 5 * 4)
    }

    fun ifNeedInit() {
        if (!isInitialized) onInit()
    }

    private fun onInit() {
        // Initialize matrices
        Matrix.setIdentityM(viewMatrix, 0)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.setIdentityM(projectionMatrix, 0)
        Matrix.orthoM(projectionMatrix, 0, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, 1.0f)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f)

        screenVertexLinker.setupVertices()
        val vertexShader = context.readAssetsText("shader/base_filter_ver.vert")
        val fragmentShader = context.readAssetsText("shader/base_filter_frag.frag")
        screenProgram = Program.of(vertexShader, fragmentShader)

        fboVertexLinker.setupVertices()
        val fboVertexShader = context.readAssetsText(vertexAssets)
        val fboFragmentShader = context.readAssetsText(fragmentAssets)
        fboProgram = Program.of(fboVertexShader, fboFragmentShader).apply {
            onInitialized(this)
        }
        Log.d("BaseImageFilter", "onInit, fboProgram: $fboProgram, fboFragmentShader:\n$fboFragmentShader")
        isInitialized = true
    }

    protected open fun onInitialized(program: Program) {}

    fun onViewSizeChanged(width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
    }

    open fun setProgress(progress: Float) {}

    fun setImageBitmap(bitmap: Bitmap) {
        inputTexture?.delete()
        inputTexture = null
        inputTexture = BitmapTexture(bitmap)
        frameBuffer?.delete()
        frameBuffer = null
        frameBuffer = FrameBuffer(bitmap.width, bitmap.height)
        onInputTextureLoaded(bitmap.width, bitmap.height)
    }

    protected open fun onInputTextureLoaded(textWidth: Int, textHeight: Int) {}

    fun onDrawFrame() {
        Log.w("BaseImageFilter", "onDrawFrame, thread: ${Thread.currentThread().name}")
        val drawTexture = inputTexture ?: return
        fboProgram?.use()

        onBeforeFrameBufferDraw(drawTexture, fboProgram, fboVertexLinker)?.let { processTexture ->
            frameBuffer?.bindFrameBuffer()
            GLES30.glViewport(0, 0, processTexture.textureWidth, processTexture.textureHeight)
            onBindTexture(processTexture)
            fboVertexLinker.draw()
            processTexture.unbindTexture()
            onAfterDraw()
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, viewWidth, viewHeight)

        screenProgram?.use()
        onBindScreenTexture(drawTexture.textureWidth, drawTexture.textureHeight)
        screenVertexLinker.draw()
    }

    protected open fun onBeforeFrameBufferDraw(inputTexture: Texture, fboProgram: Program?, defaultFboGLVertexLinker: GLVertexLinker): Texture? {
        return inputTexture
    }

    private fun onBindTexture(texture: Texture) {
        fboProgram?.getUniformLocation("uTexture")?.let {
            Log.d("BaseImageFilter", "onBindTexture, viewWidth: $viewWidth, viewHeight: $viewHeight, textureId: ${texture.textureId}, uTexture location: $it")
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            texture.bindTexture()
            GLES30.glUniform1i(it, 0)
        }
        fboProgram?.let { onPreDraw(it) }
    }

    protected open fun onPreDraw(program: Program) {}

    protected open fun onAfterDraw() {}

    private fun onBindScreenTexture(width: Int, height: Int) {
        val textureAspectRatio = width.toFloat() / height.toFloat()
        val viewAspectRatio = viewWidth.toFloat() / viewHeight.toFloat()
        val destWidth: Int
        val destHeight: Int
        if (textureAspectRatio > viewAspectRatio) {
            destWidth = viewWidth
            destHeight = (viewWidth.toFloat() / textureAspectRatio).toInt()
        } else {
            destHeight = viewHeight
            destWidth = (viewHeight.toFloat() * textureAspectRatio).toInt()
        }
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.scaleM(modelMatrix, 0, destWidth / viewWidth.toFloat(), destHeight / viewHeight.toFloat(), 1.0f)
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        screenProgram?.getUniformLocation("uMvpMatrix")?.let {
            Log.i("BaseImageFilter", "onBindScreenTexture, uMvpMatrix location: $it")
            GLES30.glUniformMatrix4fv(it, 1, false, mvpMatrix, 0)
        }

        screenProgram?.getUniformLocation("uTexture")?.let {
            frameBuffer?.texture?.textureId?.let { textureId ->
                Log.i("BaseImageFilter", "onBindScreenTexture, uTexture location: $it, textureId: $textureId")
                GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
                GLES30.glUniform1i(it, 0)
            }
        }
    }

    fun getRenderedBitmap(): Bitmap? {
        return frameBuffer?.getRenderedBitmap()
    }

    open fun onDestroy() {
        screenProgram?.delete()
        screenProgram = null
        fboProgram?.delete()
        fboProgram = null
        inputTexture?.delete()
        inputTexture = null
        screenVertexLinker.cleanup()
        fboVertexLinker.cleanup()
        isInitialized = false
    }
}