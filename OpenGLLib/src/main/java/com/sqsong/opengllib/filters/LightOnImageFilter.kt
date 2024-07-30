package com.sqsong.opengllib.filters

import android.content.Context
import android.opengl.GLES30
import com.sqsong.opengllib.common.Program
import com.sqsong.opengllib.common.Texture

class LightOnImageFilter(
    context: Context,
    initOutputBuffer: Boolean = false
) : BaseImageFilter(context, fragmentAssets = "shader/frag_light_on.glsl", initOutputBuffer = initOutputBuffer) {


}