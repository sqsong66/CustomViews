package com.sqsong.opengllib.egl

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.util.Log
import android.view.Surface

class EGLHelper(
    surface: Any?,
    flags: Int = FLAG_RECORDABLE,
    private var sharedEGLContext: EGLContext? = null
) {
    private var eglSurface: EGLSurface? = null
    private var eglConfig: EGLConfig? = null
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY

    init {
        if (sharedEGLContext == null) {
            sharedEGLContext = EGL14.EGL_NO_CONTEXT
        }

        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            throw RuntimeException("eglGetDisplay failed")
        }

        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            throw RuntimeException("eglInitialize failed")
        }

        if ((flags and FLAG_TRY_GLES3) != 0) {
            val config = getConfig(flags, 3)
            if (config != null) {
                val attribList = intArrayOf(
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
                    EGL14.EGL_NONE
                )
                val context = EGL14.eglCreateContext(eglDisplay, config, sharedEGLContext, attribList, 0)
                if (EGL14.eglGetError() == EGL14.EGL_SUCCESS) {
                    Log.d("EGLHelper", "Got GLES 3 config")
                    eglConfig = config
                    eglContext = context
                }
            }
        }

        if (eglContext == EGL14.EGL_NO_CONTEXT) {
            val config = getConfig(flags, 2)
            if (config != null) {
                val attribList = intArrayOf(
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                    EGL14.EGL_NONE
                )
                val context = EGL14.eglCreateContext(eglDisplay, config, sharedEGLContext, attribList, 0)
                if (EGL14.eglGetError() == EGL14.EGL_SUCCESS) {
                    Log.d("EGLHelper", "Got GLES 2 config")
                    eglConfig = config
                    eglContext = context
                }
            }
        }

        if (eglContext == EGL14.EGL_NO_CONTEXT) {
            throw RuntimeException("Failed to create EGL context")
        }

        val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
        Log.d("songmao", "EGLHelper: surface: $surface")
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surface, surfaceAttribs, 0)
        if (eglSurface == EGL14.EGL_NO_SURFACE) {
            throw RuntimeException("Failed to create EGL surface")
        }

        makeCurrent()
    }

    fun setPresentationTime(timestamp: Long) {
        EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, timestamp)
    }

    fun makeCurrent(): Boolean {
        val result = EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
        if (!result) {
            throw RuntimeException("eglMakeCurrent failed")
        }
        return true
    }

    private fun getConfig(flags: Int, version: Int): EGLConfig? {
        var renderType = EGL14.EGL_OPENGL_ES2_BIT
        if (version >= 3) {
            renderType = renderType or EGLExt.EGL_OPENGL_ES3_BIT_KHR
        }
        val attribList = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, renderType,
            EGL14.EGL_NONE, 0,
            EGL14.EGL_NONE
        )
        if ((flags and FLAG_RECORDABLE) != 0) {
            attribList[attribList.size - 3] = EGL_RECORDABLE_ANDROID // EGLExt.EGL_RECORDABLE_ANDROID; (required 26)
            attribList[attribList.size - 2] = 1
        }
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(eglDisplay, attribList, 0, configs, 0, configs.size, numConfigs, 0)) {
            Log.w("EGLHelper", "unable to find RGB8888 / $version EGLConfig")
            return null
        }
        return configs[0]
    }

    fun swapBuffers(): Boolean {
        return EGL14.eglSwapBuffers(eglDisplay, eglSurface)
    }

    fun release() {
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(eglDisplay)
        }
        eglSurface = null
        eglConfig = null
        eglContext = EGL14.EGL_NO_CONTEXT
        eglDisplay = EGL14.EGL_NO_DISPLAY
    }

    companion object {
        const val FLAG_RECORDABLE = 0x01
        const val FLAG_TRY_GLES3 = 0x02
        const val EGL_RECORDABLE_ANDROID = 0x3142
    }
}