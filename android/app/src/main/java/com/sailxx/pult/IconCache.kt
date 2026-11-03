package com.sailxx.pult

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Двухуровневый кэш иконок (память + локальный диск).
 * Обеспечивает мгновенный рендеринг иконок EXE без повторной загрузки по сети при каждом старте.
 */
object IconCache {
    private val memoryCache = ConcurrentHashMap<String, ImageBitmap>()

    fun getFromMemory(id: String): ImageBitmap? = memoryCache[id]

    suspend fun getOrFetch(context: Context, client: Client, id: String): ImageBitmap? {
        memoryCache[id]?.let { return it }

        return withContext(Dispatchers.IO) {
            val iconsDir = File(context.cacheDir, "pult_icons").apply { if (!exists()) mkdirs() }
            val diskFile = File(iconsDir, "$id.png")

            // 1. Попытка чтения с диска
            if (diskFile.exists() && diskFile.length() > 0) {
                try {
                    val bytes = diskFile.readBytes()
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                    if (bmp != null) {
                        memoryCache[id] = bmp
                        return@withContext bmp
                    }
                } catch (_: Exception) {}
            }

            // 2. Загрузка по сети с хоста Windows
            try {
                val bytes = client.icon(id)
                if (bytes != null && bytes.isNotEmpty()) {
                    try { diskFile.writeBytes(bytes) } catch (_: Exception) {}
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                    if (bmp != null) {
                        memoryCache[id] = bmp
                        return@withContext bmp
                    }
                }
            } catch (_: Exception) {}

            null
        }
    }

    fun clearMemory() {
        memoryCache.clear()
    }
}
