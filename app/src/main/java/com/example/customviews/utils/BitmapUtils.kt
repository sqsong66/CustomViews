package com.example.customviews.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.bumptech.glide.Glide
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
    val bWidth = bitmap.width
    val bHeight = bitmap.height
    if (bWidth <= maxSize && bHeight <= maxSize) {
        return bitmap
    }
    val scale = if (bWidth > bHeight) {
        maxSize.toFloat() / bWidth
    } else {
        maxSize.toFloat() / bHeight
    }
    val matrix = Matrix().apply { postScale(scale, scale) }
    return Bitmap.createBitmap(bitmap, 0, 0, bWidth, bHeight, matrix, true)
}

/**
 * 使用[Glide]进行图片加载。比[decodeBitmapFromUri]更加高效。
 * @param context 上下文
 * @param uri 图片Uri
 * @param maxSize 图片最大尺寸
 * @return 返回解码后的Bitmap
 */
fun decodeBitmapByGlide(context: Context, uri: Uri?, maxSize: Int = 4096): Bitmap? {
    uri ?: return null
    val inputStream = context.contentResolver.openInputStream(uri)
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeStream(inputStream, null, options)
    inputStream?.close()
    options.inJustDecodeBounds = false

    val width: Int
    val height: Int
    when {
        options.outWidth <= maxSize && options.outHeight <= maxSize -> {
            width = options.outWidth
            height = options.outHeight
        }

        options.outWidth > options.outHeight -> {
            width = maxSize
            height = options.outHeight * width / options.outWidth
        }

        else -> {
            height = maxSize
            width = options.outWidth * height / options.outHeight
        }
    }
    return Glide.with(context).asBitmap().load(uri).submit(width, height).get()
}

fun decodeResourceBitmapByGlide(context: Context, resId: Int, maxSize: Int = 4096): Bitmap {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeResource(context.resources, resId, options)
    options.inJustDecodeBounds = false

    val width: Int
    val height: Int
    when {
        options.outWidth <= maxSize && options.outHeight <= maxSize -> {
            width = options.outWidth
            height = options.outHeight
        }

        options.outWidth > options.outHeight -> {
            width = maxSize
            height = options.outHeight * width / options.outWidth
        }

        else -> {
            height = maxSize
            width = options.outWidth * height / options.outHeight
        }
    }
    return Glide.with(context).asBitmap().load(resId).submit(width, height).get()
}

fun decodeResourceBitmapToSize(context: Context, resId: Int, size: Size = Size(720, 1280), maxSize: Int = 4096): Bitmap {
    val bitmap = decodeResourceBitmapByGlide(context, resId, maxSize)
    if (bitmap.width == size.width && bitmap.height == size.height) {
        return bitmap
    }
    // 根据宽高比缩放图片，然后沿中心裁剪
    val sRatio = size.width.toFloat() / size.height
    val bRatio = bitmap.width.toFloat() / bitmap.height
    val scale = if (sRatio > bRatio) {
        size.width.toFloat() / bitmap.width
    } else {
        size.height.toFloat() / bitmap.height
    }
    val matrix = Matrix().apply { postScale(scale, scale) }
    val scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    val x = (scaledBitmap.width - size.width) / 2
    val y = (scaledBitmap.height - size.height) / 2
    val destBitmap = Bitmap.createBitmap(scaledBitmap, x, y, size.width, size.height)
    scaledBitmap.recycle()
    return destBitmap
}

fun decodeBitmapFromUri(context: Context, uri: Uri?, maxSize: Int): Bitmap? {
    uri ?: return null
    return try {
        // 第一步: 解码图片尺寸
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }

        // 第二步: 计算缩放比例
        options.apply {
            inSampleSize = calculateInSampleSize(this, maxSize)
            inJustDecodeBounds = false
        }

        // 第三步: 解码Bitmap
        val decodedBitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }

        // 第四步: 调整Bitmap尺寸
        decodedBitmap?.let { resizeBitmap(it, maxSize) }
    } catch (ex: Exception) {
        ex.printStackTrace()
        null
    }
}

fun decodeBitmapFromResource(context: Context, resId: Int, maxSize: Int = 4096): Bitmap? {
    return try {
        // 第一步: 解码图片尺寸
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeResource(context.resources, resId, options)

        // 第二步: 计算缩放比例
        options.apply {
            inSampleSize = calculateInSampleSize(this, maxSize)
            inJustDecodeBounds = false
        }

        // 第三步: 解码Bitmap
        val decodedBitmap = BitmapFactory.decodeResource(context.resources, resId, options)

        // 第四步: 调整Bitmap尺寸
        decodedBitmap?.let { resizeBitmap(it, maxSize) }
    } catch (ex: Exception) {
        ex.printStackTrace()
        null
    }
}

fun decodeBitmapFromAssets(context: Context, assetsPath: String, maxSize: Int): Bitmap? {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.assets.open(assetsPath).use {
        BitmapFactory.decodeStream(it, null, options)
    }
    options.inJustDecodeBounds = false

    val width: Int
    val height: Int
    when {
        options.outWidth <= maxSize && options.outHeight <= maxSize -> {
            width = options.outWidth
            height = options.outHeight
        }

        options.outWidth > options.outHeight -> {
            width = maxSize
            height = options.outHeight * width / options.outWidth
        }

        else -> {
            height = maxSize
            width = options.outWidth * height / options.outHeight
        }
    }
    return Glide.with(context).asBitmap().load("file:///android_asset/$assetsPath").submit(width, height).get()
}

fun calculateInSampleSize(options: BitmapFactory.Options, maxSize: Int): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1

    if (height > maxSize || width > maxSize) {
        val halfHeight = height / 2
        val halfWidth = width / 2

        while ((halfHeight / inSampleSize) >= maxSize && (halfWidth / inSampleSize) >= maxSize) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Uri {
    val fileName = "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())}.png"
    var destFile: File? = null
    val destUri = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw IllegalStateException("Failed insert new image.")
        }

        else -> {
            val destDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            if (!destDir.exists()) destDir.mkdirs()
            destFile = destDir.resolve(fileName)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destFile)
        }
    }
    // 将图片数据写入到Uri
    context.contentResolver.openOutputStream(destUri)?.use {
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
    }
    // 保存图片后，更新操作状态
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.IS_PENDING, 0)
        }
        context.contentResolver.update(destUri, contentValues, null, null)
    }

    destFile?.let {
        MediaScannerConnection.scanFile(context, arrayOf(it.absolutePath), null) { path, uri ->
            Log.d("songmao", "saveBitmapToGallery, scan file path: $path, uri: $uri")
        }
    }
    return destUri
}
