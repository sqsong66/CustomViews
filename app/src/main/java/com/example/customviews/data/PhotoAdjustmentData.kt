package com.example.customviews.data

import com.sqsong.opengllib.filters.FilterMode

data class PhotoAdjustmentData(
    @FilterMode val filterMode: Int,
    val name: String,
    val icon: Int,
    var intensity: Float,
    var isValueChangeMode: Boolean = false,
    val minValue: Float = 0f,
    val maxValue: Float = 100f,
    val stepUnit: Float = 2f,
    val clear: Boolean = false,
)
