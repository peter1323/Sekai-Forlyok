package com.sekaiforlyok

import androidx.compose.ui.graphics.ImageBitmap

// "expect" = esta función existe en todas las plataformas, pero cada una la escribe a su manera.
// Las versiones reales ("actual") están en androidMain y desktopMain.

/** Carpeta donde la app guarda sus datos (el JSON y las imágenes de los mapas). */
expect fun carpetaDatos(): String

/** Convierte los bytes de un archivo JPG o PNG en una imagen que Compose puede dibujar. */
expect fun bytesAImagen(bytes: ByteArray): ImageBitmap?
