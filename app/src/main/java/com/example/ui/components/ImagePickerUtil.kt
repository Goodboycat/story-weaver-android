package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object ImagePickerUtil {
    suspend fun uriToResizedBase64DataUrl(
        context: Context,
        uri: Uri,
        maxDimension: Int,
        quality: Int = 78,
    ): String? = withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext null
            val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return@withContext null
            val scale = maxDimension.toFloat() / max(original.width, original.height).toFloat()
            val target = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    original,
                    (original.width * scale).roundToInt().coerceAtLeast(1),
                    (original.height * scale).roundToInt().coerceAtLeast(1),
                    true
                )
            } else {
                original
            }
            val out = ByteArrayOutputStream()
            target.compress(Bitmap.CompressFormat.JPEG, quality, out)
            if (target !== original) target.recycle()
            original.recycle()
            val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            "data:image/jpeg;base64,$b64"
        } catch (_: Exception) {
            null
        }
    }

    fun decodeDataUrlBitmap(dataUrl: String?): Bitmap? {
        if (dataUrl.isNullOrBlank()) return null
        return try {
            val payload = if (dataUrl.contains(",")) dataUrl.substringAfter(",") else dataUrl
            val bytes = Base64.decode(payload, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }
}
