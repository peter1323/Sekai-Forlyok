package com.sekaiforlyok

import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init

private lateinit var carpetaAndroid: String

/** Lo llama MainActivity al arrancar. */
fun iniciarAndroid(activity: ComponentActivity) {
    carpetaAndroid = activity.filesDir.path  // Carpeta privada de la app para guardar los datos
    FileKit.init(activity)                   // FileKit necesita la Activity para abrir el selector de archivos
}

actual fun carpetaDatos(): String = carpetaAndroid

actual fun bytesAImagen(bytes: ByteArray): ImageBitmap? =
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
