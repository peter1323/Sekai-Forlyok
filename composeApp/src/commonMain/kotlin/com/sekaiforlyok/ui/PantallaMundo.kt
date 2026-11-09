package com.sekaiforlyok.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sekaiforlyok.viewmodel.MundoViewModel

private val PESTANAS = listOf("Personajes", "Facciones", "Lugares", "Mapa", "Relaciones")

/** Pantalla 2: un mundo con sus 5 pestañas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaMundo(vm: MundoViewModel, alVolver: () -> Unit) {
    val mundo by vm.mundo.collectAsState()
    var pestana by rememberSaveable { mutableIntStateOf(0) }

    // Si el mundo ya no existe (por ejemplo, se borró), volvemos a la lista
    val actual = mundo
    if (actual == null) {
        LaunchedEffect(Unit) { alVolver() }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(actual.nombre) },
                navigationIcon = {
                    IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
                },
            )
        },
    ) { margen ->
        Column(Modifier.padding(margen).fillMaxSize()) {
            // Pestañas desplazables para que quepan en pantallas de móvil estrechas
            PrimaryScrollableTabRow(selectedTabIndex = pestana, edgePadding = 0.dp) {
                PESTANAS.forEachIndexed { i, titulo ->
                    Tab(selected = pestana == i, onClick = { pestana = i }, text = { Text(titulo) })
                }
            }
            when (pestana) {
                0 -> PestanaPersonajes(actual, vm)
                1 -> PestanaFacciones(actual, vm)
                2 -> PestanaLugares(actual, vm)
                3 -> PestanaMapa(actual, vm)
                4 -> PestanaRelaciones(actual, vm)
            }
        }
    }
}

/** Contenido de una pestaña con el botón + flotando abajo a la derecha. */
@Composable
fun ConBotonAnadir(descripcion: String, alPulsar: () -> Unit, contenido: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize()) {
        contenido()
        FloatingActionButton(
            onClick = alPulsar,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Icon(Icons.Default.Add, descripcion) }
    }
}
