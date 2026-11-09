package com.sekaiforlyok.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sekaiforlyok.model.Faccion
import com.sekaiforlyok.model.Mundo
import com.sekaiforlyok.viewmodel.MundoViewModel
import com.sekaiforlyok.viewmodel.miembros
import com.sekaiforlyok.viewmodel.nuevoId

/** Pestaña de facciones: lista con número de miembros, crear, editar y borrar. */
@Composable
fun PestanaFacciones(mundo: Mundo, vm: MundoViewModel) {
    val t = LocalTextos.current
    var editando by remember { mutableStateOf<Faccion?>(null) }
    var borrando by remember { mutableStateOf<Faccion?>(null) }

    ConBotonAnadir(t.anadirFaccion, alPulsar = { editando = Faccion(id = nuevoId(), nombre = "") }) {
        if (mundo.facciones.isEmpty()) ListaVacia(t.sinFacciones)
        LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
            items(mundo.facciones.sortedBy { it.nombre.lowercase() }, key = { it.id }) { f ->
                Row(
                    Modifier.fillMaxWidth().clickable { editando = f }.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(f.nombre, style = MaterialTheme.typography.titleMedium)
                        if (f.descripcion.isNotBlank()) Text(f.descripcion, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                        Text(
                            t.miembros(mundo.miembros(f.id)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    BotonesEditarBorrar(alEditar = { editando = f }, alBorrar = { borrando = f })
                }
                HorizontalDivider()
            }
        }
    }

    editando?.let { original ->
        var f by remember(original.id) { mutableStateOf(original) }
        DialogoFormulario(
            titulo = if (mundo.facciones.none { it.id == original.id }) t.nuevaFaccion else t.editarFaccion,
            puedeGuardar = f.nombre.isNotBlank(),
            alGuardar = { vm.guardarFaccion(f); editando = null },
            alCancelar = { editando = null },
        ) {
            OutlinedTextField(f.nombre, { f = f.copy(nombre = it) }, label = { Text(t.nombre) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(f.descripcion, { f = f.copy(descripcion = it) }, label = { Text(t.descripcion) }, minLines = 3, modifier = Modifier.fillMaxWidth())
        }
    }
    borrando?.let { f ->
        ConfirmarBorrado(
            nombre = f.nombre,
            aviso = t.avisoBorrarFaccion,
            alConfirmar = { vm.borrarFaccion(f.id); borrando = null },
            alCancelar = { borrando = null },
        )
    }
}
