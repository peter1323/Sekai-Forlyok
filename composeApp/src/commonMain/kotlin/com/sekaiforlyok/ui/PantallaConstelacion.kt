package com.sekaiforlyok.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.remember
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

/** Pantalla con todos los mundos como constelación. Los enlaces se eligen al editar un mundo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaConstelacion(vm: MundosViewModel, alVolver: () -> Unit, alAbrirMundo: (String) -> Unit) {
    val t = LocalTextos.current
    val mundos by vm.mundos.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t.constelacion) },
                navigationIcon = { IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, t.volver) } },
            )
        },
    ) { margen ->
        Column(Modifier.padding(margen).fillMaxSize()) {
            Text(
                if (mundos.isEmpty()) t.sinMundos else t.ayudaConstelacion,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            Constelacion(
                nodos = mundos.map { NodoConstelacion(it.id, it.nombre, it.posX, it.posY) },
                enlaces = mundos.parejasEnlazadas().map { (a, b) -> EnlaceConstelacion(a, b, COLOR_ENLACE) },
                alMover = vm::moverEnConstelacion,
                alPulsar = alAbrirMundo,
            ) { nodo -> mundos.find { it.id == nodo.id }?.let { CirculoMundo(it, vm) } }
        }
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
