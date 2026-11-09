package com.sekaiforlyok.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.Cottage
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sekaiforlyok.model.Lugar
import com.sekaiforlyok.model.Mundo
import com.sekaiforlyok.model.TipoLugar
import com.sekaiforlyok.viewmodel.MundoViewModel
import com.sekaiforlyok.viewmodel.arbolLugares
import com.sekaiforlyok.viewmodel.nuevoId
import com.sekaiforlyok.viewmodel.padresPosibles

/** Icono de cada tipo de lugar. */
fun iconoLugar(tipo: TipoLugar): ImageVector = when (tipo) {
    TipoLugar.CONTINENTE -> Icons.Default.Public
    TipoLugar.REINO -> Icons.Default.Castle
    TipoLugar.CIUDAD -> Icons.Default.LocationCity
    TipoLugar.PUEBLO -> Icons.Default.Cottage
    TipoLugar.MAZMORRA -> Icons.Default.Whatshot
    TipoLugar.OTRO -> Icons.Default.Place
}

/** Colores por nivel del árbol: dorado, morado, azul, verde y, del nivel 4 en adelante, turquesa. */
private val COLORES_NIVEL = listOf(
    Color(0xFFD4A017), Color(0xFFAB47BC), Color(0xFF42A5F5), Color(0xFF4CAF50), Color(0xFF26A69A),
)

fun colorNivel(nivel: Int): Color = COLORES_NIVEL[nivel.coerceAtMost(COLORES_NIVEL.lastIndex)]

/** Pestaña de lugares: árbol con sangría (continente > reino > ciudad…). */
@Composable
fun PestanaLugares(mundo: Mundo, vm: MundoViewModel) {
    val t = LocalTextos.current
    var editando by remember { mutableStateOf<Lugar?>(null) }
    var borrando by remember { mutableStateOf<Lugar?>(null) }

    ConBotonAnadir(t.anadirLugar, alPulsar = { editando = Lugar(id = nuevoId(), nombre = "") }) {
        if (mundo.lugares.isEmpty()) ListaVacia(t.sinLugares)
        LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
            items(mundo.arbolLugares(), key = { it.first.id }) { (lugar, nivel) ->
                Row(
                    Modifier.fillMaxWidth().clickable { editando = lugar }
                        // Sangría: 24 dp más por cada nivel de profundidad
                        .padding(start = 16.dp + 24.dp * nivel, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(iconoLugar(lugar.tipo), contentDescription = t.tiposLugar.getValue(lugar.tipo), tint = colorNivel(nivel))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(lugar.nombre, style = MaterialTheme.typography.titleMedium, color = colorNivel(nivel))
                        Text(t.tiposLugar.getValue(lugar.tipo), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    BotonesEditarBorrar(alEditar = { editando = lugar }, alBorrar = { borrando = lugar })
                }
                HorizontalDivider()
            }
        }
    }

    editando?.let { original ->
        var l by remember(original.id) { mutableStateOf(original) }
        val esNuevo = mundo.lugares.none { it.id == original.id }
        DialogoFormulario(
            titulo = if (esNuevo) t.nuevoLugar else t.editarLugar,
            puedeGuardar = l.nombre.isNotBlank(),
            alGuardar = { vm.guardarLugar(l); editando = null },
            alCancelar = { editando = null },
        ) {
            OutlinedTextField(l.nombre, { l = l.copy(nombre = it) }, label = { Text(t.nombre) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Desplegable(t.tipo, TipoLugar.entries, l.tipo, { t.tiposLugar.getValue(it) }) { l = l.copy(tipo = it) }
            // Solo se ofrecen padres válidos: ni él mismo ni sus sublugares
            val padres = mundo.padresPosibles(if (esNuevo) null else l.id)
            Desplegable(
                t.estaDentroDe, listOf<Lugar?>(null) + padres, padres.find { it.id == l.padreId },
                { it?.let { p -> "${p.nombre} (${t.tiposLugar.getValue(p.tipo)})" } ?: t.ningunNivelSuperior },
            ) { l = l.copy(padreId = it?.id) }
            OutlinedTextField(l.descripcion, { l = l.copy(descripcion = it) }, label = { Text(t.descripcion) }, minLines = 3, modifier = Modifier.fillMaxWidth())
        }
    }
    borrando?.let { l ->
        ConfirmarBorrado(
            nombre = l.nombre,
            aviso = t.avisoBorrarLugar,
            alConfirmar = { vm.borrarLugar(l.id); borrando = null },
            alCancelar = { borrando = null },
        )
    }
}
