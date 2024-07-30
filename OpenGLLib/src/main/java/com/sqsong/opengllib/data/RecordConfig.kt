package com.sqsong.opengllib.data

import android.content.Context
import android.opengl.EGLContext

data class RecordConfig(
    val context: Context,
    val videoPath: String,
    val videoWidth: Int,
    val videoHeight: Int,
    val sharedEGLContext: EGLContext? = null
)
