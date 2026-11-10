package com.sekaiforlyok.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.sekaiforlyok.viewmodel.personajesOrdenados

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
    var enConstelacion by rememberSaveable { mutableStateOf(false) }
    var personajeVisto by remember { mutableStateOf<String?>(null) } // Círculo pulsado en la constelación
    var modo by remember { mutableStateOf(ModoConstelacion.VER) }
    var primero by remember { mutableStateOf<String?>(null) } // Primer círculo elegido al unir

    fun nombre(id: String) = mundo.personajes.find { it.id == id }?.nombre ?: "?"

    ConBotonAnadir(t.anadirRelacion, alPulsar = {
        // Hacen falta al menos 2 personajes para crear una relación
        if (mundo.personajes.size < 2) {
            avisoFaltanPersonajes = true
        } else {
            editando = Relacion(nuevoId(), mundo.personajes[0].id, mundo.personajes[1].id)
        }
    }) {
      Column {
        // Elegir cómo verlas: en lista o como constelación
        FlowRow(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!enConstelacion, onClick = { enConstelacion = false; modo = ModoConstelacion.VER }, label = { Text(t.verLista) })
            FilterChip(enConstelacion, onClick = { enConstelacion = true }, label = { Text(t.constelacion) })
            if (enConstelacion) {
                Spacer(Modifier.weight(1f)) // Empuja Unir y Borrar línea al lado derecho
                BotonesModo(modo, puedeUnir = mundo.personajes.size >= 2, alCambiar = { modo = it; primero = null }, alNoPoderUnir = { avisoFaltanPersonajes = true })
            }
        }
        if (enConstelacion) {
            TextoModo(modo, hayPrimero = primero != null)
            ConstelacionRelaciones(
                mundo, vm, seleccionado = primero,
                alPulsar = { id ->
                    when {
                        modo != ModoConstelacion.UNIR -> personajeVisto = id
                        primero == null -> primero = id
                        primero == id -> primero = null // Tocar el mismo otra vez lo desmarca
                        else -> {
                            // Ya están los dos: se abre el formulario de siempre con ellos elegidos
                            editando = Relacion(nuevoId(), primero!!, id)
                            primero = null; modo = ModoConstelacion.VER
                        }
                    }
                },
                alPulsarRelacion = if (modo == ModoConstelacion.BORRAR) { r -> borrando = r; modo = ModoConstelacion.VER } else null,
            )
            return@Column
        }
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
    // Diálogo: personaje pulsado en la constelación, con todas sus relaciones
    mundo.personajes.find { it.id == personajeVisto }?.let { p ->
        val suyas = mundo.relaciones.filter { it.personaje1Id == p.id || it.personaje2Id == p.id }
        AlertDialog(
            onDismissRequest = { personajeVisto = null },
            icon = { AvatarPersonaje(p, 64.dp) },
            title = { Text(p.nombre) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (suyas.isEmpty()) Text(t.sinRelacionesPersonaje)
                    suyas.forEach { r ->
                        val otro = nombre(if (r.personaje1Id == p.id) r.personaje2Id else r.personaje1Id)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Etiqueta(t.tiposRelacion.getValue(r.tipo), colorRelacion(r.tipo))
                            Text(otro)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { personajeVisto = null }) { Text(t.cerrar) } },
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

/**
 * Relaciones como constelación: cada personaje es un círculo con su personita (o su rango)
 * y cada relación una línea del color de su tipo. Debajo de los botones sale la leyenda de colores.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConstelacionRelaciones(
    mundo: Mundo,
    vm: MundoViewModel,
    seleccionado: String?,
    alPulsar: (String) -> Unit,
    alPulsarRelacion: ((Relacion) -> Unit)?, // null = las líneas no se pueden tocar
) {
    val t = LocalTextos.current
    val tipos = mundo.relaciones.map { it.tipo }.distinct()
    if (tipos.isNotEmpty()) {
        FlowRow(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            tipos.forEach { Etiqueta(t.tiposRelacion.getValue(it), colorRelacion(it)) }
        }
    }
    val personajes = mundo.personajesOrdenados()
    Constelacion(
        nodos = personajes.map { NodoConstelacion(it.id, it.nombre, it.posX, it.posY) },
        enlaces = mundo.relaciones.map { EnlaceConstelacion(it.personaje1Id, it.personaje2Id, colorRelacion(it.tipo)) },
        alMover = vm::moverPersonaje,
        alPulsar = alPulsar,
        // Las líneas van en el mismo orden que mundo.relaciones
        alPulsarEnlace = alPulsarRelacion?.let { elegir -> { i -> elegir(mundo.relaciones[i]) } },
    ) { nodo ->
        personajes.find { it.id == nodo.id }?.let {
            // El círculo elegido al unir lleva un aro blanco
            val aro = if (nodo.id == seleccionado) Modifier.border(4.dp, Color.White, CircleShape).padding(4.dp) else Modifier
            Box(aro) { AvatarPersonaje(it, 72.dp) }
        }
    }
}
