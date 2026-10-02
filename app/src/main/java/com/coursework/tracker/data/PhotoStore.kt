package com.coursework.tracker.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 照片管理：从系统相册选中的图片会被复制进应用私有目录，
 * 这样不依赖相册的临时授权，重启手机后依然能显示。
 */
object PhotoStore {

    private const val DIR_NAME = "photos"

    /** 用可用内存的 1/8 做位图缓存，避免列表滚动时反复解码 */
    private val cache: LruCache<String, Bitmap> by lazy {
        val maxKb = (Runtime.getRuntime().maxMemory() / 1024L / 8L).toInt().coerceAtLeast(4 * 1024)
        object : LruCache<String, Bitmap>(maxKb) {
            override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
        }
    }

    fun directory(context: Context): File =
        File(context.filesDir, DIR_NAME).apply { if (!exists()) mkdirs() }

    fun fileOf(context: Context, name: String): File = File(directory(context), name)

    /** 把相册里的图片复制进来，返回新文件名；失败返回 null */
    suspend fun import(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val name = "img_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg"
            val target = File(directory(context), name)
            val opened = context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
                true
            } ?: false
            if (opened) name else null
        }.getOrNull()
    }

    suspend fun delete(context: Context, names: List<String>) = withContext(Dispatchers.IO) {
        names.forEach { name ->
            // 缓存 key 是「文件名@目标尺寸」，这里把所有尺寸的缓存一并清掉
            cache.snapshot().keys
                .filter { it.startsWith("$name@") }
                .forEach { cache.remove(it) }
            runCatching { fileOf(context, name).delete() }
        }
    }

    suspend fun load(context: Context, name: String, maxSizePx: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            val key = "$name@$maxSizePx"
            cache.get(key)?.let { return@withContext it }
            val file = fileOf(context, name)
            if (!file.exists()) return@withContext null
            val bitmap = runCatching { decode(file, maxSizePx) }.getOrNull()
                ?: return@withContext null
            cache.put(key, bitmap)
            bitmap
        }

    const val FULL = 1600

    /**
     * 直接从相册返回的 Uri 解码一张图（不落盘），用于自定义图标的裁剪流程。
     * 同样做降采样和 EXIF 转正。
     */
    suspend fun decodeFromUri(context: Context, uri: Uri, maxSizePx: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, bounds)
                }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

                var sample = 1
                while (bounds.outWidth / sample > maxSizePx * 2 &&
                    bounds.outHeight / sample > maxSizePx * 2
                ) {
                    sample *= 2
                }

                val decoded = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(
                        it,
                        null,
                        BitmapFactory.Options().apply { inSampleSize = sample },
                    )
                } ?: return@runCatching null

                val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
                    runCatching { ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
                        .getOrDefault(ExifInterface.ORIENTATION_NORMAL)
                } ?: ExifInterface.ORIENTATION_NORMAL

                applyExif(decoded, orientation)
            }.getOrNull()
        }

    private fun decode(file: File, maxSizePx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / sample > maxSizePx * 2 && bounds.outHeight / sample > maxSizePx * 2) {
            sample *= 2
        }

        val decoded = BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample },
        ) ?: return null

        return applyExifRotation(file, decoded)
    }

    /** 手机拍的照片常常是「图是横的，靠 EXIF 标记要转多少度」，这里手动转正 */
    private fun applyExifRotation(file: File, source: Bitmap): Bitmap {
        val orientation = runCatching {
            @Suppress("DEPRECATION")
            ExifInterface(file.absolutePath)
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        return applyExif(source, orientation)
    }

    private fun applyExif(source: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return source
        }

        return runCatching {
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
                .also { rotated -> if (rotated !== source) source.recycle() }
        }.getOrDefault(source)
    }
}
