package com.sekaiforlyok.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sekaiforlyok.model.Mundo
import com.sekaiforlyok.viewmodel.MundosViewModel
import com.sekaiforlyok.viewmodel.parejasEnlazadas

private val COLOR_ENLACE = Color(0xFFE9C46A) // Líneas doradas entre mundos

/** Pantalla con todos los mundos como constelación. Los enlaces se hacen con "Unir" o al editar un mundo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaConstelacion(vm: MundosViewModel, alVolver: () -> Unit, alAbrirMundo: (String) -> Unit) {
    val t = LocalTextos.current
    val mundos by vm.mundos.collectAsState()
    var modo by remember { mutableStateOf(ModoConstelacion.VER) }
    var primero by remember { mutableStateOf<String?>(null) }                 // Primer mundo elegido al unir
    var borrando by remember { mutableStateOf<Pair<String, String>?>(null) } // Enlace a punto de borrarse
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t.constelacion) },
                navigationIcon = { IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, t.volver) } },
            )
        },
    ) { margen ->
        Column(Modifier.padding(margen).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (mundos.isEmpty()) t.sinMundos else t.ayudaConstelacion,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                BotonesModo(modo, puedeUnir = mundos.size >= 2, alCambiar = { modo = it; primero = null })
            }
            TextoModo(modo, hayPrimero = primero != null)
            val parejas = mundos.parejasEnlazadas()
            Constelacion(
                nodos = mundos.map { NodoConstelacion(it.id, it.nombre, it.posX, it.posY) },
                enlaces = parejas.map { (a, b) -> EnlaceConstelacion(a, b, COLOR_ENLACE) },
                alMover = vm::moverEnConstelacion,
                alPulsar = { id ->
                    when {
                        modo != ModoConstelacion.UNIR -> alAbrirMundo(id)
                        primero == null -> primero = id
                        primero == id -> primero = null // Tocar el mismo otra vez lo desmarca
                        else -> { vm.enlazar(primero!!, id, unir = true); primero = null; modo = ModoConstelacion.VER }
                    }
                },
                alPulsarEnlace = if (modo == ModoConstelacion.BORRAR) { i -> borrando = parejas[i]; modo = ModoConstelacion.VER } else null,
            ) { nodo ->
                mundos.find { it.id == nodo.id }?.let {
                    // El círculo elegido al unir lleva un aro blanco
                    val aro = if (nodo.id == primero) Modifier.border(4.dp, Color.White, CircleShape).padding(4.dp) else Modifier
                    Box(aro) { CirculoMundo(it, vm) }
                }
            }
        }
    }

    borrando?.let { (a, b) ->
        fun nombre(id: String) = mundos.find { it.id == id }?.nombre ?: "?"
        ConfirmarBorrado(
            nombre = "${nombre(a)} ↔ ${nombre(b)}",
            alConfirmar = { vm.enlazar(a, b, unir = false); borrando = null },
            alCancelar = { borrando = null },
        )
    }
}

/** Círculo de un mundo: la miniatura de su mapa o, si no tiene, su inicial sobre morado. */
@Composable
private fun CirculoMundo(mundo: Mundo, vm: MundosViewModel) {
    val imagen = remember(mundo.rutaMapa, mundo.semillaMapa) { vm.imagenMapa(mundo) }
    val forma = Modifier.fillMaxSize().clip(CircleShape).border(3.dp, COLOR_ENLACE, CircleShape)
    if (imagen != null) {
        Image(imagen, contentDescription = mundo.nombre, contentScale = ContentScale.Crop, modifier = forma)
    } else {
        Box(forma.background(Color(0xFF3C3489)), contentAlignment = Alignment.Center) {
            Text(mundo.nombre.take(1).uppercase(), color = COLOR_ENLACE, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        }
    }
}
