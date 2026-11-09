package com.sekaiforlyok.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sekaiforlyok.model.Mundo
import com.sekaiforlyok.viewmodel.MundosViewModel

/** Pantalla 1: lista de todos los mundos. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaMundos(vm: MundosViewModel, alAbrirMundo: (String) -> Unit) {
    val mundos by vm.mundos.collectAsState()

    // Estado de los diálogos: qué mundo se está editando o borrando (null = ninguno)
    var editando by remember { mutableStateOf<Mundo?>(null) }
    var creando by remember { mutableStateOf(false) }
    var borrando by remember { mutableStateOf<Mundo?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sekai Forlyok") },
                // Crea mundos de ejemplo para probar la app rápidamente
                actions = { TextButton(onClick = vm::cargarDatosDePrueba) { Text("Datos de prueba") } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { creando = true }) { Icon(Icons.Default.Add, "Crear mundo") }
        },
    ) { margen ->
        if (mundos.isEmpty()) {
            Column(Modifier.padding(margen)) {
                ListaVacia("Aún no tienes mundos. Pulsa + para crear el primero, o \"Datos de prueba\" arriba para ver ejemplos.")
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(margen),
            contentPadding = PaddingValues(12.dp, 12.dp, 12.dp, 88.dp), // Hueco abajo para el botón +
        ) {
            items(mundos, key = { it.id }) { mundo ->
                TarjetaMundo(
                    mundo = mundo,
                    alPulsar = { alAbrirMundo(mundo.id) },
                    alEditar = { editando = mundo },
                    alBorrar = { borrando = mundo },
                )
            }
        }
    }

    // Diálogo para crear o editar (es el mismo formulario)
    if (creando || editando != null) {
        DialogoMundo(
            mundo = editando,
            alGuardar = { nombre, descripcion ->
                vm.guardarMundo(editando?.id, nombre, descripcion)
                creando = false; editando = null
            },
            alCancelar = { creando = false; editando = null },
        )
    }

    borrando?.let { mundo ->
        ConfirmarBorrado(
            nombre = mundo.nombre,
            aviso = "Se borrarán todos sus personajes, facciones, lugares, relaciones y el mapa.",
            alConfirmar = { vm.borrarMundo(mundo.id); borrando = null },
            alCancelar = { borrando = null },
        )
    }
}

@Composable
private fun TarjetaMundo(mundo: Mundo, alPulsar: () -> Unit, alEditar: () -> Unit, alBorrar: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable(onClick = alPulsar)) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(mundo.nombre, style = MaterialTheme.typography.titleMedium)
                if (mundo.descripcion.isNotBlank()) {
                    Text(mundo.descripcion, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                }
                Text(
                    "${mundo.personajes.size} personajes · ${mundo.facciones.size} facciones · ${mundo.lugares.size} lugares",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BotonesEditarBorrar(alEditar, alBorrar)
        }
    }
}

/** Formulario de mundo: nombre y descripción. Si [mundo] es null, es un mundo nuevo. */
@Composable
private fun DialogoMundo(mundo: Mundo?, alGuardar: (String, String) -> Unit, alCancelar: () -> Unit) {
    var nombre by remember { mutableStateOf(mundo?.nombre ?: "") }
    var descripcion by remember { mutableStateOf(mundo?.descripcion ?: "") }

    DialogoFormulario(
        titulo = if (mundo == null) "Nuevo mundo" else "Editar mundo",
        puedeGuardar = nombre.isNotBlank(),
        alGuardar = { alGuardar(nombre, descripcion) },
        alCancelar = alCancelar,
    ) {
        OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(descripcion, { descripcion = it }, label = { Text("Descripción") }, minLines = 3, modifier = Modifier.fillMaxWidth())
    }
}
