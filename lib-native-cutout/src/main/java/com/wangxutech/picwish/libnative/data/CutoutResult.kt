package com.wangxutech.picwish.libnative.data

import android.graphics.Bitmap
import android.graphics.Rect

data class CutoutResult(
    var cutoutBitmap: Bitmap,
    val srcRect: Rect? = null,
    val cutoutRect: Rect = Rect(),
    var maskBitmap: Bitmap? = null, // mask图
    var srcBitmap: Bitmap? = null, // 缓存原图，在手动抠图的时候方便快速展示
)
