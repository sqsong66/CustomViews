package com.sqsong.opengllib.egl

import android.util.Log
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.egl.EGLSurface

interface EGLWindowSurfaceFactory {
    fun createWindowSurface(egl: EGL10, display: EGLDisplay, config: EGLConfig, nativeWindow: Any): EGLSurface?

    fun destroySurface(egl: EGL10, display: EGLDisplay, surface: EGLSurface)
}

class DefaultWindowSurfaceFactory : EGLWindowSurfaceFactory {
    override fun createWindowSurface(egl: EGL10, display: EGLDisplay, config: EGLConfig, nativeWindow: Any): EGLSurface? {
        return try {
            egl.eglCreateWindowSurface(display, config, nativeWindow, null)
        } catch (e: IllegalArgumentException) {
            Log.e("DefaultWindowSurfaceFactory", "eglCreateWindowSurface", e)
            null
        }
    }

    override fun destroySurface(egl: EGL10, display: EGLDisplay, surface: EGLSurface) {
        egl.eglDestroySurface(display, surface)
    }
}