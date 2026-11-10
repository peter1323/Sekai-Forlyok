package com.sekaiforlyok.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sekaiforlyok.model.Lugar
import com.sekaiforlyok.model.Mundo
import com.sekaiforlyok.viewmodel.MundoViewModel
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlin.math.roundToInt

private const val ZOOM_MAXIMO = 6f

/** Pestaña del mapa: cargar o generar un mapa, verlo con zoom y colocar lugares con pines. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PestanaMapa(mundo: Mundo, vm: MundoViewModel) {
    val t = LocalTextos.current
    // La imagen solo se vuelve a cargar cuando cambia el mapa
    val imagen = remember(mundo.rutaMapa, mundo.semillaMapa) { vm.imagenMapa(mundo) }

    var verAyuda by remember { mutableStateOf(false) }
    var confirmarGenerar by remember { mutableStateOf(false) }
    var puntoElegido by remember { mutableStateOf<Offset?>(null) }   // Dónde se tocó el mapa (0..1)
    var lugarVisto by remember { mutableStateOf<Lugar?>(null) }       // Pin pulsado

    // Selector de archivos de FileKit: solo deja elegir imágenes JPG o PNG
    val selector = rememberFilePickerLauncher(type = FileKitType.File(listOf("png", "jpg", "jpeg"))) { archivo ->
        if (archivo != null) vm.ponerImagenMapa(archivo)
    }
    // Si ya hay un mapa, se pide confirmación antes de sustituirlo por uno generado
    val generar = { if (imagen != null) confirmarGenerar = true else vm.generarMapa() }

    Column(Modifier.fillMaxSize()) {
        if (imagen == null) {
            // Sin mapa todavía: explicamos las opciones
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                Card { Column(Modifier.padding(16.dp)) { TextoRecomendaciones() } }
                Spacer(Modifier.height(16.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { selector.launch() }) { Text(t.cargarImagen) }
                    OutlinedButton(onClick = generar) { Text(t.generarTerrenoEnApp) }
                }
            }
        } else {
            // Barra de botones encima del mapa
            var escala by remember { mutableFloatStateOf(1f) }
            var desplazamiento by remember { mutableStateOf(Offset.Zero) }
            val pines = mundo.lugares.filter { it.x != null && it.y != null }.map { PinMapa(it.id, it.nombre, it.x!!, it.y!!) }
            FlowRow(Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { selector.launch() }) { Text(t.cambiarImagen) }
                OutlinedButton(onClick = generar) { Text(t.generarTerreno) }
                IconButton(onClick = { escala = 1f; desplazamiento = Offset.Zero }) {
                    Icon(Icons.Default.CenterFocusStrong, t.centrarMapa)
                }
                IconButton(onClick = { verAyuda = true }) { Icon(Icons.Default.Info, t.ayuda) }
                BotonExportar(imagen, pines, mundo.nombre)
            }
            Text(
                t.ayudaMapa,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            VisorMapa(
                imagen = imagen,
                pines = pines,
                escala = escala,
                desplazamiento = desplazamiento,
                alMover = { nuevaEscala, nuevoDesplazamiento -> escala = nuevaEscala; desplazamiento = nuevoDesplazamiento },
                alTocarMapa = { puntoElegido = it },
                alTocarPin = { pin -> lugarVisto = mundo.lugares.find { it.id == pin.id } },
            )
        }
    }

    // Diálogo: elegir qué lugar colocar en el punto tocado
    puntoElegido?.let { punto ->
        AlertDialog(
            onDismissRequest = { puntoElegido = null },
            title = { Text(t.queLugarVaAqui) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (mundo.lugares.isEmpty()) Text(t.primeroCreaLugares)
                    mundo.lugares.sortedBy { it.nombre.lowercase() }.forEach { lugar ->
                        Text(
                            lugar.nombre + " (" + t.tiposLugar.getValue(lugar.tipo) + ")" + if (lugar.x != null) t.yaEnMapa else "",
                            modifier = Modifier.fillMaxWidth()
                                .clickable { vm.colocarEnMapa(lugar.id, punto.x, punto.y); puntoElegido = null }
                                .padding(vertical = 10.dp),
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { puntoElegido = null }) { Text(t.cancelar) } },
        )
    }

    // Diálogo: datos del lugar cuyo pin se ha tocado
    lugarVisto?.let { lugar ->
        val padre = mundo.lugares.find { it.id == lugar.padreId }
        AlertDialog(
            onDismissRequest = { lugarVisto = null },
            title = { Text(lugar.nombre) },
            text = {
                Column {
                    Text(t.tiposLugar.getValue(lugar.tipo), color = MaterialTheme.colorScheme.primary)
                    if (padre != null) Text(t.dentroDe(padre.nombre))
                    if (lugar.descripcion.isNotBlank()) Text(lugar.descripcion, Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = { TextButton(onClick = { lugarVisto = null }) { Text(t.cerrar) } },
            dismissButton = {
                TextButton(onClick = { vm.quitarDelMapa(lugar.id); lugarVisto = null }) {
                    Text(t.quitarDelMapa, color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }

    if (confirmarGenerar) {
        AlertDialog(
            onDismissRequest = { confirmarGenerar = false },
            title = { Text(t.generarNuevoTitulo) },
            text = { Text(t.generarNuevoAviso) },
            confirmButton = { TextButton(onClick = { vm.generarMapa(); confirmarGenerar = false }) { Text(t.generar) } },
            dismissButton = { TextButton(onClick = { confirmarGenerar = false }) { Text(t.cancelar) } },
        )
    }
    if (verAyuda) {
        AlertDialog(
            onDismissRequest = { verAyuda = false },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { TextoRecomendaciones() } },
            confirmButton = { TextButton(onClick = { verAyuda = false }) { Text(t.entendido) } },
        )
    }
}

/** Texto de ayuda con las formas recomendadas de crear un mapa. */
@Composable
private fun TextoRecomendaciones() {
    val t = LocalTextos.current
    Text(t.recomendacionTitulo, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    Text(t.recomendacionAzgaar)
    Text(t.recomendacionInkarnate)
    Text(t.recomendacionIa)
    Spacer(Modifier.height(8.dp))
    Text(
        t.recomendacionGenerar,
        style = MaterialTheme.typography.bodySmall,
    )
}

/** Un pin sobre una imagen: un lugar o un punto libre. x e y van de 0 a 1. */
data class PinMapa(val id: String, val nombre: String, val x: Float, val y: Float, val color: Color = Color(0xFFE53935))

/**
 * Muestra el mapa (o la imagen de un lugar) manteniendo su proporción, con zoom y desplazamiento, y dibuja los pines.
 * Los pines van dentro de la misma capa que la imagen, así se mueven y escalan con ella.
 */
@Composable
fun VisorMapa(
    imagen: ImageBitmap,
    pines: List<PinMapa>,
    escala: Float,
    desplazamiento: Offset,
    alMover: (Float, Offset) -> Unit,
    alTocarMapa: (Offset) -> Unit,
    alTocarPin: (PinMapa) -> Unit,
) {
    // Los gestos se quedan escuchando todo el rato; así siempre leen el zoom y la posición actuales
    val escalaActual by rememberUpdatedState(escala)
    val desplazamientoActual by rememberUpdatedState(desplazamiento)
    Box(
        Modifier.fillMaxSize().padding(8.dp).clipToBounds()
            // Pellizcar para hacer zoom y arrastrar para mover (dedos o ratón)
            .pointerInput(Unit) {
                detectTransformGestures { _, mover, zoom, _ ->
                    alMover((escalaActual * zoom).coerceIn(1f, ZOOM_MAXIMO), desplazamientoActual + mover)
                }
            }
            // Rueda del ratón (escritorio) para hacer zoom
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val evento = awaitPointerEvent()
                        if (evento.type == PointerEventType.Scroll) {
                            val rueda = evento.changes.first().scrollDelta.y
                            val factor = if (rueda > 0) 0.9f else 1.1f
                            alMover((escalaActual * factor).coerceIn(1f, ZOOM_MAXIMO), desplazamientoActual)
                            evento.changes.forEach { it.consume() }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(
            Modifier
                .aspectRatio(imagen.width.toFloat() / imagen.height) // Mantiene la proporción del mapa
                .graphicsLayer {
                    scaleX = escala; scaleY = escala
                    translationX = desplazamiento.x; translationY = desplazamiento.y
                }
                // Al tocar, convertimos la posición a valores de 0 a 1
                .pointerInput(Unit) {
                    detectTapGestures { pos -> alTocarMapa(Offset(pos.x / size.width, pos.y / size.height)) }
                },
        ) {
            Image(imagen, contentDescription = LocalTextos.current.mapa, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            val anchoPx = constraints.maxWidth
            val altoPx = constraints.maxHeight
            pines.forEach { pin -> Pin(pin, pin.x * anchoPx, pin.y * altoPx, escala) { alTocarPin(pin) } }
        }
    }
}

/** Pin rojo con el nombre encima. La punta del pin queda justo en (x, y). */
@Composable
private fun Pin(pin: PinMapa, x: Float, y: Float, escala: Float, alPulsar: () -> Unit) {
    val ancho = 140.dp
    val alto = 46.dp
    Box(
        Modifier
            .offset { IntOffset((x - ancho.toPx() / 2).roundToInt(), (y - alto.toPx()).roundToInt()) }
            .size(ancho, alto)
            // Contrarresta el zoom para que el pin no se haga gigante al acercarse
            .graphicsLayer {
                scaleX = 1 / escala; scaleY = 1 / escala
                transformOrigin = TransformOrigin(0.5f, 1f)
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = alPulsar)) {
            Text(
                pin.nombre,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.background(Color(0xAA000000), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp),
            )
            Icon(Icons.Default.LocationOn, contentDescription = pin.nombre, tint = pin.color, modifier = Modifier.size(28.dp))
        }
    }
}
