package com.sekaiforlyok.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import com.sekaiforlyok.model.Accesorio
import com.sekaiforlyok.model.Apariencia
import com.sekaiforlyok.model.Orejas
import com.sekaiforlyok.model.Peinado
import com.sekaiforlyok.model.Ropa

// La "personita" chibi de cada personaje, dibujada con formas sencillas.
// Todo se dibuja en una cuadrícula de 100 x 140 unidades y luego se escala al tamaño real.

private const val ANCHO = 100f
private const val ALTO = 140f

// Colores que se pueden elegir en el editor
val COLORES_PIEL = listOf(
    0xFFFCE4D6, 0xFFF5D0B5, 0xFFE8B48F, 0xFFC68A5E, 0xFF8D5A3B, 0xFF5C3A24,
    // Pieles de fantasía: azul, verde, amarillo, gris, rojo, morado y blanco pálido
    0xFF7FB3E8, 0xFF8BC48A, 0xFFF2D46B, 0xFFA7A9AC, 0xFFD9675B, 0xFFA98BD6, 0xFFF4F4F8,
)
val COLORES_PELO = listOf(
    0xFF1B1B1B, 0xFF4A3020, 0xFF8B5A2B, 0xFFE8C872, 0xFFF2F2F2,
    0xFFC0392B, 0xFFE91E63, 0xFF8E44AD, 0xFF2E86DE, 0xFF27AE60,
)
val COLORES_OJOS = listOf(0xFF3E2723, 0xFF1565C0, 0xFF2E7D32, 0xFF5B3FA8, 0xFFC62828, 0xFFF9A825, 0xFF9E9E9E, 0xFF00ACC1)
val COLORES_ROPA = listOf(
    0xFF3C3489, 0xFF1B1B1B, 0xFFF2F2F2, 0xFFC0392B, 0xFF2E86DE,
    0xFF27AE60, 0xFFE9C46A, 0xFF8B5A2B, 0xFFE91E63, 0xFF607D8B,
)

/** Dibuja la personita de cuerpo entero. Si [soloCara] es true, se ve solo la cabeza (para los círculos). */
@Composable
fun Chibi(apariencia: Apariencia, modifier: Modifier = Modifier, soloCara: Boolean = false) {
    Canvas(modifier) {
        if (soloCara) {
            // Encuadre de la cabeza: de x 10 a 90 y de y 8 a 88 (un cuadrado de 80)
            val k = size.minDimension / 80f
            clipRect {
                scale(k, k, pivot = Offset.Zero) { translate(-10f, -8f) { dibujarChibi(apariencia) } }
            }
        } else {
            val k = minOf(size.width / ANCHO, size.height / ALTO)
            // Centrado dentro del espacio disponible
            translate((size.width - ANCHO * k) / 2, (size.height - ALTO * k) / 2) {
                scale(k, k, pivot = Offset.Zero) { dibujarChibi(apariencia) }
            }
        }
    }
}

