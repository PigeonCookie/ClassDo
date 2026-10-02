package com.coursework.tracker.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 用户自定义图标的存取。裁剪好的图按 PNG 存在应用私有目录，
 * 和相册里的原图完全独立，之后删掉相册原图也不影响。
 */
class IconStore(context: Context) {

    private val file = File(
        File(context.filesDir, "branding").apply { if (!exists()) mkdirs() },
        "app_icon.png",
    )

    suspend fun save(bitmap: Bitmap) = withContext(Dispatchers.IO) {
        runCatching {
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }
    }

    suspend fun load(): Bitmap? = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null
        runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        runCatching { file.delete() }
    }
}
