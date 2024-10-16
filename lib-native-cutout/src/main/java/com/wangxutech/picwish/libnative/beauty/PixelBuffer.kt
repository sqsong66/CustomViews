/*
 * Copyright (C) 2018 CyberAgent, Inc.
 * Copyright (C) 2010 jsemler
 *
 * Original publication without License
 * http://www.anddev.org/android-2d-3d-graphics-opengl-tutorials-f2/possible-to-do-opengl-off-screen-rendering-in-android-t13232.html#p41662
 */
package com.wangxutech.picwish.libnative.beauty

import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import android.util.Log
import com.wangxutech.picwish.libnative.NativeLib
import javax.microedition.khronos.egl.*
import javax.microedition.khronos.opengles.GL10

class PixelBuffer(private val width: Int, private val height: Int) {

    private val gl10: GL10
    private val egl10: EGL10
    private val eglConfig: EGLConfig?
    private val mThreadOwner: String
    private val eglContext: EGLContext
    private val eglDisplay: EGLDisplay
    private val eglSurface: EGLSurface
    private var renderer: GLSurfaceView.Renderer? = null

    init {
        val version = IntArray(2)
        val attribList = intArrayOf(
            EGL10.EGL_WIDTH, width,
            EGL10.EGL_HEIGHT, height,
            EGL10.EGL_NONE
        )

        // No error checking performed, minimum required code to elucidate logic
        egl10 = EGLContext.getEGL() as EGL10
        eglDisplay = egl10.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
        egl10.eglInitialize(eglDisplay, version)
        eglConfig = chooseConfig() // Choosing a config is a little more

        val eglContextClientVersion = 0x3098
        val newAttribList = intArrayOf(
            eglContextClientVersion, 2,
            EGL10.EGL_NONE
        )
        eglContext = egl10.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, newAttribList)
        eglSurface = egl10.eglCreatePbufferSurface(eglDisplay, eglConfig, attribList)
        egl10.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
        gl10 = eglContext.gl as GL10

        // Record thread owner of OpenGL context
        mThreadOwner = Thread.currentThread().name
    }

    fun setRenderer(renderer: GLSurfaceView.Renderer?) {
        this.renderer = renderer
        // Does this thread own the OpenGL context?
        if (Thread.currentThread().name != mThreadOwner) {
            Log.e(TAG, "setRenderer: This thread does not own the OpenGL context.")
            return
        }

        // Call the renderer initialization routines
        this.renderer?.onSurfaceCreated(gl10, eglConfig)
        this.renderer?.onSurfaceChanged(gl10, width, height)
    }

    fun getBitmap(): Bitmap? {
        if (renderer == null) {
            Log.e(TAG, "getBitmap: Renderer was not set.")
            return null
        }
        // Does this thread own the OpenGL context?
        if (Thread.currentThread().name != mThreadOwner) {
            Log.e(TAG, "getBitmap: This thread does not own the OpenGL context.")
            return null
        }
        // Call the renderer draw routine (it seems that some filters do not
        // work if this is only called once)
        renderer?.onDrawFrame(gl10)
        renderer?.onDrawFrame(gl10)
        // val destBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        // NativeLib.adjustBitmap(destBitmap)
        return try {
            NativeLib.getGlBitmap(width, height)
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        } catch (error: Error) {
            error.printStackTrace()
            null
        }
    }

    fun destroy() {
        renderer?.onDrawFrame(gl10)
        renderer?.onDrawFrame(gl10)
        egl10.eglMakeCurrent(
            eglDisplay, EGL10.EGL_NO_SURFACE,
            EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT
        )
        egl10.eglDestroySurface(eglDisplay, eglSurface)
        egl10.eglDestroyContext(eglDisplay, eglContext)
        egl10.eglTerminate(eglDisplay)
    }

    private fun chooseConfig(): EGLConfig? {
        val attribList = intArrayOf(
            EGL10.EGL_DEPTH_SIZE, 0,
            EGL10.EGL_STENCIL_SIZE, 0,
            EGL10.EGL_RED_SIZE, 8,
            EGL10.EGL_GREEN_SIZE, 8,
            EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_ALPHA_SIZE, 8,
            EGL10.EGL_RENDERABLE_TYPE, 4,
            EGL10.EGL_NONE
        )

        // No error checking performed, minimum required code to elucidate logic
        // Expand on this logic to be more selective in choosing a configuration
        val numConfig = IntArray(1)
        egl10.eglChooseConfig(eglDisplay, attribList, null, 0, numConfig)
        val configSize: Int = numConfig[0]
        val eglConfigs = arrayOfNulls<EGLConfig>(configSize)
        egl10.eglChooseConfig(eglDisplay, attribList, eglConfigs, configSize, numConfig)
        if (LIST_CONFIGS) {
            listConfig(eglConfigs)
        }
        return eglConfigs[0] // Best match is probably the first configuration
    }

    private fun listConfig(eglConfigs: Array<EGLConfig?>) {
        Log.i(TAG, "Config List {")
        for (config in eglConfigs) {
            // Expand on this logic to dump other attributes
            val d: Int = getConfigAttrib(config, EGL10.EGL_DEPTH_SIZE)
            val s: Int = getConfigAttrib(config, EGL10.EGL_STENCIL_SIZE)
            val r: Int = getConfigAttrib(config, EGL10.EGL_RED_SIZE)
            val g: Int = getConfigAttrib(config, EGL10.EGL_GREEN_SIZE)
            val b: Int = getConfigAttrib(config, EGL10.EGL_BLUE_SIZE)
            val a: Int = getConfigAttrib(config, EGL10.EGL_ALPHA_SIZE)
            Log.i(TAG, "    <d,s,r,g,b,a> = <$d,$s,$r,$g,$b,$a>")
        }
        Log.i(TAG, "}")
    }

    private fun getConfigAttrib(config: EGLConfig?, attribute: Int): Int {
        val value = IntArray(1)
        return if (egl10.eglGetConfigAttrib(
                eglDisplay, config,
                attribute, value
            )
        ) value[0] else 0
    }

    companion object {
        private const val TAG = "PixelBuffer"
        private const val LIST_CONFIGS = false
    }

}