package de.ingomc.nozio.ui.search

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream

internal data class PreparedNutritionImage(
    val bitmap: Bitmap,
    val base64: String
)

internal fun prepareNutritionImageFromFile(
    imagePath: String,
    maxSide: Int = 1800
): PreparedNutritionImage? {
    val decoded = BitmapFactory.decodeFile(imagePath) ?: return null
    val orientation = try {
        ExifInterface(imagePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    } catch (_: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }
    return prepareNutritionImage(decoded, orientation, maxSide)
}

internal fun prepareNutritionImageFromUri(
    context: Context,
    uri: Uri,
    maxSide: Int = 1800
): String? {
    val orientation = try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
    } catch (_: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }

    val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream)
    } ?: return null

    val prepared = prepareNutritionImage(decoded, orientation, maxSide) ?: return null
    val base64 = prepared.base64
    prepared.bitmap.recycle()
    return base64
}

private fun prepareNutritionImage(
    decoded: Bitmap,
    orientation: Int,
    maxSide: Int
): PreparedNutritionImage? {
    return try {
        val rotated = decoded.rotateByExifOrientation(orientation)
        if (rotated !== decoded) {
            decoded.recycle()
        }
        val scaled = rotated.limitMaxSide(maxSide)
        if (scaled !== rotated) {
            rotated.recycle()
        }
        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 92, stream)
        val imageBase64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        PreparedNutritionImage(bitmap = scaled, base64 = imageBase64)
    } catch (_: Exception) {
        decoded.recycle()
        null
    }
}

internal fun Bitmap.limitMaxSide(maxSide: Int): Bitmap {
    val currentMax = maxOf(width, height)
    if (currentMax <= maxSide) return this
    val factor = maxSide.toFloat() / currentMax.toFloat()
    val targetW = (width * factor).toInt().coerceAtLeast(1)
    val targetH = (height * factor).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, targetW, targetH, true)
}

private fun Bitmap.rotateByExifOrientation(orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.preScale(-1f, 1f)
            matrix.postRotate(270f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.preScale(-1f, 1f)
            matrix.postRotate(90f)
        }
        else -> return this
    }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
