package com.sekaiforlyok

import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
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
        // Icono de la brújula (está en desktopMain/resources; sustituye la taza de Java)
        icon = remember {
            BitmapPainter(bytesAImagen(object {}.javaClass.getResourceAsStream("/icono.png")!!.readBytes())!!)
        },
        state = rememberWindowState(width = 1000.dp, height = 750.dp),
    ) {
        // Cada vez que cambia el tema, la barra de la ventana toma los mismos colores
        App(alCambiarColores = { fondo, texto -> colorearBarra(window, fondo, texto) })
    }
}
