package com.sqsong.opengllib.egl

import android.opengl.EGL14
import android.opengl.EGLExt
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay

interface EGLConfigChooser {
    fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig
}

abstract class BaseConfigChooser(
    private val eglContextClientVersion: Int = 2,
    configSpec: IntArray
) : EGLConfigChooser {

    private val mConfigSpec: IntArray = filterConfigSpec(configSpec)

    private fun filterConfigSpec(configSpec: IntArray): IntArray {
        if (eglContextClientVersion != 2 && eglContextClientVersion != 3) {
            return configSpec
        }
        /* We know none of the subclasses define EGL_RENDERABLE_TYPE.
         * And we know the configSpec is well formed.
         */
        val len = configSpec.size
        val newConfigSpec = IntArray(len + 2)
        System.arraycopy(configSpec, 0, newConfigSpec, 0, len - 1)
        newConfigSpec[len - 1] = EGL10.EGL_RENDERABLE_TYPE
        if (eglContextClientVersion == 2) {
            newConfigSpec[len] = EGL14.EGL_OPENGL_ES2_BIT /* EGL_OPENGL_ES2_BIT */
        } else {
            newConfigSpec[len] = EGLExt.EGL_OPENGL_ES3_BIT_KHR /* EGL_OPENGL_ES3_BIT_KHR */
        }
        newConfigSpec[len + 1] = EGL10.EGL_NONE
        return newConfigSpec
    }

    override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig {
        val numConfig = IntArray(1)
        require(egl.eglChooseConfig(display, mConfigSpec, null, 0, numConfig)) { "eglChooseConfig failed" }
        val numConfigs = numConfig[0]
        require(numConfigs > 0) { "No configs match configSpec" }
        val configs = arrayOfNulls<EGLConfig>(numConfigs)
        require(egl.eglChooseConfig(display, mConfigSpec, configs, numConfigs, numConfig)) { "eglChooseConfig#2 failed" }
        val config = chooseConfig(egl, display, configs) ?: throw IllegalArgumentException("No config chosen")
        return config
    }

    abstract fun chooseConfig(egl: EGL10, display: EGLDisplay, configs: Array<EGLConfig?>): EGLConfig?
}

open class ComponentSizeChooser(
    eglContextClientVersion: Int = 2,
    private val redSize: Int,
    private val greenSize: Int,
    private val blueSize: Int,
    private val alphaSize: Int,
    private val depthSize: Int,
    private val stencilSize: Int
) : BaseConfigChooser(
    eglContextClientVersion, intArrayOf(
        EGL10.EGL_RED_SIZE, redSize,
        EGL10.EGL_GREEN_SIZE, greenSize,
        EGL10.EGL_BLUE_SIZE, blueSize,
        EGL10.EGL_ALPHA_SIZE, alphaSize,
        EGL10.EGL_DEPTH_SIZE, depthSize,
        EGL10.EGL_STENCIL_SIZE, stencilSize,
        EGL10.EGL_NONE
    )
) {
    private val mValue = IntArray(1)

    override fun chooseConfig(egl: EGL10, display: EGLDisplay, configs: Array<EGLConfig?>): EGLConfig? {
        for (config in configs) {
            val d: Int = findConfigAttrib(
                egl, display, config,
                EGL10.EGL_DEPTH_SIZE
            )
            val s: Int = findConfigAttrib(
                egl, display, config,
                EGL10.EGL_STENCIL_SIZE
            )
            if ((d >= depthSize) && (s >= stencilSize)) {
                val r: Int = findConfigAttrib(egl, display, config, EGL10.EGL_RED_SIZE)
                val g: Int = findConfigAttrib(egl, display, config, EGL10.EGL_GREEN_SIZE)
                val b: Int = findConfigAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE)
                val a: Int = findConfigAttrib(egl, display, config, EGL10.EGL_ALPHA_SIZE)
                if ((r == redSize) && (g == greenSize) && (b == blueSize) && (a == alphaSize)) {
                    return config
                }
            }
        }
        return null
    }

    private fun findConfigAttrib(egl: EGL10?, display: EGLDisplay?, config: EGLConfig?, attribute: Int): Int {
        if (egl?.eglGetConfigAttrib(display, config, attribute, mValue) == true) {
            return mValue[0]
        }
        return 0
    }
}

class SimpleEGLConfigChooser(
    eglContextClientVersion: Int = 2,
    withDepthBuffer: Boolean = true,
) : ComponentSizeChooser(eglContextClientVersion, 8, 8, 8, 0, if (withDepthBuffer) 16 else 0, 0)