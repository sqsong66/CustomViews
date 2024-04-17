package com.sqsong.imagefilter.processor

import android.graphics.Bitmap

interface SubFilter {

    var tag: Any?

    fun process(input: Bitmap): Bitmap

}