package de.ingomc.nozio.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val PROFILE_IMAGE_DIRECTORY = "profile"
private const val PROFILE_IMAGE_FILE_NAME = "avatar.jpg"
private const val PROFILE_IMAGE_SIZE_PX = 512
private const val PROFILE_IMAGE_JPEG_QUALITY = 86
private const val MAX_INPUT_IMAGE_SIDE_PX = 2048

internal data class ProfileImageCropSpec(
    val zoom: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
)

internal data class CropWindow(
    val left: Int,
    val top: Int,
    val size: Int
)

internal fun resolveStoredProfileImagePath(context: Context): String? {
    val file = profileImageFile(context)
    return file.takeIf { it.exists() }?.absolutePath
}

internal fun decodeProfileImageForEditing(context: Context, uri: Uri): Bitmap? {
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val width = info.size.width
                val height = info.size.height
                val maxSide = max(width, height)
                if (maxSide > MAX_INPUT_IMAGE_SIDE_PX) {
                    val scale = MAX_INPUT_IMAGE_SIDE_PX.toFloat() / maxSide.toFloat()
                    decoder.setTargetSize(
                        (width * scale).roundToInt().coerceAtLeast(1),
                        (height * scale).roundToInt().coerceAtLeast(1)
                    )
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            decodeLegacyBitmap(context, uri)
        }
    }.getOrNull()
}

internal fun saveProfileImageToStorage(
    context: Context,
    sourceBitmap: Bitmap,
    cropSpec: ProfileImageCropSpec
): Boolean {
    var optimizedBitmap: Bitmap? = null
    return runCatching {
        optimizedBitmap = cropAndOptimizeProfileImage(
            sourceBitmap = sourceBitmap,
            cropSpec = cropSpec,
            targetSizePx = PROFILE_IMAGE_SIZE_PX
        )
        val outputFile = profileImageFile(context).also { it.parentFile?.mkdirs() }
        FileOutputStream(outputFile).use { output ->
            val written = checkNotNull(optimizedBitmap).compress(
                Bitmap.CompressFormat.JPEG,
                PROFILE_IMAGE_JPEG_QUALITY,
                output
            )
            check(written)
        }
        true
    }.getOrElse { false }.also {
        optimizedBitmap?.recycle()
    }
}

internal fun computeProfileImageCropWindow(
    imageWidth: Int,
    imageHeight: Int,
    cropSpec: ProfileImageCropSpec
): CropWindow {
    val safeWidth = imageWidth.coerceAtLeast(1)
    val safeHeight = imageHeight.coerceAtLeast(1)
    val baseSize = min(safeWidth, safeHeight).toFloat()
    val zoom = cropSpec.zoom.coerceIn(1f, 3f)
    val cropSize = (baseSize / zoom)
        .roundToInt()
        .coerceIn(1, min(safeWidth, safeHeight))

    val maxLeft = (safeWidth - cropSize).coerceAtLeast(0)
    val maxTop = (safeHeight - cropSize).coerceAtLeast(0)
    val centeredLeft = maxLeft / 2f
    val centeredTop = maxTop / 2f
    val left = (
        centeredLeft +
            cropSpec.offsetX.coerceIn(-1f, 1f) * (maxLeft / 2f)
        )
        .roundToInt()
        .coerceIn(0, maxLeft)
    val top = (
        centeredTop +
            cropSpec.offsetY.coerceIn(-1f, 1f) * (maxTop / 2f)
        )
        .roundToInt()
        .coerceIn(0, maxTop)

    return CropWindow(left = left, top = top, size = cropSize)
}

internal fun cropAndOptimizeProfileImage(
    sourceBitmap: Bitmap,
    cropSpec: ProfileImageCropSpec,
    targetSizePx: Int = PROFILE_IMAGE_SIZE_PX
): Bitmap {
    val cropWindow = computeProfileImageCropWindow(
        imageWidth = sourceBitmap.width,
        imageHeight = sourceBitmap.height,
        cropSpec = cropSpec
    )
    val cropped = Bitmap.createBitmap(
        sourceBitmap,
        cropWindow.left,
        cropWindow.top,
        cropWindow.size,
        cropWindow.size
    )
    if (cropWindow.size == targetSizePx) return cropped
    return Bitmap.createScaledBitmap(cropped, targetSizePx, targetSizePx, true).also { scaled ->
        if (scaled !== cropped) {
            cropped.recycle()
        }
    }
}

private fun profileImageFile(context: Context): File {
    return File(context.filesDir, "$PROFILE_IMAGE_DIRECTORY/$PROFILE_IMAGE_FILE_NAME")
}

private fun decodeLegacyBitmap(context: Context, uri: Uri): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, bounds)
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val sampleSize = calculateSampleSize(
        width = bounds.outWidth,
        height = bounds.outHeight,
        maxSide = MAX_INPUT_IMAGE_SIDE_PX
    )
    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, options)
    }
}

private fun calculateSampleSize(width: Int, height: Int, maxSide: Int): Int {
    var sample = 1
    var currentWidth = width
    var currentHeight = height
    while (max(currentWidth, currentHeight) > maxSide) {
        sample *= 2
        currentWidth /= 2
        currentHeight /= 2
    }
    return sample.coerceAtLeast(1)
}
