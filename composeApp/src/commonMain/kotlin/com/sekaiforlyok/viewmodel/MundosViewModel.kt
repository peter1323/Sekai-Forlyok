package com.sekaiforlyok.viewmodel

import androidx.lifecycle.ViewModel
import com.sekaiforlyok.data.Repositorio
import com.sekaiforlyok.model.Mundo
import kotlinx.coroutines.flow.StateFlow

/** Lógica de la pantalla "Lista de mundos": crear, editar y borrar mundos. */
class MundosViewModel(private val repo: Repositorio) : ViewModel() {

    // La pantalla observa esta lista y se redibuja sola cuando cambia
    val mundos: StateFlow<List<Mundo>> = repo.mundos

    /** Crea un mundo nuevo (si [id] es null) o edita el nombre y la descripción de uno existente. */
    fun guardarMundo(id: String?, nombre: String, descripcion: String) {
        val actuales = repo.mundos.value
        val nuevos = if (id == null) {
            actuales + Mundo(id = nuevoId(), nombre = nombre.trim(), descripcion = descripcion.trim())
        } else {
            actuales.map { if (it.id == id) it.copy(nombre = nombre.trim(), descripcion = descripcion.trim()) else it }
        }
        repo.guardar(nuevos)
    }

    /** Borra un mundo con todo su contenido, incluida la imagen de su mapa. */
    fun borrarMundo(id: String) {
        repo.mundos.value.find { it.id == id }?.rutaMapa?.let { repo.borrarImagen(it) }
        repo.guardar(repo.mundos.value.filter { it.id != id })
    }
}
