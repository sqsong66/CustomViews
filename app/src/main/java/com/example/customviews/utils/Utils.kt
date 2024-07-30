package com.example.customviews.utils

import android.content.Context
import com.example.customviews.R
import com.example.customviews.data.PhotoAdjustmentData
import com.sqsong.opengllib.filters.FilterMode
import java.io.File

fun range(value: Float, start: Float, end: Float): Float {
    return (end - start) * value + start
}

fun getAdjustmentFilterData(context: Context): List<PhotoAdjustmentData> {
    val dataList = mutableListOf<PhotoAdjustmentData>()
    dataList.add(PhotoAdjustmentData(FilterMode.FILTER_BRIGHTNESS, context.getString(R.string.key_adjust_brightness), R.drawable.ic_adjustment_brightness, 0f, false, -100f, 100f, 4f, true))
    dataList.add(PhotoAdjustmentData(FilterMode.FILTER_CONTRAST, context.getString(R.string.key_adjust_contrast), R.drawable.ic_adjustment_contrast, 0f, false, -100f, 100f, 4f))
    dataList.add(PhotoAdjustmentData(FilterMode.FILTER_SATURATION, context.getString(R.string.key_adjust_saturation), R.drawable.ic_adjustment_saturation, 0f, false, -100f, 100f, 4f))
    dataList.add(PhotoAdjustmentData(FilterMode.FILTER_EXPOSURE, context.getString(R.string.key_adjust_exposure), R.drawable.ic_adjustment_exposure, 0f, false, -100f, 100f, 4f))
    dataList.add(PhotoAdjustmentData(FilterMode.FILTER_HIGHLIGHT, context.getString(R.string.key_adjust_highlight), R.drawable.ic_adjustment_highlights, 0f, false, -100f, 100f, 4f))
    dataList.add(PhotoAdjustmentData(FilterMode.FILTER_SHADOW, context.getString(R.string.key_adjust_shadow), R.drawable.ic_adjustment_shadow, 0f, false, -100f, 100f, 4f))
    dataList.add(PhotoAdjustmentData(FilterMode.FILTER_TEMPERATURE, context.getString(R.string.key_adjust_temperature), R.drawable.ic_adjustment_temperature, 0f, false, -100f, 100f, 4f))
    dataList.add(PhotoAdjustmentData(FilterMode.FILTER_SHARPNESS, context.getString(R.string.key_adjust_sharpness), R.drawable.ic_adjustment_sharpen, 0f, false, 0f, 100f, 2f))
    dataList.add(PhotoAdjustmentData(FilterMode.FILTER_VIGNETTE, context.getString(R.string.key_adjust_vignette), R.drawable.ic_adjustment_vignette, 0f, false, 0f, 100f, 2f))
    return dataList
}

