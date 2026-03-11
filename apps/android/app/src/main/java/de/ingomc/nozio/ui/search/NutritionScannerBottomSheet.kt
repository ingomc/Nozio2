package de.ingomc.nozio.ui.search

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import de.ingomc.nozio.ui.theme.nozioColors
import java.io.ByteArrayOutputStream
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScannerBottomSheet(
    isAnalyzing: Boolean,
    onDismiss: () -> Unit,
    onImageBase64Captured: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var scanMessage by remember { mutableStateOf<String?>(null) }
    val hasFlash = boundCamera?.cameraInfo?.hasFlashUnit() == true

    DisposableEffect(Unit) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var cameraProvider: ProcessCameraProvider? = null

        providerFuture.addListener(
            {
                cameraProvider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()

                try {
                    cameraProvider?.unbindAll()
                    boundCamera = cameraProvider?.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        capture
                    )
                    imageCapture = capture
                    isTorchOn = false
                } catch (_: Exception) {
                    // Ignore binding errors and allow user to dismiss.
                }
            },
            mainExecutor
        )

        onDispose {
            boundCamera?.cameraControl?.enableTorch(false)
            cameraProvider?.unbindAll()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!isAnalyzing && !isProcessing) {
                onDismiss()
            }
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.nozioColors.surface2,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Naehrwerte scannen",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = null,
                        tint = if (hasFlash) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = isTorchOn,
                        onCheckedChange = { checked ->
                            boundCamera?.cameraControl?.enableTorch(checked)
                            isTorchOn = checked
                        },
                        enabled = hasFlash && !isProcessing && !isAnalyzing
                    )
                }
            }

            Text(
                text = "Mach ein scharfes Foto der Naehrwerttabelle. Analyse laeuft serverseitig.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .align(Alignment.CenterHorizontally)
                    .widthIn(max = 380.dp)
                    .aspectRatio(1f),
                color = MaterialTheme.nozioColors.baseBgElevated,
                shape = MaterialTheme.shapes.large
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isAnalyzing) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = "Bild wird analysiert...",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(top = 10.dp)
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val capture = imageCapture ?: return@Button
                    isProcessing = true
                    scanMessage = null
                    val outputFile = File(context.cacheDir, "nutrition_${System.currentTimeMillis()}.jpg")
                    val options = ImageCapture.OutputFileOptions.Builder(outputFile).build()

                    capture.takePicture(
                        options,
                        mainExecutor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                try {
                                    val decoded = BitmapFactory.decodeFile(outputFile.absolutePath)
                                    if (decoded == null) {
                                        scanMessage = "Foto konnte nicht gelesen werden."
                                        isProcessing = false
                                        return
                                    }
                                    val prepared = decoded.limitMaxSide(1800)
                                    val stream = ByteArrayOutputStream()
                                    prepared.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, stream)
                                    val imageBase64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
                                    isProcessing = false
                                    onImageBase64Captured(imageBase64)
                                } catch (_: Exception) {
                                    scanMessage = "Foto-Verarbeitung fehlgeschlagen."
                                    isProcessing = false
                                } finally {
                                    outputFile.delete()
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                scanMessage = "Fotoaufnahme fehlgeschlagen. Bitte erneut versuchen."
                                isProcessing = false
                            }
                        }
                    )
                },
                enabled = !isProcessing && !isAnalyzing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when {
                        isAnalyzing -> "Analyse laeuft..."
                        isProcessing -> "Lade Bild..."
                        else -> "Foto aufnehmen & analysieren"
                    }
                )
            }

            scanMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "Es werden nur Werte pro 100g/100ml uebernommen.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun android.graphics.Bitmap.limitMaxSide(maxSide: Int): android.graphics.Bitmap {
    val currentMax = maxOf(width, height)
    if (currentMax <= maxSide) return this
    val factor = maxSide.toFloat() / currentMax.toFloat()
    val targetW = (width * factor).toInt().coerceAtLeast(1)
    val targetH = (height * factor).toInt().coerceAtLeast(1)
    return android.graphics.Bitmap.createScaledBitmap(this, targetW, targetH, true)
}
