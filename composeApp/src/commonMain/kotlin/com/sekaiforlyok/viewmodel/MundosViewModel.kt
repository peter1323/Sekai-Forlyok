package com.sekaiforlyok.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import com.sekaiforlyok.data.Repositorio
import com.sekaiforlyok.model.Mundo
import kotlinx.coroutines.flow.StateFlow

/** Lógica de la pantalla "Lista de mundos" y de la constelación de mundos. */
class MundosViewModel(private val repo: Repositorio) : ViewModel() {

    // La pantalla observa esta lista y se redibuja sola cuando cambia
    val mundos: StateFlow<List<Mundo>> = repo.mundos

    /**
     * Crea un mundo nuevo (si [id] es null) o edita uno existente.
     * [enlaces] son los mundos con los que queda conectado en la constelación.
     */
    fun guardarMundo(id: String?, nombre: String, descripcion: String, enlaces: Set<String>) {
        val actuales = repo.mundos.value
        val idFinal = id ?: nuevoId()
        val nuevos = if (id == null) {
            actuales + Mundo(id = idFinal, nombre = nombre.trim(), descripcion = descripcion.trim())
        } else {
            actuales.map { if (it.id == id) it.copy(nombre = nombre.trim(), descripcion = descripcion.trim()) else it }
        }
        repo.guardar(nuevos.cambiarEnlaces(idFinal, enlaces))
    }

    /** Guarda dónde se ha dejado un mundo al arrastrarlo en la constelación. */
    fun moverEnConstelacion(id: String, x: Float, y: Float) {
        repo.guardar(repo.mundos.value.map { if (it.id == id) it.copy(posX = x, posY = y) else it })
    }

    /** Añade los mundos de ejemplo (ver DatosDePrueba.kt) sin tocar los que ya existen. */
    fun cargarDatosDePrueba() {
        repo.guardar(repo.mundos.value + mundosDePrueba())
    }

    /** Borra un mundo con todo su contenido, incluidas las imágenes de su mapa y de sus lugares. */
    fun borrarMundo(id: String) {
        repo.mundos.value.find { it.id == id }?.let { m ->
            m.rutaMapa?.let { repo.borrarImagen(it) }
            m.lugares.forEach { l -> l.rutaImagen?.let { repo.borrarImagen(it) } }
        }
        repo.guardar(repo.mundos.value.sinMundo(id))
    }

    /** Imagen del mapa de un mundo (para la miniatura de la constelación), o null si no tiene. */
    fun imagenMapa(m: Mundo): ImageBitmap? = imagenMapa(repo, m)
}
