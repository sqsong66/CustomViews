package com.example.customviews.utils

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.spec.SecretKeySpec

fun extractRelativePath(absolutePath: String, fileName: String): String {
    val externalStorageRootPath = Environment.getExternalStorageDirectory().absolutePath
    return if (absolutePath.startsWith(externalStorageRootPath)) {
        absolutePath.removePrefix("$externalStorageRootPath/").removeSuffix(fileName)
    } else {
        absolutePath.removeSuffix(fileName)
    }
}

fun convertBytesToReadable(sizeInBytes: Long): String {
    val kiloBytes = sizeInBytes / 1024.0
    val megaBytes = kiloBytes / 1024.0
    val gigaBytes = megaBytes / 1024.0

    return when {
        gigaBytes > 1 -> String.format(Locale.getDefault(), "%.2f GB", gigaBytes)
        megaBytes > 1 -> String.format(Locale.getDefault(), "%.2f MB", megaBytes)
        kiloBytes > 1 -> String.format(Locale.getDefault(), "%.2f KB", kiloBytes)
        else -> "$sizeInBytes Bytes"
    }
}

fun getTrashMediaPath(context: Context): String {
    val fileFolder = context.getExternalFilesDir(null)?.absolutePath ?: context.filesDir.absolutePath
    val trashFolder = File(fileFolder, "Trash")
    if (!trashFolder.exists()) trashFolder.mkdirs()
    return trashFolder.absolutePath
}

fun getWatermarkMaterialDirPath(context: Context): String {
    val externalDir: String = context.getExternalFilesDir(null)?.absolutePath ?: context.filesDir.absolutePath
    return externalDir.plus(File.separator).plus("WatermarkMaterial")
}

fun getUndoRedoCacheDirPath(context: Context): String {
    val externalDir: String = context.getExternalFilesDir(null)?.absolutePath ?: context.filesDir.absolutePath
    return externalDir.plus(File.separator).plus("UndoRedoCache")
}

fun getWebPath(context: Context): String {
    val externalDir: String = context.getExternalFilesDir(null)?.absolutePath ?: context.filesDir.absolutePath
    return externalDir.plus(File.separator).plus("Web")
}

fun getLogPath(context: Context): String {
    val externalDir: String = context.getExternalFilesDir(null)?.absolutePath ?: context.filesDir.absolutePath
    return externalDir.plus(File.separator).plus("Logs")
}

fun getVideoPath(context: Context): String {
    val externalDir: String = context.getExternalFilesDir(null)?.absolutePath ?: context.filesDir.absolutePath
    return externalDir.plus(File.separator).plus("Videos")
}

fun getGifPath(context: Context): String {
    val externalDir: String = context.getExternalFilesDir(null)?.absolutePath ?: context.filesDir.absolutePath
    return externalDir.plus(File.separator).plus("Gifs")
}

fun deleteUndoRedoCacheFiles(context: Context) {
    val cacheDir = getUndoRedoCacheDirPath(context)
    File(cacheDir).listFiles()?.forEach {
        it.delete()
    }
}

fun copyInternalGifToPublic(context: Context, videoPath: String): Uri {
    val videoFile = File(videoPath)
    if (!videoFile.exists()) throw IllegalStateException("GIF file not exists.")
    var destFile: File? = null
    val videoUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, videoFile.name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/gif")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                videoFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } else {
            throw IllegalStateException("Failed to create gif file.")
        }
        uri
    } else {
        val destDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        if (!destDir.exists()) destDir.mkdirs()
        destFile = destDir.resolve(videoFile.name)
        videoFile.copyTo(destFile, true)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destFile)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.IS_PENDING, 0)
        }
        context.contentResolver.update(videoUri!!, contentValues, null, null)
    }

    destFile?.let {
        MediaScannerConnection.scanFile(context, arrayOf(it.absolutePath), arrayOf("image/gif")) { path, uri ->
            Log.d("songmao", "scan file path: $path, uri: $uri")
        }
    }
    return videoUri
}

fun copyInternalVideoToPublic(context: Context, videoPath: String): Uri {
    val videoFile = File(videoPath)
    if (!videoFile.exists()) throw IllegalStateException("Video file not exists.")
    var destFile: File? = null
    val videoUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, videoFile.name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                videoFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } else {
            throw IllegalStateException("Failed to create video file.")
        }
        uri
    } else {
        val destDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        if (!destDir.exists()) destDir.mkdirs()
        destFile = destDir.resolve(videoFile.name)
        videoFile.copyTo(destFile, true)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destFile)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.IS_PENDING, 0)
        }
        context.contentResolver.update(videoUri!!, contentValues, null, null)
    }

    destFile?.let {
        MediaScannerConnection.scanFile(context, arrayOf(it.absolutePath), arrayOf("video/mp4")) { path, uri ->
            Log.d("songmao", "scan file path: $path, uri: $uri")
        }
    }
    return videoUri
}

