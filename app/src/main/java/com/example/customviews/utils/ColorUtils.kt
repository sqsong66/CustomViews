package com.example.customviews.utils

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.core.graphics.ColorUtils
import com.google.android.material.color.MaterialColors
import java.util.Random

fun getThemeColorWithAlpha(context: Context, resId: Int, alpha: Int): Int {
    val color = MaterialColors.getColor(context, resId, Color.TRANSPARENT)
    return ColorUtils.setAlphaComponent(color, alpha)
}

fun getThemeColor(context: Context?, resId: Int): Int {
    if (context == null) return Color.TRANSPARENT
    return MaterialColors.getColor(context, resId, Color.TRANSPARENT)
}

fun getSurfaceGradientDrawable(context: Context): GradientDrawable {
    val startColor = getThemeColor(context, com.google.android.material.R.attr.colorSurface)
    val middleColor = getThemeColorWithAlpha(context, com.google.android.material.R.attr.colorSurface, (255 * 0.8f).toInt())
    val endColor = Color.TRANSPARENT
    return GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(startColor, middleColor, endColor))
}

fun generateRandomColor(): Int {
    val random = Random()
    // 使用Random来生成0-255范围内的ARGB值
    val alpha = 255 // 完全不透明
    val red = random.nextInt(256) // 生成0-255范围内的红色分量
    val green = random.nextInt(256) // 生成0-255范围内的绿色分量
    val blue = random.nextInt(256) // 生成0-255范围内的蓝色分量
    // 使用Color类的argb方法来创建颜色
    return Color.argb(alpha, red, green, blue)
}