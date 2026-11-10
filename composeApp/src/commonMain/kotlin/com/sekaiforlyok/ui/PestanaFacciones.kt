package com.sekaiforlyok.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sekaiforlyok.model.Faccion
import com.sekaiforlyok.model.Mundo
import com.sekaiforlyok.model.Personaje
import com.sekaiforlyok.model.RelacionFaccion
import com.sekaiforlyok.model.TipoRelacionFaccion
import com.sekaiforlyok.viewmodel.MundoViewModel
import com.sekaiforlyok.viewmodel.nuevoId
import com.sekaiforlyok.viewmodel.personajesOrdenados

/** Pestaña de facciones: lista o constelación, crear, editar y borrar. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PestanaFacciones(mundo: Mundo, vm: MundoViewModel) {
    val t = LocalTextos.current
    var editando by remember { mutableStateOf<Faccion?>(null) }
    var borrando by remember { mutableStateOf<Faccion?>(null) }
    var enConstelacion by rememberSaveable { mutableStateOf(false) }
    var modo by remember { mutableStateOf(ModoConstelacion.VER) }
    var primero by remember { mutableStateOf<String?>(null) }                    // Primera facción elegida al unir
    var uniendo by remember { mutableStateOf<Pair<String, String>?>(null) }      // Dos facciones a unir: falta el tipo
    var borrandoRelacion by remember { mutableStateOf<RelacionFaccion?>(null) }

    ConBotonAnadir(t.anadirFaccion, alPulsar = { editando = Faccion(id = nuevoId(), nombre = "") }) {
      Column {
        // Lista o constelación; en la constelación, a la derecha, Unir y Borrar línea
        FlowRow(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!enConstelacion, onClick = { enConstelacion = false; modo = ModoConstelacion.VER }, label = { Text(t.verLista) })
            FilterChip(enConstelacion, onClick = { enConstelacion = true }, label = { Text(t.constelacion) })
            if (enConstelacion) {
                Spacer(Modifier.weight(1f))
                BotonesModo(modo, puedeUnir = mundo.facciones.size >= 2, alCambiar = { modo = it; primero = null })
            }
        }
        if (enConstelacion) {
            TextoModo(modo, hayPrimero = primero != null)
            ConstelacionFacciones(
                mundo, vm, seleccionada = primero,
                alPulsar = { id ->
                    when {
                        modo != ModoConstelacion.UNIR -> editando = mundo.facciones.find { it.id == id }
                        primero == null -> primero = id
                        primero == id -> primero = null // Tocar la misma otra vez la desmarca
                        else -> { uniendo = primero!! to id; primero = null; modo = ModoConstelacion.VER }
                    }
                },
                alPulsarRelacion = if (modo == ModoConstelacion.BORRAR) { r -> borrandoRelacion = r; modo = ModoConstelacion.VER } else null,
            )
            return@Column
        }
        if (mundo.facciones.isEmpty()) ListaVacia(t.sinFacciones)
        LazyColumn(contentPadding = PaddingValues(12.dp, 12.dp, 12.dp, 88.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(mundo.facciones.sortedBy { it.nombre.lowercase() }, key = { it.id }) { f ->
                TarjetaFaccion(
                    f = f,
                    color = mundo.colorFaccion(f.id),
                    miembros = mundo.personajesOrdenados().filter { it.faccionId == f.id },
                    alEditar = { editando = f },
                    alBorrar = { borrando = f },
                )
            }
        }
      }
    }

    // Al unir dos facciones se elige cómo se llevan
    uniendo?.let { (a, b) ->
        fun nombre(id: String) = mundo.facciones.find { it.id == id }?.nombre ?: "?"
        AlertDialog(
            onDismissRequest = { uniendo = null },
            title = { Text("${nombre(a)} ↔ ${nombre(b)}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(t.comoSeLlevan)
                    TipoRelacionFaccion.entries.forEach { tipo ->
                        Button(
                            onClick = { vm.relacionarFacciones(a, b, tipo); uniendo = null },
                            colors = ButtonDefaults.buttonColors(containerColor = colorRelacionFaccion(tipo)),
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(t.tiposRelacionFaccion.getValue(tipo), color = Color.White) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { uniendo = null }) { Text(t.cancelar) } },
        )
    }
    borrandoRelacion?.let { r ->
        fun nombre(id: String) = mundo.facciones.find { it.id == id }?.nombre ?: "?"
        ConfirmarBorrado(
            nombre = "${nombre(r.faccion1Id)} ↔ ${nombre(r.faccion2Id)}",
            alConfirmar = { vm.borrarRelacionFaccion(r.id); borrandoRelacion = null },
            alCancelar = { borrandoRelacion = null },
        )
    }

    editando?.let { original ->
        var f by remember(original.id) { mutableStateOf(original) }
        // Miembros marcados en el formulario; empiezan siendo los que ya son de esta facción
        var miembros by remember(original.id) {
            mutableStateOf(mundo.personajes.filter { it.faccionId == original.id }.map { it.id }.toSet())
        }
        var moviendo by remember { mutableStateOf<Personaje?>(null) } // Pendiente de confirmar el cambio de facción
        DialogoFormulario(
            titulo = if (mundo.facciones.none { it.id == original.id }) t.nuevaFaccion else t.editarFaccion,
            puedeGuardar = f.nombre.isNotBlank(),
            alGuardar = { vm.guardarFaccion(f, miembros); editando = null },
            alCancelar = { editando = null },
        ) {
            OutlinedTextField(f.nombre, { f = f.copy(nombre = it) }, label = { Text(t.nombre) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(f.descripcion, { f = f.copy(descripcion = it) }, label = { Text(t.descripcion) }, minLines = 3, modifier = Modifier.fillMaxWidth())
            Text(t.miembros(miembros.size), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
            if (mundo.personajes.isEmpty()) Text(t.sinPersonajes, style = MaterialTheme.typography.bodySmall)
            // Todos los personajes: marcar = entra en la facción, desmarcar = se queda sin facción
            mundo.personajesOrdenados().forEach { p ->
                val otra = mundo.facciones.find { it.id == p.faccionId && it.id != original.id }
                Row(
                    Modifier.fillMaxWidth().clickable {
                        when {
                            p.id in miembros -> miembros = miembros - p.id
                            otra != null -> moviendo = p // Está en otra facción: se pregunta antes
                            else -> miembros = miembros + p.id
                        }
                    }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = p.id in miembros, onCheckedChange = null)
                    Spacer(Modifier.width(8.dp))
                    AvatarPersonaje(p, 32.dp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(p.nombre)
                        // Avisa de que al marcarlo se cambiará de facción
                        if (otra != null) Text(t.ahoraEn(otra.nombre), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        // Confirmación al pasar a alguien de otra facción a esta
        moviendo?.let { p ->
            val de = mundo.facciones.find { it.id == p.faccionId }?.nombre ?: ""
            AlertDialog(
                onDismissRequest = { moviendo = null },
                title = { Text(t.moverTitulo) },
                text = { Text(t.moverDeFaccion(p.nombre, de, f.nombre.ifBlank { t.nuevaFaccion })) },
                confirmButton = { TextButton(onClick = { miembros = miembros + p.id; moviendo = null }) { Text(t.si) } },
                dismissButton = { TextButton(onClick = { moviendo = null }) { Text(t.no) } },
            )
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

/** Colores de las facciones, por orden de creación: las 8 primeras nunca repiten color. */
private val COLORES_FACCION = listOf(
    Color(0xFFD4A017), Color(0xFFAB47BC), Color(0xFF42A5F5), Color(0xFFE53935),
    Color(0xFF4CAF50), Color(0xFF26A69A), Color(0xFFFF9800), Color(0xFFEC407A),
)

