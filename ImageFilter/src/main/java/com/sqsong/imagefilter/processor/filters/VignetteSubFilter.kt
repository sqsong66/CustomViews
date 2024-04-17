package com.sqsong.imagefilter.processor.filters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import com.sqsong.imagefilter.ImageProcessor
import com.sqsong.imagefilter.R
import com.sqsong.imagefilter.processor.SubFilter

class VignetteSubFilter(
    override var tag: Any? = null,
    private val context: Context,
    private val alphaValue: Int,
) : SubFilter {

    override fun process(input: Bitmap): Bitmap {
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.vignette)
        val newBitmap = Bitmap.createScaledBitmap(bitmap, input.width, input.height, true)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.alpha = alphaValue }
        val canvas = Canvas(input)
        canvas.drawBitmap(newBitmap, 0f, 0f, paint)
        return input
    }
}