fun getVideoOutputPath(context: Context): String {
    val fileName = "VID_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())}.mp4"
    val externalDir: String = context.getExternalFilesDir(null)?.absolutePath ?: context.filesDir.absolutePath
    val outputDir = File(externalDir, "VideoOutput")
    if (!outputDir.exists()) outputDir.mkdirs()
    val outputFile = File(outputDir, fileName)
    return outputFile.absolutePath
}

fun getAssetWebUri(context: Context, fileName: String): Uri {
    if (fileName.startsWith("http")) {
        return Uri.parse(fileName)
    } else {
        val fileDir = getWebPath(context)
        val file = File(fileDir, fileName)
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.createNewFile()
            context.assets.open("web/${fileName}").use { inputStream ->
                file.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}

fun encryptZipFile(zipFile: File, encryptedZipFile: File) {
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    val key = MessageDigest.getInstance("SHA-256").digest("GQMKbBCeJ0EDCmog7tGe".toByteArray())
    val secretKey = SecretKeySpec(key, "AES")
    cipher.init(Cipher.ENCRYPT_MODE, secretKey)

    FileOutputStream(encryptedZipFile).use { outputStream ->
        outputStream.write(cipher.iv)
        CipherOutputStream(outputStream, cipher).use { cipherOutputStream ->
            FileInputStream(zipFile).use { inputStream ->
                inputStream.copyTo(cipherOutputStream)
            }
        }
    }
}

fun zipLogFiles(context: Context): Uri? {
    val logDir = getLogPath(context)
    // 使用.foo后缀，Gmail邮箱无法接收.zip后缀文件
    val zipFile = File(logDir, "Logs.foo")
    if (zipFile.exists()) zipFile.delete()
    if (zipFile.parentFile?.exists() == false) {
        zipFile.parentFile?.mkdirs()
    }
    val zipResult = zipFolder(File(logDir), zipFile)
    if (!zipResult) return null
    val zipEncryptedFile = File(logDir, "Logs_encrypted.zip")
    encryptZipFile(zipFile, zipEncryptedFile)
    // 删除原zip文件
    zipFile.delete()
    // 重命名加密后的zip文件
    zipEncryptedFile.renameTo(zipFile)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zipFile)
}

fun zipFolder(sourceFolder: File, zipFile: File): Boolean {
    val zipSuccess =  try {
        ZipOutputStream(FileOutputStream(zipFile)).use { zipOutputStream ->
            if (!flatZipFolder(sourceFolder, sourceFolder, zipOutputStream)) {
                return false
            }
        }
        true
    } catch (e: IOException) {
        e.printStackTrace()
        false
    }
    if (!zipSuccess && zipFile.exists()) {
        zipFile.delete()
    }
    return zipSuccess
}

private fun flatZipFolder(rootFolder: File, sourceFolder: File, zipOutputStream: ZipOutputStream): Boolean {
    val files = sourceFolder.listFiles() ?: return false
    for (file in files) {
        when {
            file.isDirectory -> {
                if (!flatZipFolder(rootFolder, file, zipOutputStream)) {
                    return false
                }
            }

            file.extension == "zip" || file.extension == "foo" -> { // 跳过压缩的Log文件本身
                continue
            }

            else -> {
                FileInputStream(file).use { fileInputStream ->
                    val zipEntryName = rootFolder.toURI().relativize(file.toURI()).path
                    zipOutputStream.putNextEntry(ZipEntry(zipEntryName))
                    val buffer = ByteArray(2048)
                    var length: Int
                    while (fileInputStream.read(buffer).also { length = it } > 0) {
                        zipOutputStream.write(buffer, 0, length)
                    }
                    zipOutputStream.closeEntry()
                }
            }
        }
    }
    return true
}

/**
 * 删除过期的日志文件
 */
fun deleteExpiredLogFiles(context: Context) {
    // 最多保留最近的10条日志记录数据
    val logDir = getLogPath(context)

    // 先删除压缩的zip日志文件
    File(logDir).listFiles()?.forEach { file ->
        if (!file.isDirectory) file.delete()
    }

    val logFiles = File(logDir).listFiles()
    if (logFiles.isNullOrEmpty() || logFiles.size <= 10) return
    logFiles.sortedBy { it.lastModified() }.take(logFiles.size - 10).forEach {
        it.delete()
    }
}


