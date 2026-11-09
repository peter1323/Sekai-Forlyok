package com.sekaiforlyok.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sekaiforlyok.carpetaDatos
import com.sekaiforlyok.data.Repositorio
import com.sekaiforlyok.viewmodel.MundoViewModel
import com.sekaiforlyok.viewmodel.MundosViewModel

/** Las pantallas de la app. La navegación es simplemente cambiar cuál se muestra. */
sealed class Pantalla {
    data object ListaMundos : Pantalla()
    data class DetalleMundo(val mundoId: String) : Pantalla()
}

// Un único repositorio para toda la app. Se crea la primera vez que se usa.
private val repositorio by lazy { Repositorio(carpetaDatos()) }

/** Punto de entrada de la interfaz, común a Android y escritorio. */
@Composable
fun App() {
    TemaSekai {
        // Guardamos solo el id del mundo abierto (texto) para que sobreviva al girar la pantalla
        var mundoAbierto by rememberSaveable { mutableStateOf<String?>(null) }
        val pantalla = mundoAbierto?.let { Pantalla.DetalleMundo(it) } ?: Pantalla.ListaMundos

        when (pantalla) {
            Pantalla.ListaMundos -> PantallaMundos(
                vm = viewModel { MundosViewModel(repositorio) },
                alAbrirMundo = { mundoAbierto = it },
            )
            is Pantalla.DetalleMundo -> PantallaMundo(
                // "key" hace que cada mundo tenga su propio ViewModel
                vm = viewModel(key = pantalla.mundoId) { MundoViewModel(repositorio, pantalla.mundoId) },
                alVolver = { mundoAbierto = null },
            )
        }
    }
}

/** Tema Material 3 con colores morados y dorados. Sigue el modo claro/oscuro del sistema. */
@Composable
fun TemaSekai(contenido: @Composable () -> Unit) {
    val colores = if (isSystemInDarkTheme()) {
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
