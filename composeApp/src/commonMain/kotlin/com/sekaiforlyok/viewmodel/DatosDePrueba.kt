package com.sekaiforlyok.viewmodel

import com.sekaiforlyok.model.Faccion
import com.sekaiforlyok.model.Lugar
import com.sekaiforlyok.model.Mundo
import com.sekaiforlyok.model.Personaje
import com.sekaiforlyok.model.Rango
import com.sekaiforlyok.model.Relacion
import com.sekaiforlyok.model.TipoLugar
import com.sekaiforlyok.model.TipoRelacion

// Mundos de ejemplo para probar la app sin tener que escribirlo todo a mano.
// Los ids de dentro de cada mundo son fijos y legibles ("jin", "cuervo"…): solo tienen que
// ser únicos dentro del mismo mundo. El id del mundo sí es nuevo cada vez.

fun mundosDePrueba(): List<Mundo> = listOf(reinoDeAethel(), torreInfinita())

private fun reinoDeAethel() = Mundo(
    id = nuevoId(),
    nombre = "Reino de Aethel (ejemplo)",
    descripcion = "Hace diez años se abrieron las Puertas. Desde entonces, los cazadores despertados protegen el reino de los monstruos de las mazmorras.",
    semillaMapa = 42, // Mapa generado por la app
    facciones = listOf(
        Faccion("cuervo", "Gremio Cuervo", "Cazadores mercenarios del norte. Aceptan cualquier encargo si el precio es justo."),
        Faccion("alba", "Orden del Alba", "Sacerdotes guerreros que sirven a la familia real."),
        Faccion("carmesi", "Culto Carmesí", "Adoradores del Rey Demonio. Quieren abrir todas las Puertas a la vez."),
    ),
    lugares = listOf(
        Lugar("eastaria", "Eastaria", TipoLugar.CONTINENTE, "El continente principal."),
        Lugar("aethel", "Reino de Aethel", TipoLugar.REINO, "Reino humano del oeste.", padreId = "eastaria", x = 0.30f, y = 0.48f),
        Lugar("seul", "Seúl Nueva", TipoLugar.CIUDAD, "Capital del reino, construida sobre las ruinas de la antigua ciudad.", padreId = "aethel", x = 0.45f, y = 0.55f),
        Lugar("musgo", "Villa Musgo", TipoLugar.PUEBLO, "Pueblo tranquilo junto al bosque.", padreId = "aethel", x = 0.22f, y = 0.40f),
        Lugar("puerta", "Puerta Roja", TipoLugar.MAZMORRA, "Mazmorra de rango B que nunca se cierra.", padreId = "seul", x = 0.40f, y = 0.30f),
        // Niveles 4 y 5 del árbol: lugares dentro de la Puerta Roja
        Lugar("cripta", "Cripta Olvidada", TipoLugar.MAZMORRA, "Primer piso de la Puerta Roja, lleno de no muertos.", padreId = "puerta"),
        Lugar("sala", "Sala del Rey Esqueleto", TipoLugar.OTRO, "Donde espera el jefe final de la mazmorra.", padreId = "cripta"),
        Lugar("valtor", "Reino de Valtor", TipoLugar.REINO, "Reino de herreros en las montañas del este.", padreId = "eastaria", x = 0.80f, y = 0.45f),
        Lugar("ceniza", "Fortaleza Ceniza", TipoLugar.CIUDAD, "Ciudad amurallada que vigila la frontera.", padreId = "valtor", x = 0.82f, y = 0.62f),
        Lugar("abismo", "Torre del Abismo", TipoLugar.MAZMORRA, "Nadie ha llegado a su último piso.", padreId = "valtor", x = 0.75f, y = 0.30f),
        Lugar("islas", "Islas Flotantes", TipoLugar.OTRO, "Islas que flotan en el cielo. Nadie sabe cómo llegar."),
    ),
    personajes = listOf(
        Personaje("jin", "Kang Jin-woo", 24, Rango.S, listOf("Sombras", "Teletransporte"), "cuervo", "seul",
            "El cazador más débil del reino hasta que despertó en la Puerta Roja."),
        Personaje("haeun", "Seo Ha-eun", 22, Rango.A, listOf("Luz sagrada", "Curación"), "alba", "seul",
            "Sacerdotisa prodigio. Esconde que puede ver el futuro."),
        Personaje("rhen", "Rhen", 30, Rango.B, listOf("Espada de viento"), null, "musgo",
            "Espadachín sin gremio. Busca al monstruo que destruyó su aldea."),
        Personaje("mina", "Kang Mina", 17, Rango.C, listOf("Barrera"), "cuervo", "seul",
            "Hermana pequeña de Jin-woo. Quiere ser cazadora como él."),
        Personaje("kael", "Kael", null, Rango.A, listOf("Fuego negro", "Invocación"), "carmesi", "abismo",
            "Sumo sacerdote del Culto Carmesí. Nadie conoce su edad."),
        Personaje("tobi", "Tobi", 15, Rango.D, listOf("Sigilo"), null, "musgo",
            "Ladronzuelo que se cuela en las mazmorras para vender lo que encuentra."),
        Personaje("bo", "Aldeano Bo", 60, Rango.E, emptyList(), null, "musgo",
            "Siempre sabe algún rumor."),
    ),
    relaciones = listOf(
        Relacion("r1", "jin", "rhen", TipoRelacion.RIVALES, "Se enfrentaron en la Puerta Roja."),
        Relacion("r2", "jin", "mina", TipoRelacion.FAMILIA, "Hermanos."),
        Relacion("r3", "haeun", "mina", TipoRelacion.MAESTRO_ALUMNO, "Le enseña a controlar su barrera."),
        Relacion("r4", "jin", "kael", TipoRelacion.ENEMIGOS),
        Relacion("r5", "rhen", "tobi", TipoRelacion.ALIADOS, "Tobi le guía por las mazmorras."),
        Relacion("r6", "jin", "haeun", TipoRelacion.AMIGOS),
    ),
)

private fun torreInfinita() = Mundo(
    id = nuevoId(),
    nombre = "Torre Infinita (ejemplo)",
    descripcion = "Cien pisos. Quien llegue a la cima podrá pedir un deseo al dios que vive allí.",
    facciones = listOf(
        Faccion("escaladores", "Escaladores", "Los que intentan llegar a la cima."),
        Faccion("guardianes", "Guardianes", "Protegen cada piso de la torre."),
    ),
    lugares = listOf(
        Lugar("torre", "La Torre", TipoLugar.OTRO, "Una torre que atraviesa las nubes."),
        Lugar("piso1", "Piso 1: Pradera", TipoLugar.MAZMORRA, padreId = "torre"),
        Lugar("piso50", "Piso 50: Ciudad de Cristal", TipoLugar.CIUDAD, padreId = "torre"),
    ),
    personajes = listOf(
        Personaje("yuna", "Yuna", 19, Rango.B, listOf("Arco de hielo"), "escaladores", "piso1"),
        Personaje("guardian", "Guardián del piso 50", null, Rango.S, listOf("Cristalización"), "guardianes", "piso50"),
    ),
    relaciones = listOf(
        Relacion("r1", "yuna", "guardian", TipoRelacion.ENEMIGOS, "Yuna quiere superar su piso."),
    ),
)
