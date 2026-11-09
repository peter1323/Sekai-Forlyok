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
                    Button(onClick = { selector.launch() }) { Text("Cargar imagen (JPG o PNG)") }
                    OutlinedButton(onClick = generar) { Text("Generar terreno en la app") }
                }
            }
        } else {
            // Barra de botones encima del mapa
            var escala by remember { mutableFloatStateOf(1f) }
            var desplazamiento by remember { mutableStateOf(Offset.Zero) }
            FlowRow(Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { selector.launch() }) { Text("Cambiar imagen") }
                OutlinedButton(onClick = generar) { Text("Generar terreno") }
                IconButton(onClick = { escala = 1f; desplazamiento = Offset.Zero }) {
                    Icon(Icons.Default.CenterFocusStrong, "Centrar mapa")
                }
                IconButton(onClick = { verAyuda = true }) { Icon(Icons.Default.Info, "Ayuda") }
            }
            Text(
                "Toca el mapa para colocar un lugar. Pellizca o usa la rueda del ratón para hacer zoom.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            VisorMapa(
                imagen = imagen,
                lugares = mundo.lugares.filter { it.x != null && it.y != null },
                escala = escala,
                desplazamiento = desplazamiento,
                alMover = { nuevaEscala, nuevoDesplazamiento -> escala = nuevaEscala; desplazamiento = nuevoDesplazamiento },
                alTocarMapa = { puntoElegido = it },
                alTocarPin = { lugarVisto = it },
            )
        }
    }

    // Diálogo: elegir qué lugar colocar en el punto tocado
    puntoElegido?.let { punto ->
        AlertDialog(
            onDismissRequest = { puntoElegido = null },
            title = { Text("¿Qué lugar va aquí?") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (mundo.lugares.isEmpty()) Text("Primero crea lugares en la pestaña Lugares.")
                    mundo.lugares.sortedBy { it.nombre.lowercase() }.forEach { lugar ->
                        Text(
                            lugar.nombre + " (" + lugar.tipo.texto + ")" + if (lugar.x != null) " · ya está en el mapa" else "",
                            modifier = Modifier.fillMaxWidth()
                                .clickable { vm.colocarEnMapa(lugar.id, punto.x, punto.y); puntoElegido = null }
                                .padding(vertical = 10.dp),
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { puntoElegido = null }) { Text("Cancelar") } },
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
                    Text(lugar.tipo.texto, color = MaterialTheme.colorScheme.primary)
                    if (padre != null) Text("Dentro de: ${padre.nombre}")
                    if (lugar.descripcion.isNotBlank()) Text(lugar.descripcion, Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = { TextButton(onClick = { lugarVisto = null }) { Text("Cerrar") } },
            dismissButton = {
                TextButton(onClick = { vm.quitarDelMapa(lugar.id); lugarVisto = null }) {
                    Text("Quitar del mapa", color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }

    if (confirmarGenerar) {
        AlertDialog(
            onDismissRequest = { confirmarGenerar = false },
            title = { Text("¿Generar un terreno nuevo?") },
            text = { Text("El mapa actual se borrará. Los lugares colocados se quedarán en la misma posición.") },
            confirmButton = { TextButton(onClick = { vm.generarMapa(); confirmarGenerar = false }) { Text("Generar") } },
            dismissButton = { TextButton(onClick = { confirmarGenerar = false }) { Text("Cancelar") } },
        )
    }
    if (verAyuda) {
        AlertDialog(
            onDismissRequest = { verAyuda = false },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { TextoRecomendaciones() } },
            confirmButton = { TextButton(onClick = { verAyuda = false }) { Text("Entendido") } },
        )
    }
}

/** Texto de ayuda con las formas recomendadas de crear un mapa. */
@Composable
private fun TextoRecomendaciones() {
    Text("Crea tu mapa fuera de la app y tráelo en JPG o PNG (recomendado)", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    Text("• Azgaar's Fantasy Map Generator: gratis y sin límites.")
    Text("• Inkarnate (plan gratis): estilo dibujado a mano; máximo 3 mapas, 2K y solo uso personal.")
    Text("• Pídeselo a una IA: describe el terreno que quieres (continentes, montañas, ríos, estilo manhwa…), genera la imagen y cárgala aquí.")
    Spacer(Modifier.height(8.dp))
    Text(
        "También puedes generar un terreno sencillo dentro de la app (sin internet).",
        style = MaterialTheme.typography.bodySmall,
    )
}

/**
 * Muestra el mapa manteniendo su proporción, con zoom y desplazamiento, y dibuja los pines.
 * Los pines van dentro de la misma capa que la imagen, así se mueven y escalan con ella.
 */
@Composable
private fun VisorMapa(
    imagen: ImageBitmap,
    lugares: List<Lugar>,
    escala: Float,
    desplazamiento: Offset,
    alMover: (Float, Offset) -> Unit,
    alTocarMapa: (Offset) -> Unit,
    alTocarPin: (Lugar) -> Unit,
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
            Image(imagen, contentDescription = "Mapa", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            val anchoPx = constraints.maxWidth
            val altoPx = constraints.maxHeight
            lugares.forEach { lugar -> Pin(lugar, lugar.x!! * anchoPx, lugar.y!! * altoPx, escala) { alTocarPin(lugar) } }
        }
    }
}

/** Pin rojo con el nombre encima. La punta del pin queda justo en (x, y). */
@Composable
private fun Pin(lugar: Lugar, x: Float, y: Float, escala: Float, alPulsar: () -> Unit) {
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
                lugar.nombre,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.background(Color(0xAA000000), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp),
            )
            Icon(Icons.Default.LocationOn, contentDescription = lugar.nombre, tint = Color(0xFFE53935), modifier = Modifier.size(28.dp))
        }
    }
}
