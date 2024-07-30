package com.example.customviews.view.anno

import androidx.annotation.IntDef

@IntDef(
    GifQuality.STANDARD, GifQuality.HIGH, GifQuality.ULTRA_HIGH
)
@Retention(AnnotationRetention.SOURCE)
annotation class GifQuality {
    companion object {
        const val STANDARD = 0
        const val HIGH = 1
        const val ULTRA_HIGH = 2
    }
}
