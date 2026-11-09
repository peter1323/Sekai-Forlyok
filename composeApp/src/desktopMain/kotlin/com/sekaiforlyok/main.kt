package com.sekaiforlyok

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.sekaiforlyok.ui.App

/** Arranque de la app de escritorio: abre una ventana con la interfaz común. */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Sekai Forlyok",
        state = rememberWindowState(width = 1000.dp, height = 750.dp),
    ) {
        App()
    }
}
