package com.sekaiforlyok

import com.sekaiforlyok.model.Faccion
import com.sekaiforlyok.model.Lugar
import com.sekaiforlyok.model.Mundo
import com.sekaiforlyok.model.Personaje
import com.sekaiforlyok.model.Rango
import com.sekaiforlyok.model.Relacion
import com.sekaiforlyok.viewmodel.alturaTerreno
import com.sekaiforlyok.viewmodel.arbolLugares
import com.sekaiforlyok.viewmodel.borrarFaccion
import com.sekaiforlyok.viewmodel.borrarLugar
import com.sekaiforlyok.viewmodel.borrarPersonaje
import com.sekaiforlyok.viewmodel.mundosDePrueba
import com.sekaiforlyok.viewmodel.padresPosibles
import com.sekaiforlyok.viewmodel.personajesOrdenados
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Pruebas de las reglas del mundo. Se ejecutan con: gradlew :composeApp:desktopTest

class ReglasMundoTest {

    // Mundo de ejemplo: Eastaria (continente) > Aethel (reino) > Seúl Nueva (ciudad)
    private val mundo = Mundo(
        id = "m",
        nombre = "Prueba",
        lugares = listOf(
            Lugar("continente", "Eastaria"),
            Lugar("reino", "Aethel", padreId = "continente"),
            Lugar("ciudad", "Seúl Nueva", padreId = "reino"),
        ),
        facciones = listOf(Faccion("cuervo", "Gremio Cuervo")),
        personajes = listOf(
            Personaje("jin", "Jin-woo", rango = Rango.S, faccionId = "cuervo", lugarId = "reino"),
            Personaje("mina", "Mina", rango = Rango.C, faccionId = "cuervo"),
            Personaje("bo", "Bo", rango = Rango.E),
        ),
        relaciones = listOf(
            Relacion("r1", "jin", "mina"),
            Relacion("r2", "mina", "bo"),
        ),
    )

    @Test
    fun personajesOrdenadosDeSaE() {
        assertEquals(listOf("jin", "mina", "bo"), mundo.personajesOrdenados().map { it.id })
    }

    @Test
    fun unLugarNoPuedeEstarDentroDeSiMismoNiDeSusSublugares() {
        // El continente no puede ir dentro de él mismo, del reino ni de la ciudad
        assertEquals(emptyList(), mundo.padresPosibles("continente").map { it.id })
        // La ciudad sí puede ir dentro del continente o del reino
        assertEquals(listOf("continente", "reino"), mundo.padresPosibles("ciudad").map { it.id })
    }

    @Test
    fun alBorrarUnLugarSusHijosSubenUnNivelYLosPersonajesQuedanSinLugar() {
        val despues = mundo.borrarLugar("reino")
        assertEquals("continente", despues.lugares.first { it.id == "ciudad" }.padreId)
        assertNull(despues.personajes.first { it.id == "jin" }.lugarId)
    }

    @Test
    fun alBorrarUnaFaccionSusMiembrosQuedanSinFaccion() {
        val despues = mundo.borrarFaccion("cuervo")
        assertEquals(true, despues.personajes.all { it.faccionId == null })
    }

    @Test
    fun alBorrarUnPersonajeSeBorranSusRelaciones() {
        val despues = mundo.borrarPersonaje("mina")
        assertEquals(emptyList(), despues.relaciones)
    }

    @Test
    fun elArbolMuestraCadaLugarConSuNivel() {
        assertEquals(listOf("Eastaria" to 0, "Aethel" to 1, "Seúl Nueva" to 2), mundo.arbolLugares().map { it.first.nombre to it.second })
    }

    @Test
    fun losDatosDePruebaSonCoherentes() {
        // Todo lo que se referencia (facción, lugar, padre, personajes de una relación) debe existir
        mundosDePrueba().forEach { m ->
            val personajes = m.personajes.map { it.id }.toSet()
            val lugares = m.lugares.map { it.id }.toSet()
            val facciones = m.facciones.map { it.id }.toSet()
            m.personajes.forEach { p ->
                assert(p.faccionId == null || p.faccionId in facciones) { "${p.nombre}: facción" }
                assert(p.lugarId == null || p.lugarId in lugares) { "${p.nombre}: lugar" }
            }
            m.lugares.forEach { l -> assert(l.padreId == null || l.padreId in lugares) { "${l.nombre}: padre" } }
            m.relaciones.forEach { r ->
                assert(r.personaje1Id in personajes && r.personaje2Id in personajes && r.personaje1Id != r.personaje2Id) { r.id }
            }
        }
    }

    @Test
    fun elTerrenoTieneMarYTierra() {
        // Con cualquier semilla debe haber agua (bordes) y tierra (centro)
        repeat(5) { semilla ->
            val alturas = (0 until 240 step 4).flatMap { y -> (0 until 360 step 4).map { x -> alturaTerreno(semilla.toLong(), x, y) } }
            val agua = alturas.count { it < 0.38f }.toFloat() / alturas.size
            assert(agua in 0.25f..0.8f) { "Semilla $semilla: $agua de agua" }
        }
    }
}
