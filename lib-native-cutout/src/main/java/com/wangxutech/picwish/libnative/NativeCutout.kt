package com.wangxutech.picwish.libnative

import android.graphics.*
import com.wangxutech.picwish.libnative.data.CutoutResult
import java.nio.IntBuffer

infix fun Byte.and(mask: Int): Int = toInt() and mask

object NativeCutout {

    fun nativeCutout(srcBitmap: Bitmap, maskBitmap: Bitmap, cutToEdge: Boolean = true, isMaskAlphaChannel: Boolean = false): CutoutResult {
        // srcBitmap经过底层处理后会变成黑色背景图片(底层直接操作srcBitmap的内存地址)
        return NativeLib.nativeCutout(srcBitmap.copy(Bitmap.Config.ARGB_8888, true), maskBitmap, cutToEdge, isMaskAlphaChannel)?.toCutoutResult(srcBitmap, maskBitmap)
            ?: throw IllegalStateException("Native returned object is null.")
    }

    /**
     * 使用原图和mask图合成抠图图片。
     * @param srcBitmap 原图bitmap
     * @param maskBitmap mask图bitmap
     * @param cutToEdge 是否抠出抠图图层。true：抠到抠图位置 false：按原图尺寸进行合成
     */
    fun cutoutImageFromMask(srcBitmap: Bitmap, maskBitmap: Bitmap, cutToEdge: Boolean = true): CutoutResult {
        val srcWidth = srcBitmap.width
        val srcHeight = srcBitmap.height
        val scaledMaskBitmap = Bitmap.createScaledBitmap(maskBitmap, srcWidth, srcHeight, true)

        val srcBuffer = IntBuffer.allocate(srcWidth * srcHeight)
        srcBitmap.getPixels(srcBuffer.array(), 0, srcWidth, 0, 0, srcWidth, srcHeight)

        val maskBuffer = IntBuffer.allocate(srcWidth * srcHeight)
        scaledMaskBitmap.getPixels(maskBuffer.array(), 0, srcWidth, 0, 0, srcWidth, srcHeight)
        scaledMaskBitmap.recycle()

        val rect = IntArray(4) // x, y, w, h
        // 前景优化
        NativeLib.enhanceForeground(srcBuffer.array(), srcBuffer.array(), srcWidth, srcHeight, maskBuffer.array(), rect, false)
        // mask图合成
        NativeLib.mergeRGBA(srcBuffer.array(), srcBuffer.array(), maskBuffer.array(), srcWidth, srcHeight)
        // 如果前景优化失败则使用原图尺寸进行抠图
        if (rect[2] == 0) rect[2] = srcWidth
        if (rect[3] == 0) rect[3] = srcHeight

        val outBitmap: Bitmap = if (cutToEdge) {
            // 如果裁剪到边缘，则直接根据rect数据从srcBuffer截取裁剪到边缘的bitmap
            Bitmap.createBitmap(rect[2], rect[3], Bitmap.Config.ARGB_8888).apply {
                val offset = srcWidth * rect[1] + rect[0]
                setPixels(srcBuffer.array(), offset, srcWidth, 0, 0, rect[2], rect[3])
            }
        } else {
            // 否则填充原图
            Bitmap.createBitmap(srcWidth, srcHeight, Bitmap.Config.ARGB_8888).apply {
                setPixels(srcBuffer.array(), 0, srcWidth, 0, 0, srcWidth, srcHeight)
            }
        }
        val srcRect = Rect(0, 0, srcWidth, srcHeight)

        // 清空buffer
        srcBuffer.clear()
        maskBuffer.clear()
        return if (cutToEdge) {
            val cutRect = Rect(rect[0], rect[1], rect[2], rect[3])
            CutoutResult(outBitmap, srcRect, cutRect)
        } else {
            CutoutResult(outBitmap, srcRect, srcRect)
        }
    }

    /**
     * 将手动抠图涂抹后的bitmap缩放到跟原mask图一样大小。
     * @param maskBitmap 原mask图bitmap
     * @param paintBitmap 手动抠图涂抹后的bitmap
     * @return 进行缩放后的结果mask图bitmap
     */
    fun scalePaintBitmap(maskBitmap: Bitmap?, paintBitmap: Bitmap?): Bitmap? {
        if (maskBitmap == null || paintBitmap == null) return null
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            isFilterBitmap = true
        }
        val scaledBitmap = Bitmap.createBitmap(maskBitmap.width, maskBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(scaledBitmap).apply {
            drawFilter = PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        }
        val srcRect = Rect(0, 0, paintBitmap.width, paintBitmap.height)
        val destRect = Rect(0, 0, maskBitmap.width, maskBitmap.height)
        canvas.drawBitmap(paintBitmap, srcRect, destRect, paint)
        return scaledBitmap
    }

    /**
     * 创建阴影图片；
     * @param bitmap 原图
     * @param color 阴影颜色
     * @param widthIncreasePixels 宽度增加的像素值
     */
    fun nativeCreateShadow(bitmap: Bitmap, color: Int, widthIncreasePixels: Int): Bitmap {
        val srcBuffer = IntBuffer.allocate(bitmap.width * bitmap.height)
        bitmap.getPixels(srcBuffer.array(), 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val destWidth: Int = bitmap.width + widthIncreasePixels
        val destHeight: Int = destWidth * bitmap.height / bitmap.width
        val heightIncreasePixels = widthIncreasePixels * bitmap.height / bitmap.width
        val shadowBuffer = IntBuffer.allocate(destWidth * destHeight)

        val red = Color.red(color).toByte()
        val green = Color.green(color).toByte()
        val blue = Color.blue(color).toByte()
        NativeLib.createShadow(shadowBuffer.array(), destWidth, destHeight, widthIncreasePixels / 2, heightIncreasePixels / 2, srcBuffer.array(), bitmap.width, bitmap.height, red, green, blue)
        val destBitmap = Bitmap.createBitmap(destWidth, destHeight, Bitmap.Config.ARGB_8888)
        destBitmap.setPixels(shadowBuffer.array(), 0, destWidth, 0, 0, destWidth, destHeight)
        return destBitmap
    }

}