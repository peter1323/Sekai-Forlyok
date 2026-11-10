package com.sekaiforlyok.viewmodel

import com.sekaiforlyok.model.Lugar
import com.sekaiforlyok.model.Mundo
import com.sekaiforlyok.model.Personaje
import com.sekaiforlyok.model.RelacionFaccion
import com.sekaiforlyok.model.TipoRelacionFaccion
import kotlin.random.Random

// Reglas del mundo escritas como funciones "puras": reciben un Mundo y devuelven otro nuevo,
// sin tocar el disco. Así se pueden probar fácilmente (ver ReglasMundoTest).
// Las usan los ViewModel.

/** Crea un identificador único corto, por ejemplo "3k9x0q2a1b". */
fun nuevoId(): String = Random.nextLong().toULong().toString(36)

/** Si ya existe un elemento con el mismo id lo sustituye; si no, lo añade al final. */
fun <T> List<T>.reemplazarOAnadir(nuevo: T, id: (T) -> String): List<T> =
    if (any { id(it) == id(nuevo) }) map { if (id(it) == id(nuevo)) nuevo else it }
    else this + nuevo

/** Personajes ordenados de rango S a E (y por nombre dentro del mismo rango). */
fun Mundo.personajesOrdenados(): List<Personaje> =
    personajes.sortedWith(compareByDescending<Personaje> { it.rango }.thenBy { it.nombre.lowercase() })

/** Cuántos personajes pertenecen a una facción. */
fun Mundo.miembros(faccionId: String): Int = personajes.count { it.faccionId == faccionId }

/** Ids de todos los lugares que están dentro de [lugarId], a cualquier profundidad. */
fun Mundo.descendientes(lugarId: String): Set<String> {
    val resultado = mutableSetOf<String>()
    var pendientes = listOf(lugarId)
    while (pendientes.isNotEmpty()) {
        // Hijos directos de los lugares pendientes que aún no hemos visto
        val hijos = lugares.filter { it.padreId in pendientes && it.id !in resultado && it.id != lugarId }
        resultado += hijos.map { it.id }
        pendientes = hijos.map { it.id }
    }
    return resultado
}

/**
 * Lugares que pueden ser el "padre" de [lugarId].
 * Un lugar no puede estar dentro de sí mismo ni de uno de sus sublugares.
 */
fun Mundo.padresPosibles(lugarId: String?): List<Lugar> {
    if (lugarId == null) return lugares // Lugar nuevo: cualquiera vale
    val prohibidos = descendientes(lugarId) + lugarId
    return lugares.filter { it.id !in prohibidos }
}

/**
 * Lugares ordenados como un árbol: cada lugar va seguido de sus hijos.
 * Devuelve parejas (lugar, nivel) donde nivel 0 es la raíz; sirve para la sangría.
 */
fun Mundo.arbolLugares(): List<Pair<Lugar, Int>> {
    val ids = lugares.map { it.id }.toSet()
    val resultado = mutableListOf<Pair<Lugar, Int>>()
    val vistos = mutableSetOf<String>() // Evita bucles infinitos si los datos estuvieran mal

    fun anadir(lugar: Lugar, nivel: Int) {
        if (!vistos.add(lugar.id)) return
        resultado += lugar to nivel
        lugares.filter { it.padreId == lugar.id }.sortedBy { it.nombre.lowercase() }
            .forEach { anadir(it, nivel + 1) }
    }
    // Raíces: sin padre o con un padre que ya no existe
    lugares.filter { it.padreId == null || it.padreId !in ids }.sortedBy { it.nombre.lowercase() }
        .forEach { anadir(it, 0) }
    // Lo que quede (lugares en un bucle por datos dañados) se muestra al final, sin perderlo
    lugares.filter { it.id !in vistos }.forEach { anadir(it, 0) }
    return resultado
}

/** Borra un personaje y todas sus relaciones. */
fun Mundo.borrarPersonaje(id: String): Mundo = copy(
    personajes = personajes.filter { it.id != id },
    relaciones = relaciones.filter { it.personaje1Id != id && it.personaje2Id != id },
)