private fun DrawScope.dibujarChibi(a: Apariencia) {
    val piel = Color(a.piel)
    val pelo = Color(a.pelo)
    val ropa = Color(a.colorRopa)
    val oscuro = Color(0xFF2B2233)
    val borde = Stroke(width = 1.6f)

    // 1. Lo que va detrás de todo: pelo largo, coletas y capa
    when (a.peinado) {
        Peinado.LARGO -> drawRoundRect(pelo, Offset(17f, 40f), Size(66f, 72f), CornerRadius(18f))
        Peinado.MELENA -> drawRoundRect(pelo, Offset(17f, 38f), Size(66f, 46f), CornerRadius(16f))
        Peinado.COLETA -> drawOval(pelo, Offset(74f, 34f), Size(20f, 58f))
        Peinado.DOS_COLETAS -> {
            drawOval(pelo, Offset(4f, 40f), Size(20f, 50f))
            drawOval(pelo, Offset(76f, 40f), Size(20f, 50f))
        }
        else -> Unit
    }
    if (a.ropa == Ropa.CAPA) {
        // Más ancha que el cuerpo para que asome por los lados
        val capa = Path().apply { moveTo(30f, 84f); lineTo(70f, 84f); lineTo(88f, 132f); lineTo(12f, 132f); close() }
        drawPath(capa, ropa.copy(red = ropa.red * 0.7f, green = ropa.green * 0.7f, blue = ropa.blue * 0.7f))
    }

    // 2. Cuerpo: piernas, zapatos, brazos y torso
    drawRoundRect(oscuro, Offset(39f, 112f), Size(9f, 20f), CornerRadius(3f))
    drawRoundRect(oscuro, Offset(52f, 112f), Size(9f, 20f), CornerRadius(3f))
    drawOval(Color(0xFF4E342E), Offset(36f, 129f), Size(14f, 8f))
    drawOval(Color(0xFF4E342E), Offset(50f, 129f), Size(14f, 8f))
    drawRoundRect(piel, Offset(28f, 90f), Size(8f, 24f), CornerRadius(4f))   // Brazo izquierdo
    drawRoundRect(piel, Offset(64f, 90f), Size(8f, 24f), CornerRadius(4f))   // Brazo derecho
    drawRect(piel, Offset(45f, 78f), Size(10f, 12f))                          // Cuello
    val largoTorso = if (a.ropa == Ropa.TUNICA) 44f else 30f                 // La túnica llega a los pies
    drawRoundRect(ropa, Offset(33f, 86f), Size(34f, largoTorso), CornerRadius(8f))
    drawRoundRect(ropa, Offset(27f, 88f), Size(10f, 14f), CornerRadius(4f))  // Mangas
    drawRoundRect(ropa, Offset(63f, 88f), Size(10f, 14f), CornerRadius(4f))
    if (a.ropa == Ropa.ARMADURA) {
        val metal = Color(0xFFB0BEC5)
        drawOval(metal, Offset(24f, 84f), Size(18f, 12f))                    // Hombreras
        drawOval(metal, Offset(58f, 84f), Size(18f, 12f))
        drawRoundRect(metal, Offset(40f, 92f), Size(20f, 16f), CornerRadius(4f)) // Peto
    }
    if (a.ropa == Ropa.TUNICA) drawRect(Color(0xFFE9C46A), Offset(33f, 104f), Size(34f, 4f)) // Cinturón

    // 3. Cabeza: orejas, cara, ojos, mejillas y boca
    when (a.orejas) {
        Orejas.HUMANAS -> {
            drawCircle(piel, 6f, Offset(20f, 54f))
            drawCircle(piel, 6f, Offset(80f, 54f))
        }
        Orejas.PUNTIAGUDAS -> {
            // Largas y hacia fuera, como las de un elfo o un duende
            drawPath(Path().apply { moveTo(24f, 48f); lineTo(2f, 34f); lineTo(24f, 62f); close() }, piel)
            drawPath(Path().apply { moveTo(76f, 48f); lineTo(98f, 34f); lineTo(76f, 62f); close() }, piel)
        }
        Orejas.CONEJO -> listOf(30f, 58f).forEach { x ->
            drawOval(pelo, Offset(x, 0f), Size(12f, 34f))
            drawOval(Color(0xFFF8BBD0), Offset(x + 3f, 5f), Size(6f, 24f))
        }
        Orejas.GATO -> Unit // Van encima del pelo (más abajo)
    }
    drawCircle(piel, 30f, Offset(50f, 50f))
    drawCircle(oscuro.copy(alpha = 0.25f), 30f, Offset(50f, 50f), style = borde)
    listOf(39f, 61f).forEach { x ->
        drawOval(Color.White, Offset(x - 5f, 52f), Size(10f, 13f))
        drawOval(Color(a.ojos), Offset(x - 4f, 53f), Size(8f, 12f))
        drawCircle(oscuro, 2.2f, Offset(x, 59.5f))
        drawCircle(Color.White, 1.6f, Offset(x + 1.5f, 56f))
    }
    drawOval(Color(0x55FF7A8A), Offset(28f, 64f), Size(9f, 5f))
    drawOval(Color(0x55FF7A8A), Offset(63f, 64f), Size(9f, 5f))
    drawArc(oscuro, 20f, 140f, false, Offset(45f, 64f), Size(10f, 8f), style = Stroke(1.6f))

    // 4. Pelo de delante (el casco de la cabeza y el flequillo)
    if (a.peinado != Peinado.CALVO) {
        drawArc(pelo, 180f, 180f, true, Offset(18f, 17f), Size(64f, 64f))
        val flequillo = Path().apply {
            moveTo(19f, 50f)
            // Mechones en forma de picos a lo ancho de la frente
            val picos = if (a.peinado == Peinado.MELENA) 1 else 5
            val paso = 62f / picos
            for (i in 0 until picos) {
                val x0 = 19f + i * paso
                lineTo(x0 + paso / 2, if (picos == 1) 47f else 55f)
                lineTo(x0 + paso, 47f)
            }
            lineTo(81f, 40f); lineTo(19f, 40f); close()
        }
        drawPath(flequillo, pelo)
    }
    when (a.peinado) {
        Peinado.PUNTAS -> {
            val puntas = Path().apply {
                moveTo(20f, 34f); lineTo(14f, 14f); lineTo(32f, 24f); lineTo(38f, 4f); lineTo(50f, 20f)
                lineTo(62f, 4f); lineTo(68f, 24f); lineTo(86f, 14f); lineTo(80f, 34f); close()
            }
            drawPath(puntas, pelo)
        }
        Peinado.MONO -> drawCircle(pelo, 11f, Offset(50f, 16f))
        Peinado.COLETA -> drawCircle(Color(0xFFE53935), 3.5f, Offset(80f, 36f)) // Goma de la coleta
        else -> Unit
    }

    if (a.orejas == Orejas.GATO) {
        drawPath(Path().apply { moveTo(20f, 34f); lineTo(24f, 6f); lineTo(44f, 22f); close() }, pelo)
        drawPath(Path().apply { moveTo(80f, 34f); lineTo(76f, 6f); lineTo(56f, 22f); close() }, pelo)
        drawPath(Path().apply { moveTo(25f, 28f); lineTo(27f, 13f); lineTo(38f, 22f); close() }, Color(0xFFF8BBD0))
        drawPath(Path().apply { moveTo(75f, 28f); lineTo(73f, 13f); lineTo(62f, 22f); close() }, Color(0xFFF8BBD0))
    }

    // 5. Accesorio
    when (a.accesorio) {
        Accesorio.GAFAS -> {
            drawCircle(oscuro, 8f, Offset(39f, 58f), style = Stroke(1.8f))
            drawCircle(oscuro, 8f, Offset(61f, 58f), style = Stroke(1.8f))
            drawLine(oscuro, Offset(47f, 58f), Offset(53f, 58f), 1.8f)
        }
        Accesorio.CORONA -> {
            val corona = Path().apply {
                moveTo(34f, 24f); lineTo(34f, 10f); lineTo(42f, 17f); lineTo(50f, 6f)
                lineTo(58f, 17f); lineTo(66f, 10f); lineTo(66f, 24f); close()
            }
            drawPath(corona, Color(0xFFE9C46A))
            drawCircle(Color(0xFFC0392B), 2f, Offset(50f, 18f))
        }
        Accesorio.CUERNOS -> {
            drawPath(Path().apply { moveTo(26f, 30f); lineTo(18f, 6f); lineTo(36f, 24f); close() }, Color(0xFF3E2723))
            drawPath(Path().apply { moveTo(74f, 30f); lineTo(82f, 6f); lineTo(64f, 24f); close() }, Color(0xFF3E2723))
        }
        Accesorio.PARCHE -> {
            drawLine(oscuro, Offset(20f, 46f), Offset(80f, 60f), 2f)
            drawOval(oscuro, Offset(53f, 51f), Size(16f, 15f))
        }
        Accesorio.CICATRIZ -> {
            drawLine(Color(0xFFB71C1C), Offset(33f, 48f), Offset(45f, 70f), 2f)
            listOf(53f, 58f, 63f).forEach { drawLine(Color(0xFFB71C1C), Offset(37f, it - 2f), Offset(43f, it - 5f), 1.4f) }
        }
        Accesorio.NINGUNO -> Unit
    }
}

