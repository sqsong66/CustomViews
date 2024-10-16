package com.example.customviews.data

import android.graphics.Bitmap

data class ImageFilterData(
    val filterName: String,
    val filterLutAssets: List<String>? = null,
    val filterFragShaderPath: String,
    var intensity: Float = 1.0f,
    val previewBitmap: Bitmap? = null,
    val clear: Boolean = false
)
