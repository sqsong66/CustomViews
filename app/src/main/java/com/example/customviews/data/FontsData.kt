package com.example.customviews.data

import android.graphics.Typeface
import java.util.UUID

data class FontsData(
    val id: String = UUID.randomUUID().toString(),
    val fontName: String,
    val previewText: String = "Preview Text",
    val isDownloading: Boolean = true,
    var typeface: Typeface? = null
)
