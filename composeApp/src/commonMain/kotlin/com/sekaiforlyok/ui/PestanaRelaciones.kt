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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sekaiforlyok.model.Mundo
import com.sekaiforlyok.model.Relacion
import com.sekaiforlyok.model.TipoRelacion
import com.sekaiforlyok.viewmodel.MundoViewModel
import com.sekaiforlyok.viewmodel.nuevoId

/** Color de cada tipo de relación. */
fun colorRelacion(tipo: TipoRelacion): Color = when (tipo) {
    TipoRelacion.AMIGOS -> Color(0xFF4CAF50)          // Verde
    TipoRelacion.RIVALES -> Color(0xFFFF9800)         // Naranja
    TipoRelacion.ENEMIGOS -> Color(0xFFE53935)        // Rojo
    TipoRelacion.PAREJA -> Color(0xFFEC407A)          // Rosa
    TipoRelacion.FAMILIA -> Color(0xFF42A5F5)         // Azul
    TipoRelacion.MAESTRO_ALUMNO -> Color(0xFFAB47BC)  // Morado
    TipoRelacion.ALIADOS -> Color(0xFF26A69A)         // Turquesa
    TipoRelacion.OTRO -> Color(0xFF9E9E9E)            // Gris
}

/** Pestaña de relaciones entre personajes. */
@Composable
fun PestanaRelaciones(mundo: Mundo, vm: MundoViewModel) {
    val t = LocalTextos.current
    var editando by remember { mutableStateOf<Relacion?>(null) }
    var borrando by remember { mutableStateOf<Relacion?>(null) }
    var avisoFaltanPersonajes by remember { mutableStateOf(false) }

    fun nombre(id: String) = mundo.personajes.find { it.id == id }?.nombre ?: "?"

    ConBotonAnadir(t.anadirRelacion, alPulsar = {
        // Hacen falta al menos 2 personajes para crear una relación
        if (mundo.personajes.size < 2) {
            avisoFaltanPersonajes = true
        } else {
            editando = Relacion(nuevoId(), mundo.personajes[0].id, mundo.personajes[1].id)
        }
    }) {
        if (mundo.relaciones.isEmpty()) ListaVacia(t.sinRelaciones)
        LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
            items(mundo.relaciones, key = { it.id }) { r ->
                Row(
                    Modifier.fillMaxWidth().clickable { editando = r }.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("${nombre(r.personaje1Id)}  ↔  ${nombre(r.personaje2Id)}", style = MaterialTheme.typography.titleMedium)
                        Etiqueta(t.tiposRelacion.getValue(r.tipo), colorRelacion(r.tipo))
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
            titulo = if (mundo.relaciones.none { it.id == original.id }) t.nuevaRelacion else t.editarRelacion,
            puedeGuardar = !mismoPersonaje,
            alGuardar = { if (vm.guardarRelacion(r)) editando = null },
            alCancelar = { editando = null },
        ) {
            val personajes = mundo.personajes.sortedBy { it.nombre.lowercase() }
            Desplegable(t.personaje1, personajes, personajes.find { it.id == r.personaje1Id }, { it?.nombre ?: t.eligeUno }) {
                r = r.copy(personaje1Id = it!!.id)
            }
            Desplegable(t.personaje2, personajes, personajes.find { it.id == r.personaje2Id }, { it?.nombre ?: t.eligeUno }) {
                r = r.copy(personaje2Id = it!!.id)
            }
            if (mismoPersonaje) {
                Text(t.mismoPersonaje, color = MaterialTheme.colorScheme.error)
            }
            Desplegable(t.tipoRelacion, TipoRelacion.entries, r.tipo, { t.tiposRelacion.getValue(it) }) { r = r.copy(tipo = it) }
            OutlinedTextField(r.nota, { r = r.copy(nota = it) }, label = { Text(t.notaOpcional) }, minLines = 2, modifier = Modifier.fillMaxWidth())
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
            title = { Text(t.faltanPersonajes) },
            text = { Text(t.faltanPersonajesTexto) },
            confirmButton = { TextButton(onClick = { avisoFaltanPersonajes = false }) { Text(t.entendido) } },
        )
    }
}
