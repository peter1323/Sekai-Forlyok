package com.sekaiforlyok.viewmodel

import androidx.lifecycle.ViewModel
import com.sekaiforlyok.data.Repositorio
import com.sekaiforlyok.model.Ajustes
import com.sekaiforlyok.model.Idioma
import com.sekaiforlyok.model.Tema
import kotlinx.coroutines.flow.StateFlow

/** Lógica de la pantalla de ajustes: tema claro/oscuro e idioma. */
class AjustesViewModel(private val repo: Repositorio) : ViewModel() {

    val ajustes: StateFlow<Ajustes> = repo.ajustes

    fun cambiarTema(tema: Tema) = repo.guardarAjustes(repo.ajustes.value.copy(tema = tema))

    fun cambiarIdioma(idioma: Idioma) = repo.guardarAjustes(repo.ajustes.value.copy(idioma = idioma))
}
