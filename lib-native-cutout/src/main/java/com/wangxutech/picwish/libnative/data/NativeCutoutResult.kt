package com.wangxutech.picwish.libnative.data

import android.graphics.Bitmap
import android.graphics.Rect

data class NativeCutoutResult(
    var cutoutBitmap: Bitmap?,
    val srcArray: IntArray = intArrayOf(0, 0, 0, 0),
    val cutoutArray: IntArray = intArrayOf(0, 0, 0, 0),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NativeCutoutResult

        if (cutoutBitmap != other.cutoutBitmap) return false
        return cutoutArray.contentEquals(other.cutoutArray)
    }

    override fun hashCode(): Int {
        var result = cutoutBitmap.hashCode()
        result = 31 * result + cutoutArray.contentHashCode()
        return result
    }

    fun toCutoutResult(srcBitmap: Bitmap, maskBitmap: Bitmap): CutoutResult {
        val srcRect = Rect(srcArray[0], srcArray[1], srcArray[2], srcArray[3])
        val cutoutRect = Rect(cutoutArray[0], cutoutArray[1], cutoutArray[2], cutoutArray[3])
        val bitmap = cutoutBitmap ?: throw IllegalArgumentException("Cutout bitmap is null.")
        return CutoutResult(bitmap, srcRect = srcRect, cutoutRect = cutoutRect, maskBitmap = maskBitmap, srcBitmap = srcBitmap)
    }
}
