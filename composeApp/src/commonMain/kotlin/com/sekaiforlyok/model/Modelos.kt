package com.sekaiforlyok.model

import kotlinx.serialization.Serializable

// Estos son los datos de la app. @Serializable permite guardarlos y leerlos como JSON.
// Cada elemento tiene un "id" único para poder referirse a él desde otros (por ejemplo,
// un personaje guarda el id de su facción en lugar de copiar la facción entera).

/** Un mundo completo con todo lo que contiene. */
@Serializable
data class Mundo(
    val id: String,
    val nombre: String,
    val descripcion: String = "",
    val rutaMapa: String? = null,     // Imagen del mapa copiada en la carpeta de la app
    val semillaMapa: Long? = null,    // Si el mapa lo generó la app, se guarda solo su semilla
    val personajes: List<Personaje> = emptyList(),
    val facciones: List<Faccion> = emptyList(),
    val lugares: List<Lugar> = emptyList(),
    val relaciones: List<Relacion> = emptyList(),
)

/** Rangos de poder, de menor (E) a mayor (S). El orden importa para ordenar la lista. */
enum class Rango { E, D, C, B, A, S }

@Serializable
data class Personaje(
    val id: String,
    val nombre: String,
    val edad: Int? = null,            // null = edad desconocida
    val rango: Rango = Rango.E,
    val poderes: List<String> = emptyList(),
    val faccionId: String? = null,    // null = sin facción
    val lugarId: String? = null,      // Lugar de origen; null = sin lugar
    val descripcion: String = "",     // Descripción e historia
)

@Serializable
data class Faccion(
    val id: String,
    val nombre: String,
    val descripcion: String = "",
)

/** Tipos de lugar. El texto que se ve en pantalla está en ui/Textos.kt (según el idioma). */
enum class TipoLugar { CONTINENTE, REINO, CIUDAD, PUEBLO, MAZMORRA, OTRO }

@Serializable
data class Lugar(
    val id: String,
    val nombre: String,
    val tipo: TipoLugar = TipoLugar.OTRO,
    val descripcion: String = "",
    val padreId: String? = null,      // El lugar que lo contiene; null = está en lo más alto
    val x: Float? = null,             // Posición en el mapa, de 0 (izquierda) a 1 (derecha)
    val y: Float? = null,             // Posición en el mapa, de 0 (arriba) a 1 (abajo)
)

/** Tipos de relación. El texto que se ve en pantalla está en ui/Textos.kt (según el idioma). */
enum class TipoRelacion { AMIGOS, RIVALES, ENEMIGOS, PAREJA, FAMILIA, MAESTRO_ALUMNO, ALIADOS, OTRO }

/** Ajustes de la app. Se guardan aparte, en ajustes.json. */
@Serializable
data class Ajustes(
    val tema: Tema = Tema.SISTEMA,
    val idioma: Idioma = Idioma.ES,
)

enum class Tema { SISTEMA, CLARO, OSCURO }   // SISTEMA = el mismo modo que Windows/Android
enum class Idioma { ES, EN }

@Serializable
data class Relacion(
    val id: String,
    val personaje1Id: String,
    val personaje2Id: String,
    val tipo: TipoRelacion = TipoRelacion.AMIGOS,
    val nota: String = "",
)
