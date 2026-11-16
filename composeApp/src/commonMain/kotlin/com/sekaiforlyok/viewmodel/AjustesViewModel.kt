package com.sekaiforlyok.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sekaiforlyok.data.Repositorio
import com.sekaiforlyok.model.Ajustes
import com.sekaiforlyok.model.Idioma
import com.sekaiforlyok.model.Tema
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Lógica de la pantalla de ajustes: tema claro/oscuro, idioma y copia de seguridad. */
class AjustesViewModel(private val repo: Repositorio) : ViewModel() {

    val ajustes: StateFlow<Ajustes> = repo.ajustes

    fun cambiarTema(tema: Tema) = repo.guardarAjustes(repo.ajustes.value.copy(tema = tema))

    fun cambiarIdioma(idioma: Idioma) = repo.guardarAjustes(repo.ajustes.value.copy(idioma = idioma))

    /** Guarda todos los mundos (con sus imágenes) en [archivo]. Avisa con true si ha ido bien. */
    fun exportarCopia(archivo: PlatformFile, alTerminar: (Boolean) -> Unit) {
        viewModelScope.launch {
            // Pasar las imágenes a texto puede tardar: se hace fuera del hilo de la pantalla
            val ok = runCatching { archivo.write(withContext(Dispatchers.Default) { repo.exportar() }.encodeToByteArray()) }.isSuccess
            alTerminar(ok)
        }
    }

    /** Lee una copia de [archivo] y añade sus mundos. Avisa con cuántos trae, o null si no es una copia válida. */
    fun importarCopia(archivo: PlatformFile, alTerminar: (Int?) -> Unit) {
        viewModelScope.launch {
            val texto = runCatching { archivo.readBytes().decodeToString() }.getOrNull()
            alTerminar(texto?.let { repo.importar(it) })
        }
    }
}
