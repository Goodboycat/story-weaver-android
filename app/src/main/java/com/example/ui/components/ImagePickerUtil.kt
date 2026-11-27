package com.example.ui.components

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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
            bytesToResizedBase64DataUrl(bytes, maxDimension, quality)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun bytesToResizedBase64DataUrl(
        bytes: ByteArray,
        maxDimension: Int,
        quality: Int = 76,
    ): String? = withContext(Dispatchers.IO) {
        try {
            val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return@withContext null
            val scale = if (maxDimension > 0) {
                maxDimension.toFloat() / max(original.width, original.height).toFloat()
            } else {
                1f
            }
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

    fun decodeBase64Payload(raw: String?): ByteArray? {
        if (raw.isNullOrBlank()) return null
        return try {
            val payload = if (raw.contains(",")) raw.substringAfter(",") else raw
            Base64.decode(payload.trim(), Base64.DEFAULT)
        } catch (_: Exception) {
            null
        }
    }

    fun decodeDataUrlBitmap(dataUrl: String?): Bitmap? {
        val bytes = decodeBase64Payload(dataUrl) ?: return null
        return try {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveDataUrlToGallery(
        context: Context,
        dataUrl: String,
        displayNamePrefix: String = "storyweaver_art",
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val bytes = decodeBase64Payload(dataUrl)
                ?: return@withContext Result.failure(IllegalArgumentException("Could not decode image data."))
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                return@withContext Result.failure(UnsupportedOperationException("NEEDS_SAF"))
            }
            val isPng = dataUrl.startsWith("data:image/png", ignoreCase = true)
            val mimeType = if (isPng) "image/png" else "image/jpeg"
            val ext = if (isPng) "png" else "jpg"
            val fileName = "${displayNamePrefix}_${System.currentTimeMillis()}.$ext"

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/StoryWeaver")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext Result.failure(IllegalStateException("Could not create MediaStore gallery entry."))
            resolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: return@withContext Result.failure(IllegalStateException("Could not write image to gallery."))
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            Result.success("Saved to Gallery (Pictures/StoryWeaver/$fileName)")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun writeDataUrlToUri(
        context: Context,
        uri: Uri,
        dataUrl: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val bytes = decodeBase64Payload(dataUrl)
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid image bytes."))
            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: return@withContext Result.failure(IllegalStateException("Could not write image file."))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
