package com.sekaiforlyok.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sekaiforlyok.model.Lugar
import com.sekaiforlyok.model.Mundo
import com.sekaiforlyok.model.Suceso
import com.sekaiforlyok.viewmodel.MundoViewModel
import com.sekaiforlyok.viewmodel.nuevoId

/** Colores de los puntos de la línea temporal; se van turnando de un suceso al siguiente. */
private val COLORES_SUCESO = listOf(
    Color(0xFFE9C46A), Color(0xFFAB47BC), Color(0xFF42A5F5), Color(0xFF4CAF50), Color(0xFFEC407A), Color(0xFF26A69A),
)

/** Pestaña de historia: los sucesos del mundo en orden, como una línea temporal vertical. */
@Composable
fun PestanaHistoria(mundo: Mundo, vm: MundoViewModel) {
    val t = LocalTextos.current
    var editando by remember { mutableStateOf<Suceso?>(null) }
    var borrando by remember { mutableStateOf<Suceso?>(null) }

    ConBotonAnadir(t.anadirSuceso, alPulsar = { editando = Suceso(id = nuevoId(), titulo = "") }) {
        if (mundo.sucesos.isEmpty()) ListaVacia(t.sinSucesos)
        LazyColumn(contentPadding = PaddingValues(12.dp, 12.dp, 12.dp, 88.dp)) {
            itemsIndexed(mundo.sucesos, key = { _, s -> s.id }) { i, s ->
                FilaSuceso(
                    s, mundo, COLORES_SUCESO[i % COLORES_SUCESO.size],
                    esPrimero = i == 0, esUltimo = i == mundo.sucesos.lastIndex,
                    alEditar = { editando = s },
                    alBorrar = { borrando = s },
                    alMover = { arriba -> vm.moverSuceso(s.id, arriba) },
                )
            }
        }
    }

    editando?.let { original ->
        var s by remember(original.id) { mutableStateOf(original) }
        DialogoFormulario(
            titulo = if (mundo.sucesos.none { it.id == original.id }) t.nuevoSuceso else t.editarSuceso,
            puedeGuardar = s.titulo.isNotBlank(),
            alGuardar = { vm.guardarSuceso(s); editando = null },
            alCancelar = { editando = null },
        ) {
            OutlinedTextField(s.titulo, { s = s.copy(titulo = it) }, label = { Text(t.tituloSuceso) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(s.cuando, { s = s.copy(cuando = it) }, label = { Text(t.cuando) }, placeholder = { Text(t.ejemploCuando) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(s.descripcion, { s = s.copy(descripcion = it) }, label = { Text(t.queOcurrio) }, minLines = 3, modifier = Modifier.fillMaxWidth())
            Desplegable(
                t.dondeOcurrio, listOf<Lugar?>(null) + mundo.lugares, mundo.lugares.find { it.id == s.lugarId },
                { it?.nombre ?: t.sinLugar },
            ) { s = s.copy(lugarId = it?.id) }
            // Un personaje puede salir en muchos sucesos: no hay "otro grupo" del que moverlo
            ElegirPersonajes(
                mundo, t.participan(s.personajes.size), s.personajes.toSet(), { s = s.copy(personajes = it.toList()) },
                otroGrupo = { null }, tituloMover = "", destino = "",
            )
        }
    }
    borrando?.let { s ->
        ConfirmarBorrado(
            nombre = s.titulo,
            alConfirmar = { vm.borrarSuceso(s.id); borrando = null },
            alCancelar = { borrando = null },
        )
    }
}

/** Un suceso: a la izquierda el punto de color sobre la línea; a la derecha su tarjeta. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilaSuceso(
    s: Suceso,
    mundo: Mundo,
    color: Color,
    esPrimero: Boolean,
    esUltimo: Boolean,
    alEditar: () -> Unit,
    alBorrar: () -> Unit,
    alMover: (arriba: Boolean) -> Unit,
) {
    val t = LocalTextos.current
    val linea = MaterialTheme.colorScheme.outlineVariant
    // IntrinsicSize.Min: la línea de la izquierda mide lo mismo que la tarjeta
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(Modifier.width(28.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            // Tramo de línea por encima del punto (no en el primero) y por debajo (no en el último)
            Box(Modifier.width(3.dp).height(22.dp).background(if (esPrimero) Color.Transparent else linea))
            Box(Modifier.size(16.dp).background(color, CircleShape))
            Box(Modifier.width(3.dp).weight(1f).background(if (esUltimo) Color.Transparent else linea))
        }
        Card(
            onClick = alEditar,
            colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f)),
            border = BorderStroke(1.dp, color.copy(alpha = 0.45f)),
            modifier = Modifier.weight(1f).padding(start = 8.dp, top = 6.dp, bottom = 6.dp),
        ) {
            Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    if (s.cuando.isNotBlank()) Text(s.cuando, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    Text(s.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (s.descripcion.isNotBlank()) Text(s.descripcion, style = MaterialTheme.typography.bodyMedium)
                    val lugar = mundo.lugares.find { it.id == s.lugarId }
                    val quienes = s.personajes.mapNotNull { id -> mundo.personajes.find { it.id == id } }
                    if (lugar != null || quienes.isNotEmpty()) {
                        FlowRow(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.Center) {
                            if (lugar != null) Etiqueta("📍 " + lugar.nombre, MaterialTheme.colorScheme.onSurfaceVariant)
                            // Caras de los que participan, un poco montadas unas sobre otras
                            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                                quienes.forEach { AvatarPersonaje(it, 28.dp) }
                            }
                        }
                    }
                }
                // Flechas para cambiar el orden: más antiguo (arriba) o más reciente (abajo)
                Column {
                    IconButton(onClick = { alMover(true) }, enabled = !esPrimero) { Icon(Icons.Default.KeyboardArrowUp, t.subir) }
                    IconButton(onClick = { alMover(false) }, enabled = !esUltimo) { Icon(Icons.Default.KeyboardArrowDown, t.bajar) }
                }
                BotonesEditarBorrar(alEditar, alBorrar)
            }
        }
    }
}