/** Borra una facción; sus miembros se quedan sin facción y se borran sus alianzas y enemistades. */
fun Mundo.borrarFaccion(id: String): Mundo = copy(
    facciones = facciones.filter { it.id != id },
    relacionesFacciones = relacionesFacciones.filter { it.faccion1Id != id && it.faccion2Id != id },
    personajes = personajes.map { if (it.faccionId == id) it.copy(faccionId = null) else it },
)

/**
 * Borra un lugar. Sus hijos suben un nivel (pasan al padre del lugar borrado)
 * y los personajes que eran de allí se quedan sin lugar.
 */
fun Mundo.borrarLugar(id: String): Mundo {
    val borrado = lugares.find { it.id == id } ?: return this
    return copy(
        lugares = lugares.filter { it.id != id }
            // Su posición dentro de la imagen del lugar borrado ya no sirve
            .map { if (it.padreId == id) it.copy(padreId = borrado.padreId, xEnPadre = null, yEnPadre = null) else it },
        personajes = personajes.map { if (it.lugarId == id) it.copy(lugarId = null) else it },
    )
}

// ---------- Enlaces entre mundos (constelación) ----------
// Un enlace es una línea entre dos mundos. Se guarda en los dos para que siempre sea simétrico.

/** Deja el mundo [id] enlazado exactamente con [otros] (y actualiza también a los demás mundos). */
fun List<Mundo>.cambiarEnlaces(id: String, otros: Set<String>): List<Mundo> {
    val validos = otros - id // Un mundo no se enlaza consigo mismo
    return map { m ->
        when {
            m.id == id -> m.copy(enlaces = validos.filter { o -> any { it.id == o } })
            m.id in validos -> if (id in m.enlaces) m else m.copy(enlaces = m.enlaces + id)
            else -> m.copy(enlaces = m.enlaces - id)
        }
    }
}

/** Quita un mundo de la lista y también los enlaces que apuntaban a él. */
fun List<Mundo>.sinMundo(id: String): List<Mundo> =
    filter { it.id != id }.map { if (id in it.enlaces) it.copy(enlaces = it.enlaces - id) else it }

/** Parejas de mundos enlazados, cada una una sola vez (para dibujar las líneas). */
fun List<Mundo>.parejasEnlazadas(): List<Pair<String, String>> {
    val ids = map { it.id }.toSet()
    return flatMap { m -> m.enlaces.filter { it in ids && m.id < it }.map { m.id to it } }
}

/**
 * Deja en la facción [faccionId] solo a los personajes de [miembros].
 * Los marcados que estaban en otra facción se cambian a esta; los desmarcados se quedan sin facción.
 */
fun Mundo.ponerMiembros(faccionId: String, miembros: Set<String>): Mundo = copy(
    personajes = personajes.map {
        when {
            it.id in miembros -> it.copy(faccionId = faccionId)
            it.faccionId == faccionId -> it.copy(faccionId = null)
            else -> it
        }
    },
)

/**
 * Deja como personajes de [lugarId] solo a los de [nacidos] (su lugar de origen).
 * Los marcados que eran de otro lugar se cambian a este; los desmarcados se quedan sin lugar.
 */
fun Mundo.ponerNacidos(lugarId: String, nacidos: Set<String>): Mundo = copy(
    personajes = personajes.map {
        when {
            it.id in nacidos -> it.copy(lugarId = lugarId)
            it.lugarId == lugarId -> it.copy(lugarId = null)
            else -> it
        }
    },
)

/** Pone cómo se llevan dos facciones. Si ya tenían una relación, se cambia (nunca hay dos entre las mismas). */
fun Mundo.relacionarFacciones(a: String, b: String, tipo: TipoRelacionFaccion): Mundo {
    if (a == b) return this
    val otras = relacionesFacciones.filterNot { setOf(it.faccion1Id, it.faccion2Id) == setOf(a, b) }
    return copy(relacionesFacciones = otras + RelacionFaccion(nuevoId(), a, b, tipo))
}
