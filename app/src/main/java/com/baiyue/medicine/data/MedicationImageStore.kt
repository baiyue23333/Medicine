package com.baiyue.medicine.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Stores medication photos as downscaled JPEGs in app-private storage and removes files that are
 * no longer referenced. This keeps memory use predictable and prevents orphaned full-size photos.
 */
object MedicationImageStore {

    private const val DIR_NAME = "med_images"
    private const val JPEG_QUALITY = 88
    private const val DEFAULT_MAX_DIMENSION = 1600

    fun save(context: Context, source: Uri, maxDimension: Int = DEFAULT_MAX_DIMENSION): String? = runCatching {
        val dir = File(context.filesDir, DIR_NAME).apply { mkdirs() }
        val dest = File(dir, "${UUID.randomUUID()}.jpg")
        val bitmap = decode(context, source, maxDimension) ?: return@runCatching null
        try {
            FileOutputStream(dest).use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
                    return@runCatching null
                }
            }
        } finally {
            bitmap.recycle()
        }
        Uri.fromFile(dest).toString()
    }.getOrNull()

    fun loadBitmap(context: Context, uri: String, maxDimension: Int = DEFAULT_MAX_DIMENSION): Bitmap? =
        runCatching { decode(context, Uri.parse(uri), maxDimension) }.getOrNull()

    /** Deletes only files inside the app-private medication image directory. */
    fun delete(context: Context, uri: String) {
        if (uri.isBlank()) return
        runCatching {
            val parsed = Uri.parse(uri)
            if (parsed.scheme != "file") return
            val path = parsed.path ?: return
            val dir = File(context.filesDir, DIR_NAME).canonicalFile
            val file = File(path).canonicalFile
            if (file.parentFile == dir) file.delete()
        }
    }

    /** Removes files in the private image directory that no medication references. */
    fun pruneOrphans(context: Context, usedUris: Set<String>) {
        runCatching {
            val dir = File(context.filesDir, DIR_NAME)
            if (!dir.isDirectory) return
            val usedFiles = usedUris.mapNotNull { uri ->
                runCatching {
                    val parsed = Uri.parse(uri)
                    val path = parsed.path
                    if (parsed.scheme == "file" && path != null) File(path).canonicalPath else null
                }.getOrNull()
            }.toSet()
            dir.listFiles()?.forEach { file ->
                if (file.isFile && file.canonicalPath !in usedFiles) file.delete()
            }
        }
    }

    private fun decode(context: Context, source: Uri, maxDimension: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = ImageSizing.calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = context.contentResolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        val rotation = readRotation(context, source)
        if (rotation == 0f) return decoded
        return runCatching {
            val matrix = Matrix().apply { postRotate(rotation) }
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                .also { if (it !== decoded) decoded.recycle() }
        }.getOrDefault(decoded)
    }

    private fun readRotation(context: Context, source: Uri): Float = runCatching {
        context.contentResolver.openInputStream(source)?.use { input ->
            val orientation = ExifInterface(input).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
    }.getOrDefault(0f)
}
