package com.laundryku.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageUtils {

    private const val MAX_SIDE = 800
    private const val MAX_BYTES = 300_000

    // Membaca gambar, mengecilkan ukurannya, lalu mengubahnya jadi teks Base64
    fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            val scaled = decodeScaled(context, uri) ?: return null
            val upright = rotateIfNeeded(context, uri, scaled)

            var quality = 70
            var bytes: ByteArray
            do {
                val out = ByteArrayOutputStream()
                upright.compress(Bitmap.CompressFormat.JPEG, quality, out)
                bytes = out.toByteArray()
                quality -= 10
            } while (bytes.size > MAX_BYTES && quality >= 30)

            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Throwable) {
            null
        }
    }

    // Mengubah teks Base64 dari Firestore kembali jadi gambar
    fun base64ToBitmap(base64: String): Bitmap? {
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Throwable) {
            null
        }
    }

    private fun decodeScaled(context: Context, uri: Uri): Bitmap? {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val largest = max(bounds.outWidth, bounds.outHeight)
        if (largest <= 0) return null

        var sample = 1
        while (largest / sample > MAX_SIDE * 2) sample *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        val scale = MAX_SIDE.toFloat() / max(decoded.width, decoded.height)
        if (scale >= 1f) return decoded
        return Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * scale).toInt(),
            (decoded.height * scale).toInt(),
            true
        )
    }

    // Foto dari kamera sering tersimpan miring, jadi diputar sesuai data EXIF
    private fun rotateIfNeeded(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = context.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) return bitmap

        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}