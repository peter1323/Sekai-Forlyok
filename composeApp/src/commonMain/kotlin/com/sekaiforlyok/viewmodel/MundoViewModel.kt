package com.sekaiforlyok.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sekaiforlyok.bytesAImagen
import com.sekaiforlyok.data.Repositorio
import com.sekaiforlyok.model.Faccion
import com.sekaiforlyok.model.Lugar
import com.sekaiforlyok.model.Mundo
import com.sekaiforlyok.model.Personaje
import com.sekaiforlyok.model.Relacion
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Lógica de la pantalla de un mundo (las 5 pestañas).
 * Cada cambio crea una copia nueva del mundo y la guarda en disco al momento.
 */
class MundoViewModel(private val repo: Repositorio, private val mundoId: String) : ViewModel() {

    // El mundo actual. Es null si se ha borrado (la pantalla vuelve atrás en ese caso).
    val mundo: StateFlow<Mundo?> = repo.mundos
        .map { lista -> lista.find { it.id == mundoId } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, repo.mundos.value.find { it.id == mundoId })

    /** Aplica un cambio al mundo y lo guarda. Todas las funciones de abajo pasan por aquí. */
    private fun cambiar(cambio: (Mundo) -> Mundo) {
        repo.guardar(repo.mundos.value.map { if (it.id == mundoId) cambio(it) else it })
    }

    // ---------- PERSONAJES ----------

    /** Añade un personaje nuevo o guarda los cambios de uno existente (mismo id). */
    fun guardarPersonaje(p: Personaje) = cambiar { m ->
        m.copy(personajes = m.personajes.reemplazarOAnadir(p.copy(nombre = p.nombre.trim())) { it.id })
    }

    fun borrarPersonaje(id: String) = cambiar { it.borrarPersonaje(id) }

    // ---------- FACCIONES ----------

    fun guardarFaccion(f: Faccion) = cambiar { m ->
        m.copy(facciones = m.facciones.reemplazarOAnadir(f.copy(nombre = f.nombre.trim())) { it.id })
    }

    fun borrarFaccion(id: String) = cambiar { it.borrarFaccion(id) }

    // ---------- LUGARES ----------

    /** Guarda un lugar. Si el padre elegido no está permitido (sería un bucle), lo deja sin padre. */
    fun guardarLugar(l: Lugar) = cambiar { m ->
        val padreValido = l.padreId == null || m.padresPosibles(l.id).any { it.id == l.padreId }
        val limpio = l.copy(nombre = l.nombre.trim(), padreId = if (padreValido) l.padreId else null)
        m.copy(lugares = m.lugares.reemplazarOAnadir(limpio) { it.id })
    }

    fun borrarLugar(id: String) = cambiar { it.borrarLugar(id) }

    // ---------- RELACIONES ----------

    /** Guarda una relación. Devuelve false si no es válida (un personaje consigo mismo). */
    fun guardarRelacion(r: Relacion): Boolean {
        if (r.personaje1Id == r.personaje2Id) return false
        cambiar { m -> m.copy(relaciones = m.relaciones.reemplazarOAnadir(r.copy(nota = r.nota.trim())) { it.id }) }
        return true
    }

    fun borrarRelacion(id: String) = cambiar { m -> m.copy(relaciones = m.relaciones.filter { it.id != id }) }

    // ---------- MAPA ----------

    /** Coloca un lugar en el mapa. x e y van de 0 a 1 (proporción del ancho y alto). */
    fun colocarEnMapa(lugarId: String, x: Float, y: Float) = cambiar { m ->
        m.copy(lugares = m.lugares.map {
            if (it.id == lugarId) it.copy(x = x.coerceIn(0f, 1f), y = y.coerceIn(0f, 1f)) else it
        })
    }

    fun quitarDelMapa(lugarId: String) = cambiar { m ->
        m.copy(lugares = m.lugares.map { if (it.id == lugarId) it.copy(x = null, y = null) else it })
    }

    /**
     * Copia la imagen elegida a la carpeta de la app y la pone como mapa.
     * La imagen anterior se borra. Se hace en segundo plano porque leer el archivo puede tardar.
     */
    fun ponerImagenMapa(archivo: PlatformFile) {
        viewModelScope.launch {
            val bytes = archivo.readBytes()
            if (bytesAImagen(bytes) == null) return@launch // No es una imagen válida
            val extension = archivo.extension.ifBlank { "png" }.lowercase()
            // Nombre distinto cada vez para que la pantalla no muestre la imagen vieja
            val ruta = repo.guardarImagen("mapa_${mundoId}_${nuevoId()}.$extension", bytes)
            borrarImagenAnterior()
            cambiar { it.copy(rutaMapa = ruta, semillaMapa = null) }
        }
    }

    /** Genera un terreno aleatorio nuevo. Solo se guarda la semilla; la imagen se recrea al mostrarla. */
    fun generarMapa() {
        borrarImagenAnterior()
        cambiar { it.copy(rutaMapa = null, semillaMapa = Random.nextLong()) }
    }

    private fun borrarImagenAnterior() {
        mundo.value?.rutaMapa?.let { repo.borrarImagen(it) }
    }

    /** Devuelve la imagen del mapa para dibujarla (o null si no hay mapa). */
    fun imagenMapa(m: Mundo): ImageBitmap? = when {
        m.semillaMapa != null -> generarTerreno(m.semillaMapa)
        m.rutaMapa != null -> repo.leerImagen(m.rutaMapa)?.let { bytesAImagen(it) }
        else -> null
    }
}
