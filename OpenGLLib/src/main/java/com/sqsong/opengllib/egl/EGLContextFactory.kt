package com.sqsong.opengllib.egl

import android.util.Log
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay

interface EGLContextFactory {
    fun createContext(egl: EGL10, display: EGLDisplay, eglConfig: EGLConfig): EGLContext
    fun destroyContext(egl: EGL10, display: EGLDisplay, context: EGLContext)
}

class DefaultContextFactory(private val eglContextClientVersion: Int) : EGLContextFactory {

    override fun createContext(egl: EGL10, display: EGLDisplay, eglConfig: EGLConfig): EGLContext {
        val attribList = intArrayOf(
            EGL_CONTEXT_CLIENT_VERSION, eglContextClientVersion,
            EGL10.EGL_NONE
        )

        return egl.eglCreateContext(
            display, eglConfig, EGL10.EGL_NO_CONTEXT,
            if (eglContextClientVersion != 0) attribList else null
        )
    }

    override fun destroyContext(egl: EGL10, display: EGLDisplay, context: EGLContext) {
        if (!egl.eglDestroyContext(display, context)) {
            Log.e("DefaultContextFactory", "display: $display context: $context")
            Log.i("DefaultContextFactory", "tid=" + Thread.currentThread().id)
            throwEglException("eglDestroyContext", egl.eglGetError())
        }
    }

    companion object {
        private const val EGL_CONTEXT_CLIENT_VERSION = 0x3098
    }

}