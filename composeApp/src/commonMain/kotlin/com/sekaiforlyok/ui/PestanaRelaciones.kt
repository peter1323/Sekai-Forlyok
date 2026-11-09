package com.sekaiforlyok.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sekaiforlyok.model.Mundo
import com.sekaiforlyok.model.Relacion
import com.sekaiforlyok.model.TipoRelacion
import com.sekaiforlyok.viewmodel.MundoViewModel
import com.sekaiforlyok.viewmodel.nuevoId

/** Pestaña de relaciones entre personajes. */
@Composable
fun PestanaRelaciones(mundo: Mundo, vm: MundoViewModel) {
    var editando by remember { mutableStateOf<Relacion?>(null) }
    var borrando by remember { mutableStateOf<Relacion?>(null) }
    var avisoFaltanPersonajes by remember { mutableStateOf(false) }

    fun nombre(id: String) = mundo.personajes.find { it.id == id }?.nombre ?: "?"

    ConBotonAnadir("Añadir relación", alPulsar = {
        // Hacen falta al menos 2 personajes para crear una relación
        if (mundo.personajes.size < 2) {
            avisoFaltanPersonajes = true
        } else {
            editando = Relacion(nuevoId(), mundo.personajes[0].id, mundo.personajes[1].id)
        }
    }) {
        if (mundo.relaciones.isEmpty()) ListaVacia("Todavía no hay relaciones.")
        LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
            items(mundo.relaciones, key = { it.id }) { r ->
                Row(
                    Modifier.fillMaxWidth().clickable { editando = r }.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("${nombre(r.personaje1Id)}  ↔  ${nombre(r.personaje2Id)}", style = MaterialTheme.typography.titleMedium)
                        Text(r.tipo.texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        if (r.nota.isNotBlank()) Text(r.nota, style = MaterialTheme.typography.bodySmall)
                    }
                    BotonesEditarBorrar(alEditar = { editando = r }, alBorrar = { borrando = r })
                }
                HorizontalDivider()
            }
        }
    }

    editando?.let { original ->
        var r by remember(original.id) { mutableStateOf(original) }
        val mismoPersonaje = r.personaje1Id == r.personaje2Id
        DialogoFormulario(
            titulo = if (mundo.relaciones.none { it.id == original.id }) "Nueva relación" else "Editar relación",
            puedeGuardar = !mismoPersonaje,
            alGuardar = { if (vm.guardarRelacion(r)) editando = null },
            alCancelar = { editando = null },
        ) {
            val personajes = mundo.personajes.sortedBy { it.nombre.lowercase() }
            Desplegable("Personaje 1", personajes, personajes.find { it.id == r.personaje1Id }, { it?.nombre ?: "Elige uno" }) {
                r = r.copy(personaje1Id = it!!.id)
            }
            Desplegable("Personaje 2", personajes, personajes.find { it.id == r.personaje2Id }, { it?.nombre ?: "Elige uno" }) {
                r = r.copy(personaje2Id = it!!.id)
            }
            if (mismoPersonaje) {
                Text("Un personaje no puede relacionarse consigo mismo.", color = MaterialTheme.colorScheme.error)
            }
            Desplegable("Tipo de relación", TipoRelacion.entries, r.tipo, { it.texto }) { r = r.copy(tipo = it) }
            OutlinedTextField(r.nota, { r = r.copy(nota = it) }, label = { Text("Nota (opcional)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        }
    }
    borrando?.let { r ->
        ConfirmarBorrado(
            nombre = "${nombre(r.personaje1Id)} ↔ ${nombre(r.personaje2Id)}",
            alConfirmar = { vm.borrarRelacion(r.id); borrando = null },
            alCancelar = { borrando = null },
        )
    }
    if (avisoFaltanPersonajes) {
        AlertDialog(
            onDismissRequest = { avisoFaltanPersonajes = false },
            title = { Text("Faltan personajes") },
            text = { Text("Necesitas al menos 2 personajes para crear una relación.") },
            confirmButton = { TextButton(onClick = { avisoFaltanPersonajes = false }) { Text("Entendido") } },
        )
    }
}
