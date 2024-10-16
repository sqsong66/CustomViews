package com.wangxutech.picwish.libnative.beauty.filter

import android.content.Context
import android.opengl.GLES20
import com.wangxutech.picwish.libnative.beauty.GPUImageRenderer
import com.wangxutech.picwish.libnative.utils.Rotation
import com.wangxutech.picwish.libnative.utils.TextureRotationUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class GPUImageGroupFilter(context: Context, private val filters: List<GPUImageFilter> = mutableListOf()) :
    GPUImageFilter(context) {

    private var frameBuffers: IntArray? = null
    private var frameBufferTextures: IntArray? = null
    private var mergedFilters = mutableListOf<GPUImageFilter>()

    private val glCubeBuffer: FloatBuffer by lazy {
        ByteBuffer.allocateDirect(GPUImageRenderer.vertexCoor.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(GPUImageRenderer.vertexCoor)
                position(0)
            }
    }

    private val glTextureBuffer: FloatBuffer by lazy {
        ByteBuffer.allocateDirect(GPUImageRenderer.textureCoor.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(GPUImageRenderer.textureCoor)
                position(0)
            }
    }

    private val glTextureFlipBuffer: FloatBuffer by lazy {
        val flipTexture: FloatArray = TextureRotationUtil.getRotation(Rotation.NORMAL, flipHorizontal = false, flipVertical = true)
        ByteBuffer.allocateDirect(flipTexture.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(flipTexture)
                position(0)
            }
    }

    init {
        updateMergedFilters()
    }

    override fun onInit() {
        super.onInit()
        for (filter in filters) {
            filter.ifNeedInit()
        }
    }

    override fun onDestroy() {
        destroyFrameBuffers()
        for (filter in filters) {
            filter.destroy()
        }
        super.onDestroy()
    }

    override fun onOutputSizeChanged(width: Int, height: Int) {
        super.onOutputSizeChanged(width, height)
        if (frameBuffers != null) {
            destroyFrameBuffers()
        }
        var size = filters.size
        for (i in 0 until size) {
            filters[i].onOutputSizeChanged(width, height)
        }
        if (mergedFilters.size > 0) {
            size = mergedFilters.size
            frameBuffers = IntArray(size - 1)
            frameBufferTextures = IntArray(size - 1)
            for (i in 0 until size - 1) {
                GLES20.glGenFramebuffers(1, frameBuffers, i)
                GLES20.glGenTextures(1, frameBufferTextures, i)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameBufferTextures!![i])
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE.toFloat())
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE.toFloat())
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers!![i])
                GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, frameBufferTextures!![i], 0)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
            }
        }
    }

    private fun destroyFrameBuffers() {
        if (frameBufferTextures != null) {
            GLES20.glDeleteTextures(frameBufferTextures!!.size, frameBufferTextures, 0)
            frameBufferTextures = null
        }
        if (frameBuffers != null) {
            GLES20.glDeleteFramebuffers(frameBuffers!!.size, frameBuffers, 0)
            frameBuffers = null
        }
    }

    override fun onDraw(textureId: Int, cubeBuffer: FloatBuffer, textureBuffer: FloatBuffer) {
        runPendingOnDrawTasks()
        if (!isInitialized || frameBuffers == null || frameBufferTextures == null) {
            return
        }
        val size = mergedFilters.size
        var previousTexture = textureId
        for (i in 0 until size) {
            val filter = mergedFilters[i]
            val isNotLast = i < size - 1
            if (isNotLast) {
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers!![i])
                // GLES20.glClearColor(247f / 255f, 248f / 255f, 250f / 255f, 1.0f)
                GLES20.glClearColor(0f, 0f, 0f, 0f)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
            }
            when (i) {
                0 -> {
                    filter.onDraw(previousTexture, cubeBuffer, textureBuffer)
                }
                size - 1 -> {
                    filter.onDraw(previousTexture, glCubeBuffer, if (size % 2 == 0) glTextureFlipBuffer else glTextureBuffer)
                }
                else -> {
                    filter.onDraw(previousTexture, glCubeBuffer, glTextureBuffer)
                }
            }
            if (isNotLast) {
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
                previousTexture = frameBufferTextures!![i]
            }
        }
    }

    fun setBeautyWhite(percent: Float) { // 0f~1f
        (mergedFilters.find { it is GPUImageBeautyFilter } as? GPUImageBeautyFilter)?.setBrightnessLevel(percent)
    }

    fun setBeautyDerma(percent: Float) { // 0f~1f
        (mergedFilters.find { it is GPUImageBeautyFilter } as? GPUImageBeautyFilter)?.setBeautyLevel(percent)
    }

    fun setBrightness(percent: Float) { // -1f~1f
        (mergedFilters.find { it is GPUImageBrightnessFilter } as? GPUImageBrightnessFilter)?.setBrightnessLevel(percent)
    }

    fun setSaturation(percent: Float) { // -1f~1f
        (mergedFilters.find { it is GPUImageSaturationFilter } as? GPUImageSaturationFilter)?.setSaturationLevel(percent)
    }

    fun getMergedFilters(): List<GPUImageFilter> {
        return mergedFilters
    }

    fun updateMergedFilters() {
        mergedFilters.clear()
        var filters: List<GPUImageFilter?>
        for (filter in this.filters) {
            if (filter is GPUImageGroupFilter) {
                filter.updateMergedFilters()
                filters = filter.getMergedFilters()
                if (filters.isEmpty()) continue
                mergedFilters.addAll(filters)
                continue
            }
            mergedFilters.add(filter)
        }
    }

}