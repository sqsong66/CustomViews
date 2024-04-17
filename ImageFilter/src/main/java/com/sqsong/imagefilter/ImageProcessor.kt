package com.sqsong.imagefilter

import android.graphics.Bitmap

object ImageProcessor {
    fun applyCurves(rgb: IntArray?, red: IntArray?, green: IntArray?, blue: IntArray?, inputImage: Bitmap): Bitmap {
        val width = inputImage.width
        val height = inputImage.height
        var pixels = IntArray(width * height)
        inputImage.getPixels(pixels, 0, width, 0, 0, width, height)
        if (rgb != null) {
            pixels = NativeLib.applyRGBCurve(pixels, rgb, width, height)
        }
        if (!(red == null && green == null && blue == null)) {
            pixels = NativeLib.applyChannelCurves(pixels, red, green, blue, width, height)
        }
        try {
            inputImage.setPixels(pixels, 0, width, 0, 0, width, height)
        } catch (ise: IllegalStateException) {
            ise.printStackTrace()
        }
        return inputImage
    }

    fun doBrightness(value: Int, inputImage: Bitmap): Bitmap {
        val width = inputImage.width
        val height = inputImage.height
        val pixels = IntArray(width * height)
        inputImage.getPixels(pixels, 0, width, 0, 0, width, height)
        NativeLib.doBrightness(pixels, value, width, height)
        inputImage.setPixels(pixels, 0, width, 0, 0, width, height)
        return inputImage
    }

    fun doContrast(value: Float, inputImage: Bitmap): Bitmap {
        val width = inputImage.width
        val height = inputImage.height
        val pixels = IntArray(width * height)
        inputImage.getPixels(pixels, 0, width, 0, 0, width, height)
        NativeLib.doContrast(pixels, value, width, height)
        inputImage.setPixels(pixels, 0, width, 0, 0, width, height)
        return inputImage
    }

    fun doColorOverlay(depth: Int, red: Float, green: Float, blue: Float, inputImage: Bitmap): Bitmap {
        val width = inputImage.width
        val height = inputImage.height
        val pixels = IntArray(width * height)
        inputImage.getPixels(pixels, 0, width, 0, 0, width, height)
        NativeLib.doColorOverlay(pixels, depth, red, green, blue, width, height)
        inputImage.setPixels(pixels, 0, width, 0, 0, width, height)
        return inputImage
    }

    fun doSaturation(inputImage: Bitmap, level: Float): Bitmap {
        val width = inputImage.width
        val height = inputImage.height
        val pixels = IntArray(width * height)
        inputImage.getPixels(pixels, 0, width, 0, 0, width, height)
        NativeLib.doSaturation(pixels, level, width, height)
        inputImage.setPixels(pixels, 0, width, 0, 0, width, height)
        return inputImage
    }

}