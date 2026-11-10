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
import com.sekaiforlyok.model.Punto
import com.sekaiforlyok.model.Relacion
import com.sekaiforlyok.model.TipoRelacionFaccion
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

    /** Guarda dónde se ha dejado un personaje al arrastrarlo en la constelación de relaciones. */
    fun moverPersonaje(id: String, x: Float, y: Float) = cambiar { m ->
        m.copy(personajes = m.personajes.map { if (it.id == id) it.copy(posX = x, posY = y) else it })
    }

    // ---------- FACCIONES ----------

    /** Guarda una facción y deja como miembros exactamente a los personajes de [miembros]. */
    fun guardarFaccion(f: Faccion, miembros: Set<String>) = cambiar { m ->
        m.copy(facciones = m.facciones.reemplazarOAnadir(f.copy(nombre = f.nombre.trim())) { it.id })
            .ponerMiembros(f.id, miembros)
    }

    fun borrarFaccion(id: String) = cambiar { it.borrarFaccion(id) }

    /** Une dos facciones en la constelación como aliadas, neutrales o enemigas. */
    fun relacionarFacciones(a: String, b: String, tipo: TipoRelacionFaccion) = cambiar { it.relacionarFacciones(a, b, tipo) }

    fun borrarRelacionFaccion(id: String) = cambiar { m ->
        m.copy(relacionesFacciones = m.relacionesFacciones.filter { it.id != id })
    }

    /** Guarda dónde se ha dejado una facción al arrastrarla en su constelación. */
    fun moverFaccion(id: String, x: Float, y: Float) = cambiar { m ->
        m.copy(facciones = m.facciones.map { if (it.id == id) it.copy(posX = x, posY = y) else it })
    }

    // ---------- LUGARES ----------

    /**
     * Guarda un lugar. Si el padre elegido no está permitido (sería un bucle), lo deja sin padre.
     * Si cambia de padre, pierde su posición sobre la imagen del padre anterior.
     */
    fun guardarLugar(l: Lugar, nacidos: Set<String>) = cambiar { m ->
        val padreValido = l.padreId == null || m.padresPosibles(l.id).any { it.id == l.padreId }
        val padre = if (padreValido) l.padreId else null
        val mismoPadre = m.lugares.find { it.id == l.id }?.padreId == padre
        val limpio = l.copy(
            nombre = l.nombre.trim(),
            padreId = padre,
            xEnPadre = if (mismoPadre) l.xEnPadre else null,
            yEnPadre = if (mismoPadre) l.yEnPadre else null,
        )
        m.copy(lugares = m.lugares.reemplazarOAnadir(limpio) { it.id }).ponerNacidos(l.id, nacidos)
    }

    /** Borra un lugar (y su imagen, si tenía). */
    fun borrarLugar(id: String) {
        borrarImagenLugar(id)
        cambiar { it.borrarLugar(id) }
    }

    // ---------- IMAGEN DE UN LUGAR (ciudad, mazmorra…) ----------

    /** Pone una imagen JPG o PNG a un lugar. Sus pines se mantienen al cambiar de imagen. */
    fun ponerImagenLugar(lugarId: String, archivo: PlatformFile) {
        viewModelScope.launch {
            val ruta = copiarImagen(archivo, "lugar_$lugarId") ?: return@launch // No es una imagen válida
            borrarImagenLugar(lugarId)
            cambiarLugar(lugarId) { it.copy(rutaImagen = ruta) }
        }
    }

    /** Quita la imagen de un lugar, sus puntos y la posición de sus sublugares sobre ella. */
    fun quitarImagenLugar(lugarId: String) {
        borrarImagenLugar(lugarId)
        cambiar { m ->
            m.copy(lugares = m.lugares.map {
                when {
                    it.id == lugarId -> it.copy(rutaImagen = null, puntos = emptyList())
                    it.padreId == lugarId -> it.copy(xEnPadre = null, yEnPadre = null)
                    else -> it
                }
            })
        }
    }

    /** Coloca un sublugar sobre la imagen de su padre. x e y van de 0 a 1. */
    fun colocarEnImagen(sublugarId: String, x: Float, y: Float) =
        cambiarLugar(sublugarId) { it.copy(xEnPadre = x.coerceIn(0f, 1f), yEnPadre = y.coerceIn(0f, 1f)) }

    fun quitarDeImagen(sublugarId: String) = cambiarLugar(sublugarId) { it.copy(xEnPadre = null, yEnPadre = null) }

    /** Añade un punto libre con nombre ("Plaza central") sobre la imagen de un lugar. */
    fun anadirPunto(lugarId: String, nombre: String, x: Float, y: Float) = cambiarLugar(lugarId) {
        it.copy(puntos = it.puntos + Punto(nuevoId(), nombre.trim(), x.coerceIn(0f, 1f), y.coerceIn(0f, 1f)))
    }

    fun borrarPunto(lugarId: String, puntoId: String) =
        cambiarLugar(lugarId) { l -> l.copy(puntos = l.puntos.filter { it.id != puntoId }) }

    /** Imagen de un lugar para dibujarla (o null si no tiene). */
    fun imagenLugar(l: Lugar): ImageBitmap? = l.rutaImagen?.let { repo.leerImagen(it) }?.let { bytesAImagen(it) }

    private fun cambiarLugar(id: String, cambio: (Lugar) -> Lugar) = cambiar { m ->
        m.copy(lugares = m.lugares.map { if (it.id == id) cambio(it) else it })
    }

    private fun borrarImagenLugar(lugarId: String) {
        mundo.value?.lugares?.find { it.id == lugarId }?.rutaImagen?.let { repo.borrarImagen(it) }
    }

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
    fun colocarEnMapa(lugarId: String, x: Float, y: Float) =
        cambiarLugar(lugarId) { it.copy(x = x.coerceIn(0f, 1f), y = y.coerceIn(0f, 1f)) }

    fun quitarDelMapa(lugarId: String) = cambiarLugar(lugarId) { it.copy(x = null, y = null) }

    /**
     * Copia la imagen elegida a la carpeta de la app y la pone como mapa.
     * La imagen anterior se borra. Se hace en segundo plano porque leer el archivo puede tardar.
     */
    fun ponerImagenMapa(archivo: PlatformFile) {
        viewModelScope.launch {
            val ruta = copiarImagen(archivo, "mapa_$mundoId") ?: return@launch // No es una imagen válida
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
    fun imagenMapa(m: Mundo): ImageBitmap? = imagenMapa(repo, m)

    /**
     * Copia una imagen elegida por el usuario a la carpeta de la app y devuelve su ruta.
     * Devuelve null si el archivo no es una imagen válida.
     */
    private suspend fun copiarImagen(archivo: PlatformFile, prefijo: String): String? {
        val bytes = archivo.readBytes()
        if (bytesAImagen(bytes) == null) return null
        val extension = archivo.extension.ifBlank { "png" }.lowercase()
        // Nombre distinto cada vez para que la pantalla no muestre la imagen vieja
        return repo.guardarImagen("${prefijo}_${nuevoId()}.$extension", bytes)
    }
}

/** Imagen del mapa de un mundo: el terreno generado o la imagen cargada (null si no tiene). */
fun imagenMapa(repo: Repositorio, m: Mundo): ImageBitmap? = when {
    m.semillaMapa != null -> generarTerreno(m.semillaMapa)
    m.rutaMapa != null -> repo.leerImagen(m.rutaMapa)?.let { bytesAImagen(it) }
    else -> null
}
