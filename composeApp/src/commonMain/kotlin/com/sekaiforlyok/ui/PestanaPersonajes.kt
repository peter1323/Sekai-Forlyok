package com.sekaiforlyok.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sekaiforlyok.model.Apariencia
import com.sekaiforlyok.model.Faccion
import com.sekaiforlyok.model.Lugar
import com.sekaiforlyok.model.Mundo
import com.sekaiforlyok.model.Personaje
import com.sekaiforlyok.model.Rango
import com.sekaiforlyok.viewmodel.MundoViewModel
import com.sekaiforlyok.viewmodel.nuevoId
import com.sekaiforlyok.viewmodel.personajesOrdenados

/** Color del círculo de cada rango. */
fun colorRango(rango: Rango): Color = when (rango) {
    Rango.E -> Color(0xFF8E8E8E) // Gris
    Rango.D -> Color(0xFF8B5A2B) // Marrón
    Rango.C -> Color(0xFF3E9B4F) // Verde
    Rango.B -> Color(0xFF2F6FD6) // Azul
    Rango.A -> Color(0xFF8A44C9) // Morado
    Rango.S -> Color(0xFFD4A017) // Dorado
}

/** Círculo de color con la letra del rango. */
@Composable
fun CirculoRango(rango: Rango) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(colorRango(rango)),
        contentAlignment = Alignment.Center,
    ) { Text(rango.name, color = Color.White, fontWeight = FontWeight.Bold) }
}

/**
 * Foto del personaje: la cara de su personita con un borde del color de su rango y la letra del rango.
 * Si no tiene personita, se ve el círculo del rango de siempre.
 */
@Composable
fun AvatarPersonaje(p: Personaje, tamano: Dp) {
    val apariencia = p.apariencia
    if (apariencia == null) {
        Box(Modifier.size(tamano).clip(CircleShape).background(colorRango(p.rango)), contentAlignment = Alignment.Center) {
            Text(p.rango.name, color = Color.White, fontWeight = FontWeight.Bold)
        }
        return
    }
    Box(Modifier.size(tamano)) {
        Chibi(
            apariencia,
            Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFFE8E0F5)).border(3.dp, colorRango(p.rango), CircleShape),
            soloCara = true,
        )
        // Letra del rango en una bolita abajo a la derecha
        Box(
            Modifier.align(Alignment.BottomEnd).size(tamano * 0.38f).clip(CircleShape).background(colorRango(p.rango)),
            contentAlignment = Alignment.Center,
        ) { Text(p.rango.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (tamano.value * 0.2f).sp) }
    }
}

