package com.sqsong.opengllib.filters

import android.content.Context
import android.opengl.GLES30
import android.opengl.Matrix
import android.util.Log
import android.util.Size
import com.sqsong.opengllib.common.FrameBuffer
import com.sqsong.opengllib.common.GLVertexLinker
import com.sqsong.opengllib.common.Program
import com.sqsong.opengllib.common.Texture
import com.sqsong.opengllib.utils.ext.readAssetsText
import java.util.Random

open class MultiTextureFilter(
    private val context: Context,
    private val vertexAssets: String = "shader/no_filter_ver.vert",
    private vararg val fragmentAssets: String = arrayOf(
        /*"shader/frag_windowbinds.glsl",*/
       "shader/frag_glitch_memories.glsl", "shader/frag_water_drop.glsl",
        "shader/frag_polka_dots_curtain.glsl", "shader/frag_grid_flip.glsl",
        "shader/frag_flyeye.glsl", "shader/frag_doorway.glsl",
        "shader/frag_swap.glsl", "shader/frag_bounce.glsl", "shader/frag_cube.glsl",
        "shader/frag_cross_warp.glsl", "shader/frag_hexagonalize.glsl",
        "shader/frag_undulating_burn_out.glsl", "shader/frag_butterfly_wave_scrawler.glsl",
        "shader/frag_swirl.glsl", "shader/frag_glitch_memories.glsl",
        "shader/frag_cube.glsl", "shader/frag_perlin.glsl",
        "shader/frag_mosaic.glsl", "shader/frag_inverted_page_curl.glsl",
        "shader/frag_linear_blur.glsl", "shader/frag_squareswire.glsl",
    ),
    // "shader/frag_stereo_viewer.glsl"
) {

    private var isInitialized = false
    private var viewSize = Size(0, 0)
    private var inputTextureSize = Size(0, 0)
    private val mvpMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val fboPrograms = mutableListOf<Program>()

    private val vertTexCoords = floatArrayOf(
        -1f, 1f, 0.0f, 0.0f, 0.0f,  // top left
        -1f, -1f, 0.0f, 0.0f, 1.0f,  // bottom left
        1f, -1f, 0.0f, 1.0f, 1.0f,  // bottom right
        1f, 1f, 0.0f, 1.0f, 0.0f, // top right
    )

    private val fboVertTexCoords = floatArrayOf(
        -1f, 1f, 0.0f, 0.0f, 0.0f,  // top left
        -1f, -1f, 0.0f, 0.0f, 1.0f,  // bottom left
        1f, -1f, 0.0f, 1.0f, 1.0f,  // bottom right
        1f, 1f, 0.0f, 1.0f, 0.0f, // top right
    )

    private val vertIndices = shortArrayOf(0, 1, 2, 0, 2, 3)

    // 屏幕着色器程序
    private var screenProgram: Program? = null

    // 屏幕着色器顶点数据链接器
    private val screenVertexLinker by lazy {
        GLVertexLinker(vertTexCoords, vertIndices, 5 * 4)
    }

    // 离屏渲染着色器程序
    private var fboProgram: Program? = null

    // 离屏渲染帧缓冲区
    private var frameBuffer: FrameBuffer? = null

    // 离屏渲染顶点数据链接器
    private val fboVertexLinker by lazy {
        GLVertexLinker(fboVertTexCoords, vertIndices, 5 * 4)
    }

    private var programIndex = 0
    private var frameIndex = 0
    private var loopCount = 0
    private val LOOP_COUNT = 200
    private val random by lazy { Random() }

    /**
     * 初始化。主要是在GLSurfaceView.Renderer的onSurfaceCreated()方法中调用,
     * 因为这时OpenGL的环境已创建好。
     */
    fun ifNeedInit() {
        if (!isInitialized) onInit()
    }

    private fun onInit() {
        // 初始化屏幕参数：设置屏幕输出顶点坐标、纹理坐标、变换矩阵、着色器程序等
        initScreenParams()
        // 初始离屏渲染参数
        fboVertexLinker.setupVertices()

        val fboVertexShader = context.readAssetsText(vertexAssets)

        fragmentAssets.forEach { fragmentAsset ->
            val fboFragmentShader = context.readAssetsText(fragmentAsset)
            fboPrograms.add(Program.of(fboVertexShader, fboFragmentShader).apply {
                onInitialized(this)
            })
        }
        fboProgram = fboPrograms.getOrNull(0)
        isInitialized = true
    }

    private fun initScreenParams() {
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
    }

    fun onViewSizeChanged(width: Int, height: Int) {
        viewSize = Size(width, height)
    }

    protected open fun onInitialized(program: Program) {}

    fun onInputTextureLoaded(textureWidth: Int, textureHeight: Int) {
        inputTextureSize = Size(textureWidth, textureHeight)
        frameBuffer?.delete()
        frameBuffer = null
        frameBuffer = FrameBuffer(textureWidth, textureHeight)
    }

    fun onDrawFrame(textures: List<Texture>) {
        if (!isInitialized) return
        frameIndex++
        // 计算gl-transition变化进度
        val progress = (frameIndex % LOOP_COUNT).toFloat() / LOOP_COUNT
        if (frameIndex % LOOP_COUNT == 0) {
            loopCount++
            // 随机切换特效着色器
            programIndex = random.nextInt(fboPrograms.size)
            fboProgram = fboPrograms.getOrNull(programIndex % fboPrograms.size)
        }

        // 开启离屏渲染
        fboProgram?.use()
        frameBuffer?.bindFrameBuffer()
        GLES30.glViewport(0, 0, inputTextureSize.width, inputTextureSize.height)

        // 获取到开始变换的图片纹理，并将其设置给着色器程序
        val inputTexture = textures[loopCount % textures.size]
        fboProgram?.getUniformLocation("uTexture0")?.let {
            inputTexture.bindTexture(it, 0)
        }
        fboProgram?.getUniformLocation("texture0Size")?.let {
            GLES30.glUniform2f(it, inputTexture.textureWidth.toFloat(), inputTexture.textureHeight.toFloat())
        }

        // 获取到目的(下一张)图片纹理，并将其设置给着色器程序
        val nextTexture = textures[(loopCount + 1) % textures.size]
        fboProgram?.getUniformLocation("uTexture1")?.let {
            nextTexture.bindTexture(it, 1)
        }
        fboProgram?.getUniformLocation("texture1Size")?.let {
            GLES30.glUniform2f(it, nextTexture.textureWidth.toFloat(), nextTexture.textureHeight.toFloat())
        }

        // 设置gl-transition变换进度
        fboProgram?.getUniformLocation("progress")?.let {
            GLES30.glUniform1f(it, progress)
        }

        // 设置纹理尺寸
        fboProgram?.getUniformLocation("resolution")?.let {
            GLES30.glUniform2f(it, inputTextureSize.width.toFloat(), inputTextureSize.height.toFloat())
        }

        // 设置gl-transition变换时长占图片显示总时长的百分比
        fboProgram?.getUniformLocation("textureStayRatio")?.let {
            GLES30.glUniform1f(it, 0.4f)
        }

        // 渲染纹理
        fboVertexLinker.draw()
        inputTexture.unbindTexture()
        nextTexture.unbindTexture()
        frameBuffer?.unbindFrameBuffer()

        // 绘制到屏幕上
        drawTextureOnScreen(frameBuffer?.texture)
    }

    private fun drawTextureOnScreen(texture: Texture?) {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, viewSize.width, viewSize.height)
        screenProgram?.use()
        onBindScreenTexture(texture, inputTextureSize.width, inputTextureSize.height)
        screenVertexLinker.draw()
    }

    private fun onBindScreenTexture(texture: Texture?, width: Int, height: Int) {
        // Log.d("BaseImageFilter", "$TAG onBindScreenTexture, viewWidth: $viewWidth, viewHeight: $viewHeight")
        // 进行视图变换将纹理正确的显示在屏幕(GLSurfaceView)上(根据纹理的宽高以及控件的宽高比来计算模型矩阵)
        val textureAspectRatio = width.toFloat() / height.toFloat()
        val viewAspectRatio = viewSize.width.toFloat() / viewSize.height.toFloat()
        val destWidth: Int
        val destHeight: Int
        if (textureAspectRatio > viewAspectRatio) {
            destWidth = viewSize.width
            destHeight = (viewSize.width.toFloat() / textureAspectRatio).toInt()
        } else {
            destHeight = viewSize.height
            destWidth = (viewSize.height.toFloat() * textureAspectRatio).toInt()
        }
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.scaleM(modelMatrix, 0, destWidth / viewSize.width.toFloat(), destHeight / viewSize.height.toFloat(), 1.0f)
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        screenProgram?.getUniformLocation("uMvpMatrix")?.let {
            GLES30.glUniformMatrix4fv(it, 1, false, mvpMatrix, 0)
        }

        // 绑定离屏渲染的纹理，显示到屏幕上
        screenProgram?.getUniformLocation("uTexture")?.let {
            texture?.textureId?.let { textureId ->
                GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
                GLES30.glUniform1i(it, 0)
            }
        }
    }

    fun getFrameBufferTexture(): Texture? {
        return frameBuffer?.texture
    }

    open fun onDestroy() {
        screenProgram?.delete()
        screenProgram = null
        fboProgram?.delete()
        fboProgram = null
        fboPrograms.forEach { it.delete() }
        fboPrograms.clear()
        frameBuffer?.delete()
        frameBuffer = null
        screenVertexLinker.cleanup()
        fboVertexLinker.cleanup()
        isInitialized = false
    }
}