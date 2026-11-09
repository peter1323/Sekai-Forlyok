package com.sekaiforlyok.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// Piezas pequeñas que se repiten en varias pantallas.

/**
 * Desplegable sencillo: un botón que muestra la opción elegida y, al pulsarlo, la lista.
 * [texto] dice cómo se muestra cada opción en pantalla.
 */
@Composable
fun <T> Desplegable(
    etiqueta: String,
    opciones: List<T>,
    seleccion: T,
    texto: (T) -> String,
    alElegir: (T) -> Unit,
) {
    var abierto by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium)
        Box {
            OutlinedButton(onClick = { abierto = true }, modifier = Modifier.fillMaxWidth()) {
                Text(texto(seleccion), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
                opciones.forEach { opcion ->
                    DropdownMenuItem(
                        text = { Text(texto(opcion)) },
                        onClick = { alElegir(opcion); abierto = false },
                    )
                }
            }
        }
    }
}

/** Diálogo con un formulario: título, campos desplazables y botones Guardar/Cancelar. */
@Composable
fun DialogoFormulario(
    titulo: String,
    puedeGuardar: Boolean,
    alGuardar: () -> Unit,
    alCancelar: () -> Unit,
    contenido: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = alCancelar,
        title = { Text(titulo) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { contenido() } },
        confirmButton = { TextButton(onClick = alGuardar, enabled = puedeGuardar) { Text(LocalTextos.current.guardar) } },
        dismissButton = { TextButton(onClick = alCancelar) { Text(LocalTextos.current.cancelar) } },
    )
}

/** Pregunta "¿Seguro que quieres borrar…?" antes de borrar algo. */
@Composable
fun ConfirmarBorrado(nombre: String, aviso: String = "", alConfirmar: () -> Unit, alCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = alCancelar,
        title = { Text(LocalTextos.current.borrarTitulo(nombre)) },
        text = if (aviso.isNotEmpty()) ({ Text(aviso) }) else null,
        confirmButton = {
            TextButton(onClick = alConfirmar) { Text(LocalTextos.current.borrar, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = alCancelar) { Text(LocalTextos.current.cancelar) } },
    )
}

/** Botones de editar y borrar que aparecen a la derecha de cada fila. */
@Composable
fun BotonesEditarBorrar(alEditar: () -> Unit, alBorrar: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = alEditar) { Icon(Icons.Default.Edit, contentDescription = LocalTextos.current.editar) }
        IconButton(onClick = alBorrar) { Icon(Icons.Default.Delete, contentDescription = LocalTextos.current.borrar) }
    }
}

/** Etiqueta pequeña con fondo de color suave y texto del mismo color (poderes, tipo de relación…). */
@Composable
fun Etiqueta(texto: String, color: Color) {
    Text(
        texto,
        color = color,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier
            .padding(top = 4.dp)
            .background(color.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Texto que se muestra cuando una lista está vacía. */
@Composable
fun ListaVacia(texto: String) {
    Text(
        texto,
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
