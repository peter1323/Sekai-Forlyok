package com.sekaiforlyok.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import io.github.vinceglb.filekit.ImageFormat
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.dialogs.compose.util.encodeToByteArray
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max

// Exportar un mapa (o la imagen de un lugar) con sus pines y nombres como archivo PNG o JPG.

/** Botón de exportar: pregunta PNG o JPG y abre el "Guardar como" del sistema. */
@Composable
fun BotonExportar(imagen: ImageBitmap, pines: List<PinMapa>, nombreArchivo: String) {
    val t = LocalTextos.current
    val medidor = rememberTextMeasurer()
    val corrutinas = rememberCoroutineScope()
    var eligiendo by remember { mutableStateOf(false) }
    var formato by remember { mutableStateOf(ImageFormat.PNG) }

    val guardar = rememberFileSaverLauncher(dialogSettings = FileKitDialogSettings.createDefault()) { archivo ->
        if (archivo != null) corrutinas.launch {
            archivo.write(dibujarConPines(imagen, pines, medidor).encodeToByteArray(formato, 92))
        }
    }

    IconButton(onClick = { eligiendo = true }) { Icon(Icons.Default.SaveAlt, t.exportar) }
    if (eligiendo) {
        AlertDialog(
            onDismissRequest = { eligiendo = false },
            title = { Text(t.exportar) },
            text = { Text(t.exportarExplicacion) },
            confirmButton = {
                TextButton(onClick = { formato = ImageFormat.PNG; eligiendo = false; guardar.launch(nombreArchivo, "png") }) { Text("PNG") }
                TextButton(onClick = { formato = ImageFormat.JPEG; eligiendo = false; guardar.launch(nombreArchivo, "jpg") }) { Text("JPG") }
            },
            dismissButton = { TextButton(onClick = { eligiendo = false }) { Text(t.cancelar) } },
        )
    }
}

/**
 * Crea una imagen nueva con el mapa y, encima, cada pin con su nombre.
 * Los mapas pequeños (como el terreno generado, 360 px) se agrandan hasta unos 2000 px sin suavizar,
 * para que los nombres se lean bien.
 */
fun dibujarConPines(imagen: ImageBitmap, pines: List<PinMapa>, medidor: TextMeasurer): ImageBitmap {
    val aumento = ceil(2000f / max(imagen.width, imagen.height)).toInt().coerceAtLeast(1)
    val ancho = imagen.width * aumento
    val alto = imagen.height * aumento
    val salida = ImageBitmap(ancho, alto)
    val px = Density(1f) // 1 sp = 1 píxel, así los tamaños de abajo son píxeles de la imagen
    CanvasDrawScope().draw(px, LayoutDirection.Ltr, Canvas(salida), Size(ancho.toFloat(), alto.toFloat())) {
        drawImage(imagen, dstSize = IntSize(ancho, alto), filterQuality = if (aumento > 1) FilterQuality.None else FilterQuality.Low)
        val r = max(ancho, alto) / 110f // Radio de la cabeza del pin, proporcional a la imagen
        pines.forEach { pin ->
            val punta = Offset(pin.x * ancho, pin.y * alto)
            val cabeza = punta - Offset(0f, r * 2.2f)
            // Pin: triángulo que apunta al sitio, círculo de color y punto blanco
            val pico = Path().apply {
                moveTo(cabeza.x - r * 0.85f, cabeza.y + r * 0.5f)
                lineTo(cabeza.x + r * 0.85f, cabeza.y + r * 0.5f)
                lineTo(punta.x, punta.y); close()
            }
            drawPath(pico, pin.color)
            drawCircle(pin.color, r, cabeza)
            drawCircle(Color.White, r * 0.4f, cabeza)
            // Nombre encima, sobre una etiqueta oscura
            val texto = medidor.measure(
                pin.nombre,
                TextStyle(color = Color.White, fontSize = (r * 1.3f).sp, fontWeight = FontWeight.Bold),
                density = px,
            )
            val margen = r * 0.35f
            val arriba = Offset(cabeza.x - texto.size.width / 2f, cabeza.y - r - margen * 2 - texto.size.height)
            drawRoundRect(
                Color(0xAA000000),
                arriba - Offset(margen, margen / 2),
                Size(texto.size.width + margen * 2, texto.size.height + margen),
                CornerRadius(margen),
            )
            drawText(texto, topLeft = arriba)
        }
    }
    return salida
}
