package com.sqsong.imagefilter.processor.filters

import android.graphics.Bitmap
import com.sqsong.imagefilter.ImageProcessor
import com.sqsong.imagefilter.processor.SubFilter

class SaturationSubFilter(
    override var tag: Any? = null,
    private val level: Float
) : SubFilter {


    override fun process(input: Bitmap): Bitmap {
        return ImageProcessor.doSaturation(input, level)
    }
}