fun Mundo.colorFaccion(id: String): Color =
    COLORES_FACCION[facciones.indexOfFirst { it.id == id }.coerceAtLeast(0) % COLORES_FACCION.size]

private const val CARAS_VISIBLES = 6 // Más miembros se resumen como "+N"

/** Tarjeta de una facción: escudo con su inicial, nombre en su color, descripción y caras de sus miembros. */
@Composable
private fun TarjetaFaccion(f: Faccion, color: Color, miembros: List<Personaje>, alEditar: () -> Unit, alBorrar: () -> Unit) {
    val t = LocalTextos.current
    Card(
        onClick = alEditar,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Escudo(f, color, Modifier.size(56.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(f.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
                if (f.descripcion.isNotBlank()) {
                    Text(f.descripcion, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Caras de los miembros, un poco montadas unas sobre otras (separación negativa)
                    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                        miembros.take(CARAS_VISIBLES).forEach { AvatarPersonaje(it, 32.dp) }
                    }
                    if (miembros.size > CARAS_VISIBLES) {
                        Text("+${miembros.size - CARAS_VISIBLES}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
                    }
                    if (miembros.isNotEmpty()) Spacer(Modifier.width(10.dp))
                    Etiqueta(t.miembros(miembros.size), color)
                }
            }
            BotonesEditarBorrar(alEditar, alBorrar)
        }
    }
}

/** Escudo de la facción en su color con su inicial dentro. */
@Composable
private fun Escudo(f: Faccion, color: Color, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Icon(Icons.Default.Shield, contentDescription = null, tint = color, modifier = Modifier.fillMaxSize())
        Text(f.nombre.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(bottom = 4.dp))
    }
}

/** Color de la línea según cómo se llevan dos facciones. */
fun colorRelacionFaccion(tipo: TipoRelacionFaccion): Color = when (tipo) {
    TipoRelacionFaccion.ALIADAS -> Color(0xFF4CAF50)   // Verde
    TipoRelacionFaccion.NEUTRALES -> Color(0xFF9E9E9E) // Gris
    TipoRelacionFaccion.ENEMIGAS -> Color(0xFFE53935)  // Rojo
}

/** Facciones como constelación: un escudo por facción y líneas de alianza, neutralidad o enemistad. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConstelacionFacciones(
    mundo: Mundo,
    vm: MundoViewModel,
    seleccionada: String?,
    alPulsar: (String) -> Unit,
    alPulsarRelacion: ((RelacionFaccion) -> Unit)?, // null = las líneas no se pueden tocar
) {
    val t = LocalTextos.current
    // Leyenda: siempre los tres tipos, para que se sepa qué significa cada color
    FlowRow(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TipoRelacionFaccion.entries.forEach { Etiqueta(t.tiposRelacionFaccion.getValue(it), colorRelacionFaccion(it)) }
    }
    Constelacion(
        nodos = mundo.facciones.map { NodoConstelacion(it.id, it.nombre, it.posX, it.posY) },
        enlaces = mundo.relacionesFacciones.map { EnlaceConstelacion(it.faccion1Id, it.faccion2Id, colorRelacionFaccion(it.tipo)) },
        alMover = vm::moverFaccion,
        alPulsar = alPulsar,
        // Las líneas van en el mismo orden que mundo.relacionesFacciones
        alPulsarEnlace = alPulsarRelacion?.let { elegir -> { i -> elegir(mundo.relacionesFacciones[i]) } },
    ) { nodo ->
        mundo.facciones.find { it.id == nodo.id }?.let { f ->
            // La facción elegida al unir lleva un aro blanco
            val aro = if (f.id == seleccionada) Modifier.border(3.dp, Color.White, CircleShape).padding(6.dp) else Modifier
            Escudo(f, mundo.colorFaccion(f.id), aro.size(64.dp))
        }
    }
}
