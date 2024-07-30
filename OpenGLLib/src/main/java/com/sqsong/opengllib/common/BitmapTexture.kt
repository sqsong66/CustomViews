package com.sqsong.opengllib.common

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils
import android.util.Log
import java.lang.ref.WeakReference

class BitmapTexture(bitmap: Bitmap) : Texture(bitmap.width, bitmap.height) {

    private val bitmapWeakRef: WeakReference<Bitmap> = WeakReference(bitmap)

    init {
        create()
    }

    override fun onTextureCreated() {
        // Log.d("BaseImageFilter", "onTextureCreated: ${bitmapWeakRef.get()}")
        bitmapWeakRef.get()?.let { bitmap ->
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        }
    }

}