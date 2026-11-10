package com.sekaiforlyok

import androidx.compose.ui.geometry.Offset
import com.sekaiforlyok.model.Faccion
import com.sekaiforlyok.model.Lugar
import com.sekaiforlyok.model.Mundo
import com.sekaiforlyok.model.Personaje
import com.sekaiforlyok.model.Rango
import com.sekaiforlyok.model.Relacion
import com.sekaiforlyok.model.TipoRelacionFaccion
import com.sekaiforlyok.ui.distanciaASegmento
import com.sekaiforlyok.viewmodel.alturaTerreno
import com.sekaiforlyok.viewmodel.arbolLugares
import com.sekaiforlyok.viewmodel.borrarFaccion
import com.sekaiforlyok.viewmodel.borrarLugar
import com.sekaiforlyok.viewmodel.borrarPersonaje
import com.sekaiforlyok.viewmodel.cambiarEnlaces
import com.sekaiforlyok.viewmodel.parejasEnlazadas
import com.sekaiforlyok.viewmodel.sinMundo
import com.sekaiforlyok.viewmodel.mundosDePrueba
import com.sekaiforlyok.viewmodel.padresPosibles
import com.sekaiforlyok.viewmodel.personajesOrdenados
import com.sekaiforlyok.viewmodel.ponerMiembros
import com.sekaiforlyok.viewmodel.ponerNacidos
import com.sekaiforlyok.viewmodel.relacionarFacciones
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
    fun elEjemploDeAethelLlegaAlNivel5() {
        assertEquals(5, mundosDePrueba()[0].arbolLugares().maxOf { it.second })
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

    @Test
    fun losEnlacesEntreMundosSonSimetricosYSeLimpianAlBorrar() {
        val mundos = listOf(Mundo("a", "A"), Mundo("b", "B"), Mundo("c", "C"))
        // A se enlaza con B y C (y consigo mismo, que se ignora)
        val enlazados = mundos.cambiarEnlaces("a", setOf("a", "b", "c"))
        assertEquals(listOf("b", "c"), enlazados.first { it.id == "a" }.enlaces)
        assertEquals(listOf("a"), enlazados.first { it.id == "b" }.enlaces)
        assertEquals(listOf("a" to "b", "a" to "c"), enlazados.parejasEnlazadas())

        // Quitar el enlace con C desde A también lo quita de C
        val sinC = enlazados.cambiarEnlaces("a", setOf("b"))
        assertEquals(emptyList(), sinC.first { it.id == "c" }.enlaces)

        // Al borrar B, nadie sigue apuntando a él
        assertEquals(listOf("c"), enlazados.sinMundo("b").first { it.id == "a" }.enlaces)
    }

    @Test
    fun alBorrarUnLugarSusHijosPierdenLaPosicionEnSuImagen() {
        val m = Mundo(
            "m", "M",
            lugares = listOf(
                Lugar("reino", "Reino"),
                Lugar("ciudad", "Ciudad", padreId = "reino"),
                Lugar("plaza", "Plaza", padreId = "ciudad", xEnPadre = 0.5f, yEnPadre = 0.5f),
            ),
        )
        val plaza = m.borrarLugar("ciudad").lugares.first { it.id == "plaza" }
        assertEquals("reino", plaza.padreId)
        assertNull(plaza.xEnPadre)
    }

    @Test
    fun laDistanciaAUnaLineaSeMideHastaElTrozoMasCercano() {
        val a = Offset(0f, 0f)
        val b = Offset(10f, 0f)
        assertEquals(3f, distanciaASegmento(Offset(5f, 3f), a, b))   // Encima del medio
        assertEquals(5f, distanciaASegmento(Offset(13f, 4f), a, b))  // Más allá del extremo b
    }

    @Test
    fun alElegirMiembrosDeUnaFaccionSeMuevenOSeQuedanSinFaccion() {
        val m = Mundo(
            "m", "M",
            facciones = listOf(Faccion("f1", "Uno"), Faccion("f2", "Dos")),
            personajes = listOf(
                Personaje("a", "A", faccionId = "f1"), // Se desmarca: queda sin facción
                Personaje("b", "B", faccionId = "f2"), // Se marca: pasa de Dos a Uno
                Personaje("c", "C", faccionId = "f2"), // No se toca
            ),
        )
        val r = m.ponerMiembros("f1", setOf("b")).personajes.associate { it.id to it.faccionId }
        assertEquals(mapOf("a" to null, "b" to "f1", "c" to "f2"), r)
    }

    @Test
    fun entreDosFaccionesSoloHayUnaRelacionYSeBorraConEllas() {
        val m = Mundo("m", "M", facciones = listOf(Faccion("a", "A"), Faccion("b", "B"), Faccion("c", "C")))
            .relacionarFacciones("a", "b", TipoRelacionFaccion.ALIADAS)
            .relacionarFacciones("b", "a", TipoRelacionFaccion.ENEMIGAS) // Cambia la de antes, no añade otra
            .relacionarFacciones("a", "a", TipoRelacionFaccion.ALIADAS)  // Consigo misma: se ignora
            .relacionarFacciones("b", "c", TipoRelacionFaccion.NEUTRALES)
        assertEquals(listOf(TipoRelacionFaccion.ENEMIGAS, TipoRelacionFaccion.NEUTRALES), m.relacionesFacciones.map { it.tipo })
        // Al borrar B desaparecen sus dos relaciones
        assertEquals(emptyList(), m.borrarFaccion("b").relacionesFacciones)
    }
    @Test
    fun alElegirPersonajesDeUnLugarSeMuevenOSeQuedanSinLugar() {
        val m = Mundo(
            "m", "M",
            lugares = listOf(Lugar("l1", "Uno"), Lugar("l2", "Dos")),
            personajes = listOf(
                Personaje("a", "A", lugarId = "l1"), // Se desmarca: queda sin lugar
                Personaje("b", "B", lugarId = "l2"), // Se marca: pasa de Dos a Uno
                Personaje("c", "C", lugarId = "l2"), // No se toca
            ),
        )
        val r = m.ponerNacidos("l1", setOf("b")).personajes.associate { it.id to it.lugarId }
        assertEquals(mapOf("a" to null, "b" to "l1", "c" to "l2"), r)
    }
}
