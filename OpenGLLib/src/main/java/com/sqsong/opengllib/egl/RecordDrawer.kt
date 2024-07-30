package com.sqsong.opengllib.egl

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLES30
import android.util.Log
import com.sqsong.opengllib.common.GLVertexLinker
import com.sqsong.opengllib.common.Program
import com.sqsong.opengllib.utils.ext.readAssetsText

class RecordDrawer(
    private val context: Context,
    private val vertexAssets: String = "shader/no_filter_ver.vert",
    private val fragmentAssets: String = "shader/no_filter_frag.frag",
) {

    private val TAG = this.javaClass.simpleName

    private var isInitialized = false
    private val vertTexCoords = floatArrayOf(
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

    /**
     * 初始化。主要是在GLSurfaceView.Renderer的onSurfaceCreated()方法中调用,
     * 因为这是OpenGL的环境已创建好。
     */
    fun ifNeedInit() {
        if (!isInitialized) onInit()
    }

    private fun onInit() {
        initScreenParams()
        isInitialized = true
    }

    private fun initScreenParams() {
        screenVertexLinker.setupVertices()
        val vertexShader = context.readAssetsText(vertexAssets)
        val fragmentShader = context.readAssetsText(fragmentAssets)
        screenProgram = Program.of(vertexShader, fragmentShader)
    }

    fun onDrawFrame(textureId: Int) {
        checkGlError("onDrawFrame start")
        screenProgram?.use()
        checkGlError("onDrawFrame use program")
        onBindScreenTexture(textureId)
        checkGlError("onDrawFrame bind texture")
        screenVertexLinker.draw()
        checkGlError("onDrawFrame draw")
        GLES30.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        checkGlError("onDrawFrame unbind texture")
        screenProgram?.unUse()
    }

    private fun onBindScreenTexture(textureId: Int) {
        // 绑定离谱渲染的纹理，显示到屏幕上
        screenProgram?.getUniformLocation("uTexture")?.let {
            Log.i("sqsong", "$TAG onBindScreenTexture, uTexture location: $it, textureId: $textureId")
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
            GLES30.glUniform1i(it, 0)
        }
    }

    fun onDestroy() {
        screenProgram?.delete()
        screenProgram = null
        screenVertexLinker.cleanup()
        isInitialized = false
    }
}