package com.sekaiforlyok.viewmodel

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import kotlin.math.floor
import kotlin.math.sqrt

// Generador de terreno sencillo.
// Idea: para cada punto se calcula una "altura" con ruido (valores aleatorios suavizados).
// Según la altura se pinta agua, playa, llanura, bosque, montaña o nieve.
// La misma semilla siempre da el mismo mapa, por eso basta con guardar la semilla.

const val ANCHO_TERRENO = 360
const val ALTO_TERRENO = 240

/** Crea la imagen del terreno para una semilla. */
fun generarTerreno(semilla: Long): ImageBitmap {
    val imagen = ImageBitmap(ANCHO_TERRENO, ALTO_TERRENO)
    val lienzo = Canvas(imagen)
    val pincel = Paint()
    for (y in 0 until ALTO_TERRENO) {
        for (x in 0 until ANCHO_TERRENO) {
            pincel.color = colorTerreno(alturaTerreno(semilla, x, y))
            lienzo.drawRect(Rect(x.toFloat(), y.toFloat(), x + 1f, y + 1f), pincel)
        }
    }
    return imagen
}

/** Altura de un punto: más o menos entre 0 (fondo del mar) y 1 (cima de montaña). */
fun alturaTerreno(semilla: Long, x: Int, y: Int): Float {
    // Suma de varias capas de ruido: la primera da los continentes y las siguientes los detalles
    var altura = 0f
    var escala = 80f      // Tamaño de las "manchas" de la capa (más grande = formas más amplias)
    var peso = 0.5f
    repeat(6) { capa ->
        altura += peso * ruidoSuave(semilla + capa, x / escala, y / escala)
        escala /= 2f
        peso /= 2f
    }
    // La suma de capas se queda cerca de 0.5; la estiramos para tener más variedad de alturas
    altura = 0.5f + (altura - 0.5f) * 2.2f
    // Bajamos los bordes para que el mapa quede rodeado de mar
    val dx = (x - ANCHO_TERRENO / 2f) / (ANCHO_TERRENO / 2f)
    val dy = (y - ALTO_TERRENO / 2f) / (ALTO_TERRENO / 2f)
    val distanciaAlCentro = sqrt(dx * dx + dy * dy) // 0 en el centro, ~1 en los bordes
    return altura + 0.08f - 0.4f * distanciaAlCentro * distanciaAlCentro
}

/** Color según la altura. */
fun colorTerreno(altura: Float): Color = when {
    altura < 0.30f -> Color(0xFF1E4E79) // Mar profundo
    altura < 0.38f -> Color(0xFF2F74B5) // Mar
    altura < 0.40f -> Color(0xFFE8D9A0) // Playa
    altura < 0.52f -> Color(0xFF8DBF5A) // Llanura
    altura < 0.64f -> Color(0xFF3F7A35) // Bosque
    altura < 0.86f -> Color(0xFF8A7A66) // Montaña
    else -> Color(0xFFF2F2F2)           // Nieve
}

/** Ruido suave: mezcla los valores aleatorios de las 4 esquinas de la casilla donde cae el punto. */
private fun ruidoSuave(semilla: Long, x: Float, y: Float): Float {
    val x0 = floor(x).toInt()
    val y0 = floor(y).toInt()
    // Cuánto nos hemos movido dentro de la casilla (0..1), suavizado para evitar bordes rectos
    val fx = suavizar(x - x0)
    val fy = suavizar(y - y0)
    val arriba = mezclar(azar(semilla, x0, y0), azar(semilla, x0 + 1, y0), fx)
    val abajo = mezclar(azar(semilla, x0, y0 + 1), azar(semilla, x0 + 1, y0 + 1), fx)
    return mezclar(arriba, abajo, fy)
}

private fun suavizar(t: Float) = t * t * (3 - 2 * t)
private fun mezclar(a: Float, b: Float, t: Float) = a + (b - a) * t

/** Número "aleatorio" entre 0 y 1 que siempre es el mismo para la misma casilla y semilla. */
private fun azar(semilla: Long, x: Int, y: Int): Float {
    var h = semilla * 6364136223846793005L + x * 374761393L + y * 668265263L
    h = (h xor (h ushr 33)) * -0xae502812aa7333L
    h = h xor (h ushr 29)
    return ((h ushr 11).toDouble() / (1L shl 53).toDouble()).toFloat()
}
