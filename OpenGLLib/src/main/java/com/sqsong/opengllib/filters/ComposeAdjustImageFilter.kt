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

    private val highlightShadowImageFilter by lazy {
        HighlightShadowFilter(context, initOutputBuffer = false)
    }

    private val temperatureTintImageFilter by lazy {
        TemperatureTintFilter(context, initOutputBuffer = false)
    }

    private val sharpenImageFilter by lazy {
        SharpenImageFilter(context, initOutputBuffer = false)
    }

    init {
        addFilter(brightnessImageFilter)
        addFilter(temperatureTintImageFilter)
        addFilter(contrastImageFilter)
        addFilter(saturationImageFilter)
        addFilter(highlightShadowImageFilter)
        addFilter(sharpenImageFilter)
    }

    override fun setProgress(progress: Float, extraType: Int) {
        when (extraType) {
            FilterMode.FILTER_BRIGHTNESS -> {
                brightnessImageFilter.setProgress(progress, extraType)
            }

            FilterMode.FILTER_TEMPERATURE -> {
                temperatureTintImageFilter.setProgress(progress, extraType)
            }

            FilterMode.FILTER_CONTRAST -> {
                contrastImageFilter.setProgress(progress, extraType)
            }

            FilterMode.FILTER_SATURATION -> {
                saturationImageFilter.setProgress(progress, extraType)
            }

            FilterMode.FILTER_HIGHLIGHT -> {
                highlightShadowImageFilter.setProgress(progress, extraType)
            }

            FilterMode.FILTER_SHADOW -> {
                highlightShadowImageFilter.setProgress(progress, extraType)
            }

            FilterMode.FILTER_SHARPNESS -> {
                sharpenImageFilter.setProgress(progress, extraType)
            }

            FilterMode.FILTER_TINT -> {
                temperatureTintImageFilter.setProgress(progress, extraType)
            }
        }
    }
}