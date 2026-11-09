package com.sekaiforlyok

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import java.io.File

// En escritorio los datos van a la carpeta del usuario, por ejemplo C:\Users\Nombre\SekaiForlyok
actual fun carpetaDatos(): String =
    File(System.getProperty("user.home"), "SekaiForlyok").path

actual fun bytesAImagen(bytes: ByteArray): ImageBitmap? =
    runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()
