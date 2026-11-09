package com.sekaiforlyok

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import java.awt.Window
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

// Pinta la barra de título de Windows (la de minimizar, maximizar y cerrar) con los colores de la app.
// Se usa la barra de Windows en vez de dibujar una propia para no perder el cambio de tamaño
// arrastrando los bordes ni el acople a los lados de la pantalla (Snap).

/** Función de Windows (dwmapi.dll) que cambia el aspecto de la barra de una ventana. */
private interface Dwm : Library {
    fun DwmSetWindowAttribute(ventana: Pointer, atributo: Int, valor: IntByReference, tamano: Int): Int
}

// Solo se carga en Windows; en otros sistemas queda null y no se hace nada
private val dwm: Dwm? by lazy {
    if (!System.getProperty("os.name").startsWith("Windows")) null
    else runCatching { Native.load("dwmapi", Dwm::class.java) }.getOrNull()
}

private const val MODO_OSCURO = 20     // Windows 10/11: barra oscura o clara
private const val COLOR_BARRA = 35     // Windows 11: color de fondo de la barra
private const val COLOR_TEXTO = 36     // Windows 11: color del título

fun colorearBarra(ventana: Window, fondo: Color, texto: Color) {
    val api = dwm ?: return
    // La ventana aún no se ha mostrado: lo hacemos en cuanto se abra
    if (!ventana.isDisplayable) {
        ventana.addWindowListener(object : WindowAdapter() {
            override fun windowOpened(e: WindowEvent) = colorearBarra(ventana, fondo, texto)
        })
        return
    }
    val hwnd = Native.getWindowPointer(ventana)
    fun poner(atributo: Int, valor: Int) = api.DwmSetWindowAttribute(hwnd, atributo, IntByReference(valor), 4)
    poner(MODO_OSCURO, if (fondo.luminance() < 0.5f) 1 else 0)
    poner(COLOR_BARRA, colorWindows(fondo))
    poner(COLOR_TEXTO, colorWindows(texto))
}

/** Windows guarda los colores al revés que Compose: 0x00BBGGRR. */
private fun colorWindows(color: Color): Int {
    val argb = color.toArgb()
    val rojo = (argb shr 16) and 0xFF
    val verde = (argb shr 8) and 0xFF
    val azul = argb and 0xFF
    return (azul shl 16) or (verde shl 8) or rojo
}
