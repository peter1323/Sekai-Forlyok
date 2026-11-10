package com.sekaiforlyok.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sekaiforlyok.model.Lugar
import com.sekaiforlyok.model.Mundo
import com.sekaiforlyok.viewmodel.MundoViewModel
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher

private val COLOR_SUBLUGAR = Color(0xFF42A5F5) // Pines azules: sublugares del árbol
private val COLOR_PUNTO = Color(0xFFE9C46A)    // Pines dorados: puntos libres

/**
 * Imagen propia de un lugar (una ciudad, una mazmorra…) con sus pines, como el mapa del mundo.
 * Se pueden colocar sus sublugares o puntos libres con nombre ("Plaza central").
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImagenLugar(lugar: Lugar, mundo: Mundo, vm: MundoViewModel, alVolver: () -> Unit) {
    val t = LocalTextos.current
    val imagen = remember(lugar.rutaImagen) { vm.imagenLugar(lugar) }
    val sublugares = mundo.lugares.filter { it.padreId == lugar.id }.sortedBy { it.nombre.lowercase() }

    var puntoElegido by remember { mutableStateOf<Offset?>(null) }   // Dónde se tocó la imagen (0..1)
    var pinVisto by remember { mutableStateOf<PinMapa?>(null) }
    var confirmarQuitar by remember { mutableStateOf(false) }

    val selector = rememberFilePickerLauncher(type = FileKitType.File(listOf("png", "jpg", "jpeg"))) { archivo ->
        if (archivo != null) vm.ponerImagenLugar(lugar.id, archivo)
    }

    Column(Modifier.fillMaxSize()) {
        // Cabecera: volver a la lista y nombre del lugar
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, t.volver) }
            Text(lugar.nombre, style = MaterialTheme.typography.titleLarge)
        }
        if (imagen == null) {
            Column(Modifier.padding(16.dp)) {
                Text(t.imagenLugarExplicacion)
                Button(onClick = { selector.launch() }, modifier = Modifier.padding(top = 16.dp)) { Text(t.anadirImagen) }
            }
            return@Column
        }
        var escala by remember { mutableFloatStateOf(1f) }
        var desplazamiento by remember { mutableStateOf(Offset.Zero) }
        // Pines: los sublugares colocados (azules) y los puntos libres (dorados)
        val pines = sublugares.filter { it.xEnPadre != null && it.yEnPadre != null }
            .map { PinMapa(it.id, it.nombre, it.xEnPadre!!, it.yEnPadre!!, COLOR_SUBLUGAR) } +
            lugar.puntos.map { PinMapa(it.id, it.nombre, it.x, it.y, COLOR_PUNTO) }
        FlowRow(Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { selector.launch() }) { Text(t.cambiarImagen) }
            OutlinedButton(onClick = { confirmarQuitar = true }) { Text(t.quitarImagen) }
            IconButton(onClick = { escala = 1f; desplazamiento = Offset.Zero }) {
                Icon(Icons.Default.CenterFocusStrong, t.centrarMapa)
            }
            BotonExportar(imagen, pines, lugar.nombre)
        }
        Text(t.ayudaImagenLugar, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 12.dp))
        VisorMapa(
            imagen = imagen,
            pines = pines,
            escala = escala,
            desplazamiento = desplazamiento,
            alMover = { nuevaEscala, nuevoDesplazamiento -> escala = nuevaEscala; desplazamiento = nuevoDesplazamiento },
            alTocarMapa = { puntoElegido = it },
            alTocarPin = { pinVisto = it },
        )
    }

    // Diálogo: colocar un sublugar o crear un punto libre donde se ha tocado
    puntoElegido?.let { punto ->
        var nombrePunto by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { puntoElegido = null },
            title = { Text(t.queHayAqui) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(t.nuevoPunto, style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            nombrePunto, { nombrePunto = it }, label = { Text(t.nombrePunto) },
                            singleLine = true, modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            enabled = nombrePunto.isNotBlank(),
                            onClick = { vm.anadirPunto(lugar.id, nombrePunto, punto.x, punto.y); puntoElegido = null },
                        ) { Text(t.anadir) }
                    }
                    Text(t.oColocaSublugar, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp))
                    if (sublugares.isEmpty()) Text(t.sinSublugares, style = MaterialTheme.typography.bodySmall)
                    sublugares.forEach { s ->
                        Text(
                            s.nombre + " (" + t.tiposLugar.getValue(s.tipo) + ")" + if (s.xEnPadre != null) t.yaEnMapa else "",
                            modifier = Modifier.fillMaxWidth()
                                .clickable { vm.colocarEnImagen(s.id, punto.x, punto.y); puntoElegido = null }
                                .padding(vertical = 10.dp),
                        )
                        HorizontalDivider()
                    }
                }
            },
            confirmButton = { TextButton(onClick = { puntoElegido = null }) { Text(t.cancelar) } },
        )
    }

    // Diálogo: pin pulsado. Un sublugar se quita de la imagen; un punto libre se borra.
    pinVisto?.let { pin ->
        val sublugar = sublugares.find { it.id == pin.id }
        AlertDialog(
            onDismissRequest = { pinVisto = null },
            title = { Text(pin.nombre) },
            text = {
                Column {
                    if (sublugar != null) {
                        Text(t.tiposLugar.getValue(sublugar.tipo), color = MaterialTheme.colorScheme.primary)
                        if (sublugar.descripcion.isNotBlank()) Text(sublugar.descripcion, Modifier.padding(top = 8.dp))
                    } else {
                        Text(t.puntoLibre, color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pinVisto = null }) { Text(t.cerrar) } },
            dismissButton = {
                TextButton(onClick = {
                    if (sublugar != null) vm.quitarDeImagen(sublugar.id) else vm.borrarPunto(lugar.id, pin.id)
                    pinVisto = null
                }) {
                    Text(if (sublugar != null) t.quitarDelMapa else t.borrar, color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }

    if (confirmarQuitar) {
        AlertDialog(
            onDismissRequest = { confirmarQuitar = false },
            title = { Text(t.quitarImagen) },
            text = { Text(t.avisoQuitarImagen) },
            confirmButton = {
                TextButton(onClick = { vm.quitarImagenLugar(lugar.id); confirmarQuitar = false }) {
                    Text(t.quitarImagen, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmarQuitar = false }) { Text(t.cancelar) } },
        )
    }
}
