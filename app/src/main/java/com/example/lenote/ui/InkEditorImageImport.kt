package com.example.lenote.ui

import android.content.ActivityNotFoundException
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import com.example.lenote.ink.InkingState
import java.io.File
import java.io.FileOutputStream

@Composable
internal fun rememberImageImportAction(
    viewModel: InkPlaygroundViewModel?,
    viewportSize: IntSize,
    engineState: InkingState
): () -> Unit {
    val context = LocalContext.current

    fun importImage(uri: android.net.Uri) {
        val imagesDir = File(context.filesDir, "images").apply { mkdirs() }
        val targetFile = File(imagesDir, "img_${System.currentTimeMillis()}.png")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use(input::copyTo)
            } ?: error("Não foi possível abrir a imagem selecionada")
            check(targetFile.length() > 0L) { "A imagem selecionada está vazia" }

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(targetFile.absolutePath, bounds)
            check(bounds.outWidth > 0 && bounds.outHeight > 0) { "Formato de imagem inválido" }

            val maxSide = 360f
            val aspect = bounds.outWidth.toFloat() / bounds.outHeight
            val (width, height) = if (aspect >= 1f) {
                maxSide to (maxSide / aspect).coerceAtLeast(80f)
            } else {
                (maxSide * aspect).coerceAtLeast(80f) to maxSide
            }
            val origin = visibleImageOrigin(
                viewportSize = viewportSize,
                zoom = engineState.zoomLevel,
                panX = engineState.panOffsetX,
                panY = engineState.panOffsetY,
                imageWidth = width,
                imageHeight = height
            )
            check(viewModel?.addImageBlock(targetFile, origin.x, origin.y, width, height) == true) {
                "Nenhuma página ativa para inserir a imagem"
            }
        } catch (exception: Exception) {
            android.util.Log.e("LeNoteImage", "Falha ao adicionar imagem: ${exception.message}", exception)
            targetFile.delete()
            Toast.makeText(context, "Não foi possível adicionar a imagem.", Toast.LENGTH_SHORT).show()
        }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(::importImage)
    }
    val legacyPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(::importImage)
    }

    return {
        try {
            if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)) {
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } else {
                legacyPicker.launch("image/*")
            }
        } catch (_: ActivityNotFoundException) {
            try {
                legacyPicker.launch("image/*")
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, "Nenhum seletor de imagens disponível.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