/** Pestaña de personajes: lista ordenada de S a E y formulario para crear/editar. */
@Composable
fun PestanaPersonajes(mundo: Mundo, vm: MundoViewModel) {
    val t = LocalTextos.current
    var editando by remember { mutableStateOf<Personaje?>(null) }  // Personaje abierto en el formulario
    var borrando by remember { mutableStateOf<Personaje?>(null) }

    ConBotonAnadir(t.anadirPersonaje, alPulsar = { editando = Personaje(id = nuevoId(), nombre = "") }) {
        if (mundo.personajes.isEmpty()) ListaVacia(t.sinPersonajes)
        LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
            items(mundo.personajesOrdenados(), key = { it.id }) { p ->
                FilaPersonaje(
                    p = p,
                    faccion = mundo.facciones.find { it.id == p.faccionId },
                    alEditar = { editando = p },
                    alBorrar = { borrando = p },
                )
                HorizontalDivider()
            }
        }
    }

    editando?.let { p ->
        DialogoPersonaje(
            original = p,
            mundo = mundo,
            alGuardar = { vm.guardarPersonaje(it); editando = null },
            alCancelar = { editando = null },
        )
    }
    borrando?.let { p ->
        ConfirmarBorrado(
            nombre = p.nombre,
            aviso = t.avisoBorrarPersonaje,
            alConfirmar = { vm.borrarPersonaje(p.id); borrando = null },
            alCancelar = { borrando = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilaPersonaje(p: Personaje, faccion: Faccion?, alEditar: () -> Unit, alBorrar: () -> Unit) {
    val t = LocalTextos.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = alEditar).padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarPersonaje(p, 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            val edad = p.edad?.let { t.edadAnos(it) } ?: ""
            Text(p.nombre + edad, style = MaterialTheme.typography.titleMedium)
            Text(
                faccion?.nombre ?: t.sinFaccion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Poderes como pequeñas etiquetas
            if (p.poderes.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    p.poderes.forEach { Etiqueta(it, MaterialTheme.colorScheme.primary) }
                }
            }
        }
        BotonesEditarBorrar(alEditar, alBorrar)
    }
}

/** Formulario de personaje. Al editar uno existente, también muestra sus relaciones. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DialogoPersonaje(original: Personaje, mundo: Mundo, alGuardar: (Personaje) -> Unit, alCancelar: () -> Unit) {
    val t = LocalTextos.current
    // Copia de trabajo: se va cambiando con el formulario y solo se guarda al pulsar "Guardar"
    var p by remember { mutableStateOf(original) }
    var edadTexto by remember { mutableStateOf(original.edad?.toString() ?: "") }
    var nuevoPoder by remember { mutableStateOf("") }
    val esNuevo = mundo.personajes.none { it.id == original.id }

    fun anadirPoder() {
        val poder = nuevoPoder.trim()
        if (poder.isNotEmpty() && poder !in p.poderes) p = p.copy(poderes = p.poderes + poder)
        nuevoPoder = ""
    }

    DialogoFormulario(
        titulo = if (esNuevo) t.nuevoPersonaje else t.editarPersonaje,
        puedeGuardar = p.nombre.isNotBlank(),
        alGuardar = { alGuardar(p.copy(edad = edadTexto.toIntOrNull())) },
        alCancelar = alCancelar,
    ) {
        OutlinedTextField(p.nombre, { p = p.copy(nombre = it) }, label = { Text(t.nombre) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            edadTexto,
            { texto -> edadTexto = texto.filter { it.isDigit() }.take(6) }, // Solo números
            label = { Text(t.edad) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        // Rangos de S a E en el desplegable
        Desplegable(t.rango, Rango.entries.reversed(), p.rango, { it.name }) { p = p.copy(rango = it) }

        // Poderes: se escriben y se añaden como chips; la X los quita
        Text(t.poderes, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            p.poderes.forEach { poder ->
                InputChip(
                    selected = false,
                    onClick = { p = p.copy(poderes = p.poderes - poder) },
                    label = { Text(poder) },
                    trailingIcon = { Icon(Icons.Default.Close, t.quitarPoder, Modifier.size(16.dp)) },
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(nuevoPoder, { nuevoPoder = it }, label = { Text(t.nuevoPoder) }, singleLine = true, modifier = Modifier.weight(1f))
            TextButton(onClick = ::anadirPoder) { Text(t.anadir) }
        }

        // Desplegables de facción y lugar. "null" es la opción "Sin …"
        Desplegable(
            t.faccion, listOf<Faccion?>(null) + mundo.facciones, mundo.facciones.find { it.id == p.faccionId },
            { it?.nombre ?: t.sinFaccion },
        ) { p = p.copy(faccionId = it?.id) }
        Desplegable(
            t.lugarOrigen, listOf<Lugar?>(null) + mundo.lugares, mundo.lugares.find { it.id == p.lugarId },
            { it?.nombre ?: t.sinLugar },
        ) { p = p.copy(lugarId = it?.id) }

        OutlinedTextField(p.descripcion, { p = p.copy(descripcion = it) }, label = { Text(t.historia) }, minLines = 4, modifier = Modifier.fillMaxWidth())

        // La personita chibi: se crea si se quiere; sin ella se ve el círculo del rango
        Text(t.personita, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp))
        val apariencia = p.apariencia
        if (apariencia == null) {
            OutlinedButton(onClick = { p = p.copy(apariencia = Apariencia()) }) { Text(t.crearPersonita) }
        } else {
            EditorChibi(apariencia) { p = p.copy(apariencia = it) }
            TextButton(onClick = { p = p.copy(apariencia = null) }) { Text(t.quitarPersonita, color = MaterialTheme.colorScheme.error) }
        }

        // Relaciones de este personaje (solo lectura; se editan en la pestaña Relaciones)
        if (!esNuevo) {
            val relaciones = mundo.relaciones.filter { it.personaje1Id == p.id || it.personaje2Id == p.id }
            Text(t.relaciones, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp))
            if (relaciones.isEmpty()) Text(t.sinRelacionesPersonaje, style = MaterialTheme.typography.bodySmall)
            relaciones.forEach { r ->
                val otroId = if (r.personaje1Id == p.id) r.personaje2Id else r.personaje1Id
                val otro = mundo.personajes.find { it.id == otroId }?.nombre ?: "?"
                val nota = if (r.nota.isNotBlank()) " — ${r.nota}" else ""
                Text("• " + t.relacionCon(t.tiposRelacion.getValue(r.tipo), otro) + nota, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
