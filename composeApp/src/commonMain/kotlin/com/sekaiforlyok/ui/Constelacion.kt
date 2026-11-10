package com.sekaiforlyok.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

// Vista de "constelación": círculos sobre un cielo estrellado unidos por líneas.
// La usan la pantalla de mundos (mundos enlazados) y la pestaña Relaciones (personajes).

/** Un círculo de la constelación. [x] va de 0 a 1 a lo ancho; [y] son dp desde arriba. null = posición automática. */
data class NodoConstelacion(val id: String, val nombre: String, val x: Float?, val y: Float?)

/** Una línea entre dos círculos. */
data class EnlaceConstelacion(val desde: String, val hasta: String, val color: Color)

private val TAM_NODO = 76.dp       // Diámetro del círculo
private val ALTO_NODO = 116.dp     // Círculo + nombre debajo
private val ANCHO_NODO = 110.dp
private val SEPARACION_FILAS = 150.dp
private val COLOR_CIELO = Color(0xFF110D1F)

/**
 * Dibuja la constelación. Los círculos se pueden arrastrar ([alMover] guarda la nueva posición)
 * y tocar ([alPulsar]). [contenido] dibuja lo de dentro de cada círculo.
 */
@Composable
fun Constelacion(
    nodos: List<NodoConstelacion>,
    enlaces: List<EnlaceConstelacion>,
    alMover: (id: String, x: Float, y: Float) -> Unit,
    alPulsar: (id: String) -> Unit,
    modifier: Modifier = Modifier,
    alPulsarEnlace: ((indice: Int) -> Unit)? = null, // null = las líneas no se pueden tocar
    contenido: @Composable (NodoConstelacion) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize().background(COLOR_CIELO)) {
        val anchoDp = maxWidth
        // Posiciones automáticas en zigzag (como un orden de lectura) para los que no se han movido
        val automaticas = remember(nodos.map { it.id }) { posicionesZigzag(nodos.size) }
        // Posiciones en dp mientras se arrastra; se rehacen cuando cambian los datos guardados
        val posiciones = remember(nodos, anchoDp) {
            mutableStateMapOf<String, Offset>().apply {
                nodos.forEachIndexed { i, n ->
                    val (ax, ay) = automaticas[i]
                    put(n.id, Offset((n.x ?: ax) * anchoDp.value, n.y ?: ay))
                }
            }
        }
        // El lienzo crece hacia abajo si hay círculos más abajo de lo que cabe en pantalla
        val altoDp = max(maxHeight.value, (posiciones.values.maxOfOrNull { it.y } ?: 0f) + ALTO_NODO.value + 40f)
        // Los gestos viven más que una pantalla dibujada: así siempre llaman a la versión actual
        val alMoverActual by rememberUpdatedState(alMover)
        val alPulsarActual by rememberUpdatedState(alPulsar)
        val alPulsarEnlaceActual by rememberUpdatedState(alPulsarEnlace)
        val enlacesActuales by rememberUpdatedState(enlaces)

        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(
                Modifier.fillMaxWidth().height(altoDp.dp)
                    // Tocar el cielo: busca la línea más cercana (los toques en un círculo no llegan aquí)
                    .pointerInput(posiciones) {
                        detectTapGestures { toque ->
                            val elegir = alPulsarEnlaceActual ?: return@detectTapGestures
                            val radio = TAM_NODO.toPx() / 2
                            val cercano = enlacesActuales.withIndex().mapNotNull { (i, e) ->
                                val a = posiciones[e.desde] ?: return@mapNotNull null
                                val b = posiciones[e.hasta] ?: return@mapNotNull null
                                i to distanciaASegmento(
                                    toque,
                                    Offset(a.x * density, a.y * density + radio),
                                    Offset(b.x * density, b.y * density + radio),
                                )
                            }.minByOrNull { it.second }
                            if (cercano != null && cercano.second < 24.dp.toPx()) elegir(cercano.first)
                        }
                    },
            ) {
                // Fondo: estrellas y líneas entre los círculos
                Canvas(Modifier.fillMaxSize()) {
                    val estrellas = Random(7)
                    repeat((size.width * size.height / 9000f).toInt().coerceAtMost(600)) {
                        drawCircle(
                            Color.White.copy(alpha = 0.15f + estrellas.nextFloat() * 0.5f),
                            radius = 0.6f + estrellas.nextFloat() * 1.4f,
                            center = Offset(estrellas.nextFloat() * size.width, estrellas.nextFloat() * size.height),
                        )
                    }
                    val radio = TAM_NODO.toPx() / 2
                    enlaces.forEach { e ->
                        val a = posiciones[e.desde] ?: return@forEach
                        val b = posiciones[e.hasta] ?: return@forEach
                        // El centro del círculo está medio círculo por debajo de su esquina superior
                        val pa = Offset(a.x * density, a.y * density + radio)
                        val pb = Offset(b.x * density, b.y * density + radio)
                        drawLine(e.color.copy(alpha = 0.25f), pa, pb, strokeWidth = 8f, cap = StrokeCap.Round) // Brillo
                        drawLine(e.color, pa, pb, strokeWidth = 2.5f, cap = StrokeCap.Round)
                    }
                }
                nodos.forEach { nodo ->
                    val pos = posiciones[nodo.id] ?: return@forEach
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            // La posición guardada es el centro horizontal y el borde de arriba del círculo
                            .offset { IntOffset(((pos.x - ANCHO_NODO.value / 2) * density).roundToInt(), (pos.y * density).roundToInt()) }
                            .width(ANCHO_NODO)
                            .pointerInput(nodo.id) { detectTapGestures { alPulsarActual(nodo.id) } }
                            // "posiciones" en la clave: al guardar se crea un mapa nuevo y el gesto tiene que usar ese
                            .pointerInput(nodo.id, anchoDp, posiciones) {
                                detectDragGestures(
                                    onDragEnd = {
                                        val p = posiciones[nodo.id] ?: return@detectDragGestures
                                        alMoverActual(nodo.id, p.x / anchoDp.value, p.y)
                                    },
                                ) { cambio, arrastre ->
                                    cambio.consume()
                                    val p = posiciones[nodo.id] ?: return@detectDragGestures
                                    // Se limita al ancho de la pantalla y a no salirse por arriba
                                    posiciones[nodo.id] = Offset(
                                        (p.x + arrastre.x / density).coerceIn(ANCHO_NODO.value / 2, anchoDp.value - ANCHO_NODO.value / 2),
                                        (p.y + arrastre.y / density).coerceAtLeast(8f),
                                    )
                                }
                            },
                    ) {
                        Box(Modifier.width(TAM_NODO).height(TAM_NODO), contentAlignment = Alignment.Center) { contenido(nodo) }
                        Text(
                            nodo.nombre,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Posiciones automáticas: tres columnas en zigzag, bajando fila a fila. x de 0 a 1, y en dp. */
private fun posicionesZigzag(cantidad: Int): List<Pair<Float, Float>> = List(cantidad) { i ->
    val fila = i / 3
    val columna = if (fila % 2 == 0) i % 3 else 2 - i % 3 // En las filas impares se vuelve hacia atrás
    val x = 0.2f + columna * 0.3f
    val y = 24f + fila * SEPARACION_FILAS.value + (if (columna == 1) 40f else 0f) // El del medio, un poco más abajo
    x to y
}

/** Distancia (en píxeles) del punto [p] al segmento que va de [a] a [b]. */
internal fun distanciaASegmento(p: Offset, a: Offset, b: Offset): Float {
    val ab = b - a
    val largo2 = ab.x * ab.x + ab.y * ab.y
    // Proyección del punto sobre la línea, limitada a los extremos del segmento
    val t = if (largo2 == 0f) 0f else (((p - a).x * ab.x + (p - a).y * ab.y) / largo2).coerceIn(0f, 1f)
    return (p - (a + ab * t)).getDistance()
}

/** Qué hace tocar la constelación: ver/abrir, unir dos círculos o borrar una línea. */
enum class ModoConstelacion { VER, UNIR, BORRAR }

/**
 * Botones "Unir" y "Borrar línea" (pulsar otra vez para salir del modo).
 * [puedeUnir] es false si no hay al menos dos círculos; entonces se llama a [alNoPoderUnir].
 */
@Composable
fun BotonesModo(
    modo: ModoConstelacion,
    puedeUnir: Boolean,
    alCambiar: (ModoConstelacion) -> Unit,
    alNoPoderUnir: () -> Unit = {},
) {
    val t = LocalTextos.current
    FilterChip(modo == ModoConstelacion.UNIR, onClick = {
        when {
            !puedeUnir -> alNoPoderUnir()
            else -> alCambiar(if (modo == ModoConstelacion.UNIR) ModoConstelacion.VER else ModoConstelacion.UNIR)
        }
    }, label = { Text(t.unir) })
    FilterChip(modo == ModoConstelacion.BORRAR, onClick = {
        alCambiar(if (modo == ModoConstelacion.BORRAR) ModoConstelacion.VER else ModoConstelacion.BORRAR)
    }, label = { Text(t.borrarLinea) })
}

/** Instrucción del modo actual ("Toca el primer…", "Toca la línea…"). Nada en modo VER. */
@Composable
fun TextoModo(modo: ModoConstelacion, hayPrimero: Boolean) {
    val t = LocalTextos.current
    val texto = when (modo) {
        ModoConstelacion.UNIR -> if (hayPrimero) t.eligeSegundoCirculo else t.eligePrimerCirculo
        ModoConstelacion.BORRAR -> t.tocaUnaLinea
        ModoConstelacion.VER -> return
    }
    Text(texto, Modifier.padding(horizontal = 12.dp))
}
