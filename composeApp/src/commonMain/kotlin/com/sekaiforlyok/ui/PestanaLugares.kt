package com.sekaiforlyok.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Cottage
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Whatshot
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
    var conImagen by remember { mutableStateOf<String?>(null) } // Lugar cuya imagen se está viendo

    // Al abrir la imagen de un lugar, ocupa toda la pestaña hasta pulsar "volver"
    val abierto = mundo.lugares.find { it.id == conImagen }
    if (abierto != null) {
        ImagenLugar(abierto, mundo, vm, alVolver = { conImagen = null })
        return
    }

    ConBotonAnadir(t.anadirLugar, alPulsar = { editando = Lugar(id = nuevoId(), nombre = "") }) {
        if (mundo.lugares.isEmpty()) ListaVacia(t.sinLugares)
        LazyColumn(contentPadding = PaddingValues(12.dp, 12.dp, 12.dp, 88.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(mundo.arbolLugares(), key = { it.first.id }) { (lugar, nivel) ->
                // Sangría: 20 dp más por cada nivel de profundidad
                Box(Modifier.padding(start = 20.dp * nivel)) {
                    TarjetaLugar(
                        lugar, colorNivel(nivel), mundo, vm,
                        alEditar = { editando = lugar },
                        alBorrar = { borrando = lugar },
                        alAbrirImagen = { conImagen = lugar.id },
                    )
                }
            }
        }
    }

    editando?.let { original ->
        var l by remember(original.id) { mutableStateOf(original) }
        val esNuevo = mundo.lugares.none { it.id == original.id }
        // Personajes marcados como de aquí; empiezan siendo los que ya tienen este lugar de origen
        var nacidos by remember(original.id) {
            mutableStateOf(mundo.personajes.filter { it.lugarId == original.id }.map { it.id }.toSet())
        }
        DialogoFormulario(
            titulo = if (esNuevo) t.nuevoLugar else t.editarLugar,
            puedeGuardar = l.nombre.isNotBlank(),
            alGuardar = { vm.guardarLugar(l, nacidos); editando = null },
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
            ElegirPersonajes(
                mundo, t.nacidosAqui(nacidos.size), nacidos, { nacidos = it },
                // Nombre del otro lugar del que es, si es de otro
                otroGrupo = { p -> mundo.lugares.find { it.id == p.lugarId && it.id != original.id }?.nombre },
                tituloMover = t.moverLugarTitulo,
                destino = l.nombre.ifBlank { t.nuevoLugar },
            )
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

private const val CARAS_LUGAR = 5 // Más personajes se resumen como "+N"

/** Tarjeta de un lugar en el color de su nivel: icono, tipo, sublugares, personajes de aquí y miniatura. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaLugar(
    lugar: Lugar,
    color: Color,
    mundo: Mundo,
    vm: MundoViewModel,
    alEditar: () -> Unit,
    alBorrar: () -> Unit,
    alAbrirImagen: () -> Unit,
) {
    val t = LocalTextos.current
    val sublugares = mundo.lugares.count { it.padreId == lugar.id }
    val nacidos = mundo.personajes.filter { it.lugarId == lugar.id }
    val miniatura = remember(lugar.rutaImagen) { vm.imagenLugar(lugar) }
    Card(
        onClick = alEditar,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            // Icono del tipo en un círculo del color del nivel
            Box(Modifier.size(44.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
                Icon(iconoLugar(lugar.tipo), contentDescription = t.tiposLugar.getValue(lugar.tipo), tint = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(lugar.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
                if (lugar.descripcion.isNotBlank()) {
                    Text(lugar.descripcion, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.Center) {
                    Etiqueta(t.tiposLugar.getValue(lugar.tipo), color)
                    if (sublugares > 0) Etiqueta(t.sublugaresN(sublugares), color)
                    if (nacidos.isNotEmpty()) {
                        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                                nacidos.take(CARAS_LUGAR).forEach { AvatarPersonaje(it, 24.dp) }
                            }
                            Text(
                                // El número total también cubre las caras que no caben
                                "  " + t.nacidosAqui(nacidos.size),
                                style = MaterialTheme.typography.labelMedium, color = color,
                            )
                        }
                    }
                }
            }
            // Imagen propia del lugar (los continentes ya tienen el mapa del mundo)
            if (lugar.tipo != TipoLugar.CONTINENTE) {
                if (miniatura != null) {
                    Image(
                        miniatura, contentDescription = t.imagenDelLugar, contentScale = ContentScale.Crop,
                        modifier = Modifier.padding(start = 8.dp).size(56.dp).clip(RoundedCornerShape(10.dp))
                            .border(2.dp, color, RoundedCornerShape(10.dp)).clickable(onClick = alAbrirImagen),
                    )
                } else {
                    IconButton(onClick = alAbrirImagen) {
                        Icon(Icons.Default.AddPhotoAlternate, t.imagenDelLugar, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            BotonesEditarBorrar(alEditar, alBorrar)
        }
    }
}
