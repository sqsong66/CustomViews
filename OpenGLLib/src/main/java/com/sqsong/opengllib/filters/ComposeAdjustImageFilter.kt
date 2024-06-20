package com.sqsong.opengllib.filters

import android.content.Context

class ComposeAdjustImageFilter(
    context: Context,
) : GroupImageFilter(context) {

    private val brightnessImageFilter by lazy {
        BrightnessImageFilter(context, initOutputBuffer = false)
    }

    private val contrastImageFilter by lazy {
        ContrastImageFilter(context, initOutputBuffer = false)
    }

    private val saturationImageFilter by lazy {
        SaturationImageFilter(context, initOutputBuffer = false)
    }

    private val exposureImageFilter by lazy {
        ExposureImageFilter(context, initOutputBuffer = false)
    }

    private val highlightImageFilter by lazy {
        HighlightImageFilter(context, initOutputBuffer = false)
    }

    private val shadowImageFilter by lazy {
        ShadowImageFilter(context, initOutputBuffer = false)
    }

    private val temperatureImageFilter by lazy {
        ColorTemperatureImageFilter(context, initOutputBuffer = false)
    }

    private val sharpenImageFilter by lazy {
        SharpenImageFilter(context, initOutputBuffer = false)
    }

    private val vignetteImageFilter by lazy {
        VignetteImageFilter(context, initOutputBuffer = false)
    }

    init {
        addFilter(brightnessImageFilter)
        addFilter(contrastImageFilter)
        addFilter(saturationImageFilter)
        addFilter(exposureImageFilter)
        addFilter(highlightImageFilter)
        addFilter(shadowImageFilter)
        addFilter(temperatureImageFilter)
        addFilter(sharpenImageFilter)
        addFilter(vignetteImageFilter)
    }

    override fun setProgress(progress: Float, extraType: Int) {
        when (extraType) {
            FilterMode.FILTER_BRIGHTNESS -> {
                brightnessImageFilter.setProgress(progress, extraType)
            }
            FilterMode.FILTER_CONTRAST -> {
                contrastImageFilter.setProgress(progress, extraType)
            }
            FilterMode.FILTER_SATURATION -> {
                saturationImageFilter.setProgress(progress, extraType)
            }
            FilterMode.FILTER_EXPOSURE -> {
                exposureImageFilter.setProgress(progress, extraType)
            }
            FilterMode.FILTER_HIGHLIGHT -> {
                highlightImageFilter.setProgress(progress, extraType)
            }
            FilterMode.FILTER_SHADOW -> {
                shadowImageFilter.setProgress(progress, extraType)
            }
            FilterMode.FILTER_TEMPERATURE -> {
                temperatureImageFilter.setProgress(progress, extraType)
            }
            FilterMode.FILTER_SHARPNESS -> {
                sharpenImageFilter.setProgress(progress, extraType)
            }
            FilterMode.FILTER_VIGNETTE -> {
                vignetteImageFilter.setProgress(progress, extraType)
            }
        }
    }
}