/** Editor de la personita: vista previa y opciones de peinado, colores, ropa y accesorio. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorChibi(apariencia: Apariencia, alCambiar: (Apariencia) -> Unit) {
    val vista = @Composable {
        Chibi(
            apariencia,
            Modifier.size(120.dp, 168.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp),
        )
    }
    // Si hay sitio, la vista previa va al lado de las opciones para verla siempre mientras se cambian
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 460.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                vista()
                Column(Modifier.weight(1f)) { OpcionesChibi(apariencia, alCambiar) }
            }
        } else {
            Column {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { vista() }
                OpcionesChibi(apariencia, alCambiar)
            }
        }
    }
}

@Composable
private fun OpcionesChibi(apariencia: Apariencia, alCambiar: (Apariencia) -> Unit) {
    val t = LocalTextos.current
    Desplegable(t.peinado, Peinado.entries, apariencia.peinado, { t.peinados.getValue(it) }) { alCambiar(apariencia.copy(peinado = it)) }
    Muestras(t.colorPiel, COLORES_PIEL, apariencia.piel) { alCambiar(apariencia.copy(piel = it)) }
    Desplegable(t.orejas, Orejas.entries, apariencia.orejas, { t.tiposOrejas.getValue(it) }) { alCambiar(apariencia.copy(orejas = it)) }
    Muestras(t.colorPelo, COLORES_PELO, apariencia.pelo) { alCambiar(apariencia.copy(pelo = it)) }
    Muestras(t.colorOjos, COLORES_OJOS, apariencia.ojos) { alCambiar(apariencia.copy(ojos = it)) }
    Desplegable(t.ropa, Ropa.entries, apariencia.ropa, { t.ropas.getValue(it) }) { alCambiar(apariencia.copy(ropa = it)) }
    Muestras(t.colorRopa, COLORES_ROPA, apariencia.colorRopa) { alCambiar(apariencia.copy(colorRopa = it)) }
    Desplegable(t.accesorio, Accesorio.entries, apariencia.accesorio, { t.accesorios.getValue(it) }) { alCambiar(apariencia.copy(accesorio = it)) }
}

/** Fila de circulitos de color; el elegido lleva un borde. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Muestras(titulo: String, colores: List<Long>, elegido: Long, alElegir: (Long) -> Unit) {
    Text(titulo, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        colores.forEach { c ->
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(Color(c))
                    .border(if (c == elegido) 3.dp else 1.dp, if (c == elegido) MaterialTheme.colorScheme.primary else Color(0x33000000), CircleShape)
                    .clickable { alElegir(c) },
            )
        }
    }
}
