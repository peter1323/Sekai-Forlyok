package com.sekaiforlyok.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sekaiforlyok.carpetaDatos
import com.sekaiforlyok.data.Repositorio
import com.sekaiforlyok.model.Tema
import com.sekaiforlyok.viewmodel.AjustesViewModel
import com.sekaiforlyok.viewmodel.MundoViewModel
import com.sekaiforlyok.viewmodel.MundosViewModel

/** Las pantallas de la app. La navegación es simplemente cambiar cuál se muestra. */
sealed class Pantalla {
    data object ListaMundos : Pantalla()
    data class DetalleMundo(val mundoId: String) : Pantalla()
    data object Ajustes : Pantalla()
    data object Constelacion : Pantalla()
}

// Un único repositorio para toda la app. Se crea la primera vez que se usa.
private val repositorio by lazy { Repositorio(carpetaDatos()) }

/**
 * Punto de entrada de la interfaz, común a Android y escritorio.
 * [alCambiarColores] avisa del color de fondo y de texto del tema (escritorio lo usa para pintar la barra de la ventana).
 */
@Composable
fun App(alCambiarColores: (fondo: Color, texto: Color) -> Unit = { _, _ -> }) {
    val ajustesVm = viewModel { AjustesViewModel(repositorio) }
    val ajustes by ajustesVm.ajustes.collectAsState()
    val oscuro = when (ajustes.tema) {
        Tema.SISTEMA -> isSystemInDarkTheme()
        Tema.CLARO -> false
        Tema.OSCURO -> true
    }

    TemaSekai(oscuro) {
        val fondo = MaterialTheme.colorScheme.surface
        val texto = MaterialTheme.colorScheme.onSurface
        LaunchedEffect(fondo, texto) { alCambiarColores(fondo, texto) }

        // Todos los textos de las pantallas salen del idioma elegido
        CompositionLocalProvider(LocalTextos provides textosDe(ajustes.idioma)) {
            // Se guarda como texto/booleano para que sobreviva al girar la pantalla
            var mundoAbierto by rememberSaveable { mutableStateOf<String?>(null) }
            var enAjustes by rememberSaveable { mutableStateOf(false) }
            var enConstelacion by rememberSaveable { mutableStateOf(false) }
            val pantalla = when {
                enAjustes -> Pantalla.Ajustes
                mundoAbierto != null -> Pantalla.DetalleMundo(mundoAbierto!!)
                enConstelacion -> Pantalla.Constelacion
                else -> Pantalla.ListaMundos
            }
            val mundosVm = viewModel { MundosViewModel(repositorio) }

            when (pantalla) {
                Pantalla.ListaMundos -> PantallaMundos(
                    vm = mundosVm,
                    alAbrirMundo = { mundoAbierto = it },
                    alAbrirAjustes = { enAjustes = true },
                    alAbrirConstelacion = { enConstelacion = true },
                )
                // Al volver de un mundo abierto desde aquí, se vuelve a la constelación
                Pantalla.Constelacion -> PantallaConstelacion(
                    vm = mundosVm,
                    alVolver = { enConstelacion = false },
                    alAbrirMundo = { mundoAbierto = it },
                )
                is Pantalla.DetalleMundo -> PantallaMundo(
                    // "key" hace que cada mundo tenga su propio ViewModel
                    vm = viewModel(key = pantalla.mundoId) { MundoViewModel(repositorio, pantalla.mundoId) },
                    alVolver = { mundoAbierto = null },
                )
                Pantalla.Ajustes -> PantallaAjustes(vm = ajustesVm, alVolver = { enAjustes = false })
            }
        }
    }
}

/** Tema Material 3 con colores morados y dorados, en modo claro u oscuro. */
@Composable
fun TemaSekai(oscuro: Boolean, contenido: @Composable () -> Unit) {
    val colores = if (oscuro) {
        darkColorScheme(
            primary = Color(0xFFCDB8FF),
            secondary = Color(0xFFE9C46A),
            background = Color(0xFF121018),
            surface = Color(0xFF121018),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF5B3FA8),
            secondary = Color(0xFF9A6B00),
        )
    }
    MaterialTheme(colorScheme = colores, content = contenido)
}
