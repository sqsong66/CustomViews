package com.wangxutech.picwish.libnative

import android.graphics.Bitmap
import androidx.annotation.FloatRange
import com.wangxutech.picwish.libnative.data.NativeCutoutResult

object NativeLib {

    init {
        System.loadLibrary("libnative")
    }

    external fun enhanceForeground(dst: IntArray?, src: IntArray?, width: Int, height: Int, alpha: IntArray?, rect: IntArray?, isMaskAlphaChannel: Boolean): Int

    external fun mergeRGBA(dst: IntArray?, src: IntArray?, alpha: IntArray?, width: Int, height: Int): Int

    external fun createShadow(rgba_view: IntArray, view_width: Int, view_height: Int, x: Int, y: Int, rgba_fg: IntArray, fg_width: Int, fg_height: Int, r: Byte, g: Byte, b: Byte): Int

    /**
     * Native进行抠图；
     * 注意：由于底层复用了[srcBitmap] (直接操作的是srcBitmap的像素数据), 所以经过改方法后[srcBitmap]的数据已发生变动，
     * 如果上传需要使用到原图bitmap，则需要自己重新去decode。
     * @param srcBitmap 原图bitmap
     * @param maskBitmap mask图bitmap
     * @param cutToEdge 是否裁剪到边缘。 true-裁剪到边缘(底层会创建一个裁剪到边缘的新bitmap对象)； false-底层复用的[srcBitmap]，所以上层请勿
     * 回收该bitmap对象，否则将会出错。
     * @param isMaskAlphaChannel mask图是否带alpha通道
     * @return [NativeCutoutResult]包含抠图bitmap及图片的位置和宽高信息
     */
    external fun nativeCutout(srcBitmap: Bitmap, maskBitmap: Bitmap, cutToEdge: Boolean, isMaskAlphaChannel: Boolean): NativeCutoutResult?

    external fun nativeCutout1(srcBitmap: Bitmap, maskBitmap: Bitmap, cutToEdge: Boolean, isMaskAlphaChannel: Boolean): NativeCutoutResult?

    /**
     * 根据黑白mask图创建带颜色的mask图，用于手动抠图遮罩。
     * @param maskBitmap 黑白mask图
     * @param r red通道色值
     * @param g green通道色值
     * @param b blue通道色值
     * @return 红色的mask图
     */
    external fun createColorBitmapFromMask(maskBitmap: Bitmap, r: Int, g: Int, b: Int): Bitmap?

    /**
     * 将涂抹的带alpha通道的bitmap转换成黑白mask图。由于该图实际上只操作的是alpha通道(其它通道为固定的纯色)，
     * 所以底层操作将其变换为黑白mask图时，只是将alpha通道的值赋值到其它rgb通道，alpha通道修改为255.
     * @param paintBitmap 涂抹的带透明通道的颜色mask图
     */
    external fun convertAlphaMaskBitmap(paintBitmap: Bitmap): Bitmap?

    external fun handleSmoothAndWhiteSkin(bitmap: Bitmap, @FloatRange(from = 1.0, to = 500.0) smoothValue: Float, @FloatRange(from = 1.0, to = 10.0) whiteValue: Float)

    external fun adjustBitmap(bitmap: Bitmap)

    external fun unPremultipliedBitmap(bitmap: Bitmap)

    external fun premultiplyAlpha(bitmap: Bitmap)

    external fun getGlBitmap(width: Int, height: Int): Bitmap?

    external fun lightOn(bitmap: Bitmap): Bitmap

    external fun convertMaskToAlphaChannel(maskBitmap: Bitmap): Bitmap

    external fun cropPNGImageBitmap(bitmap: Bitmap): IntArray
}