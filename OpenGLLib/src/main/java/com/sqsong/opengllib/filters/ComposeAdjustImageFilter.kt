package com.sqsong.opengllib.filters

import android.content.Context

class ComposeAdjustImageFilter(
    context: Context,
) : GroupImageFilter(context) {

    private val brightnessImageFilter by lazy {
        BrightnessImageFilter(context, initOutputBuffer = false)
    }

    init {
        addFilter(brightnessImageFilter)
    }

    override fun setProgress(progress: Float, extraType: Int) {
        when (extraType) {
            FilterMode.FILTER_BRIGHTNESS -> {
                brightnessImageFilter.setProgress(progress, extraType)
            }
        }
    }
}