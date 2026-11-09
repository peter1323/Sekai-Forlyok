package com.sekaiforlyok

import com.sekaiforlyok.data.Repositorio
import com.sekaiforlyok.model.Lugar
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
        Repositorio(carpeta).guardar(mundos)

        // Un repositorio nuevo lee el archivo desde el disco
        assertEquals(mundos, Repositorio(carpeta).mundos.value)
    }
}
