package com.sekaiforlyok.data

import com.sekaiforlyok.model.Ajustes
import com.sekaiforlyok.model.Mundo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.json.Json

/**
 * Guarda y carga todos los mundos en un archivo JSON (mundos.json) dentro de [carpeta].
 * También guarda y borra las imágenes de los mapas.
 * Solo lo usan los ViewModel; las pantallas nunca lo tocan.
 */
class Repositorio(private val carpeta: String) {

    private val json = Json {
        prettyPrint = true          // JSON legible si alguien abre el archivo
        ignoreUnknownKeys = true    // Si en el futuro se quitan campos, no falla al leer
    }
    private val archivo = Path(carpeta, "mundos.json")

    // Lista de mundos en memoria. Los ViewModel la observan para enterarse de los cambios.
    private val _mundos = MutableStateFlow(cargar())
    val mundos: StateFlow<List<Mundo>> = _mundos.asStateFlow()

    /** Lee el JSON del disco. Si no existe, empieza con la lista vacía. */
    private fun cargar(): List<Mundo> {
        SystemFileSystem.createDirectories(Path(carpeta))
        if (!SystemFileSystem.exists(archivo)) return emptyList()
        val texto = SystemFileSystem.source(archivo).buffered().use { it.readString() }
        return try {
            json.decodeFromString<List<Mundo>>(texto)
        } catch (e: Exception) {
            // Archivo dañado: se guarda una copia para no perder los datos y se empieza de cero
            SystemFileSystem.atomicMove(archivo, Path(carpeta, "mundos-dañado.json"))
            emptyList()
        }
    }

    /** Sustituye la lista entera de mundos y la guarda en disco. */
    fun guardar(nuevos: List<Mundo>) {
        _mundos.value = nuevos
        escribir(archivo, json.encodeToString(nuevos))
    }

    // ---------- AJUSTES (tema e idioma) ----------

    private val archivoAjustes = Path(carpeta, "ajustes.json")

    private val _ajustes = MutableStateFlow(cargarAjustes())
    val ajustes: StateFlow<Ajustes> = _ajustes.asStateFlow()

    /** Si no hay ajustes guardados (o están dañados) se usan los de por defecto: no se pierde nada importante. */
    private fun cargarAjustes(): Ajustes = runCatching {
        json.decodeFromString<Ajustes>(SystemFileSystem.source(archivoAjustes).buffered().use { it.readString() })
    }.getOrDefault(Ajustes())

    fun guardarAjustes(nuevos: Ajustes) {
        _ajustes.value = nuevos
        escribir(archivoAjustes, json.encodeToString(nuevos))
    }

    /**
     * Escribe primero en un archivo temporal y luego lo renombra:
     * así, si la app se cierra a mitad, el archivo anterior sigue intacto.
     */
    private fun escribir(destino: Path, texto: String) {
        val temporal = Path("$destino.tmp")
        SystemFileSystem.sink(temporal).buffered().use { it.writeString(texto) }
        SystemFileSystem.atomicMove(temporal, destino)
    }

    /** Guarda una imagen en la carpeta de la app y devuelve su ruta. */
    fun guardarImagen(nombre: String, bytes: ByteArray): String {
        val ruta = Path(carpeta, nombre)
        SystemFileSystem.sink(ruta).buffered().use { it.write(bytes) }
        return ruta.toString()
    }

    /** Lee una imagen guardada (null si ya no existe). */
    fun leerImagen(ruta: String): ByteArray? {
        val path = Path(ruta)
        if (!SystemFileSystem.exists(path)) return null
        return SystemFileSystem.source(path).buffered().use { it.readByteArray() }
    }

    fun borrarImagen(ruta: String) {
        SystemFileSystem.delete(Path(ruta), mustExist = false)
    }
}
