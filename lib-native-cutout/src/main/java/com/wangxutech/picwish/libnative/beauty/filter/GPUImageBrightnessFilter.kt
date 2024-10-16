/*
 * Copyright (C) 2018 CyberAgent, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wangxutech.picwish.libnative.beauty.filter

import android.content.Context
import android.opengl.GLES20
import android.util.Log
import com.wangxutech.picwish.libnative.R
import com.wangxutech.picwish.libnative.utils.OpenGlUtils

/**
 * brightness value ranges from -1.0 to 1.0, with 0.0 as the normal level
 */
class GPUImageBrightnessFilter @JvmOverloads constructor(context: Context, private var brightness: Float = 0.0f) :
    GPUImageFilter(
        context, OpenGlUtils.readGlShader(context, R.raw.vertex_no_filter),
        OpenGlUtils.readGlShader(context, R.raw.fragment_brightness)
    ) {

    private var brightnessLocation = 0 // -1~1

    override fun onInit() {
        super.onInit()
        brightnessLocation = GLES20.glGetUniformLocation(glProgId, "brightness")
    }

    public override fun onInitialized() {
        super.onInitialized()
        setBrightnessLevel(brightness)
    }

    override fun onDrawArraysPre() {
        super.onDrawArraysPre()
        GLES20.glUniform1f(brightnessLocation, brightness)
    }

    fun setBrightnessLevel(brightness: Float) { // -1~1
        this.brightness = brightness
    }

}