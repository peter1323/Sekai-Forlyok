package com.sekaiforlyok

import com.sekaiforlyok.data.Repositorio
import com.sekaiforlyok.model.Ajustes
import com.sekaiforlyok.model.Idioma
import com.sekaiforlyok.model.Lugar
import com.sekaiforlyok.model.Tema
import com.sekaiforlyok.model.Mundo
import com.sekaiforlyok.model.Personaje
import com.sekaiforlyok.model.Rango
import kotlinx.io.files.Path
import kotlinx.io.files.SystemTemporaryDirectory
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class RepositorioTest {

    @Test
    fun loQueSeGuardaSeVuelveACargarIgual() {
        // Carpeta temporal nueva para no tocar los datos reales
        val carpeta = Path(SystemTemporaryDirectory, "sekai-test-${Random.nextInt(1_000_000)}").toString()
        val mundos = listOf(
            Mundo(
                id = "m1",
                nombre = "Aethel",
                personajes = listOf(Personaje("p1", "Jin-woo", edad = 24, rango = Rango.S, poderes = listOf("Sombras"))),
                lugares = listOf(Lugar("l1", "Seúl Nueva", x = 0.3f, y = 0.6f)),
            ),
        )
        val ajustes = Ajustes(tema = Tema.OSCURO, idioma = Idioma.EN)
        Repositorio(carpeta).apply { guardar(mundos); guardarAjustes(ajustes) }

        // Un repositorio nuevo lee los archivos desde el disco
        val leido = Repositorio(carpeta)
        assertEquals(mundos, leido.mundos.value)
        assertEquals(ajustes, leido.ajustes.value)
    }

    @Test
    fun unaCopiaDeSeguridadPasaLosMundosYSusImagenesAOtraCarpeta() {
        fun carpetaNueva() = Path(SystemTemporaryDirectory, "sekai-test-${Random.nextInt(1_000_000)}").toString()
        val origen = Repositorio(carpetaNueva())
        val ruta = origen.guardarImagen("mapa_m1.png", byteArrayOf(1, 2, 3))
        origen.guardar(listOf(Mundo("m1", "Aethel", rutaMapa = ruta, lugares = listOf(Lugar("l1", "Seúl", rutaImagen = ruta)))))
        val copia = origen.exportar()

        // Otra carpeta (como otro dispositivo) que ya tiene una versión vieja de "m1" y otro mundo
        val destino = Repositorio(carpetaNueva())
        destino.guardar(listOf(Mundo("m1", "Viejo"), Mundo("m2", "Otro")))
        assertEquals(1, destino.importar(copia))

        val m1 = destino.mundos.value.first { it.id == "m1" }
        assertEquals("Aethel", m1.nombre)                                       // Sustituido por el de la copia
        assertEquals(listOf("m2", "m1"), destino.mundos.value.map { it.id })    // El otro se conserva
        assertEquals(listOf<Byte>(1, 2, 3), destino.leerImagen(m1.rutaMapa!!)!!.toList()) // La imagen viaja dentro
        assertEquals(m1.rutaMapa, m1.lugares[0].rutaImagen)

        // Un archivo que no es una copia no cambia nada
        assertEquals(null, destino.importar("[]"))
        assertEquals(2, destino.mundos.value.size)
    }
}
