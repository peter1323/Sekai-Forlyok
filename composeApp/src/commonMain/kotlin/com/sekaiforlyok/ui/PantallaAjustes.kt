package com.sekaiforlyok.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.sekaiforlyok.model.Idioma
import com.sekaiforlyok.model.Tema
import com.sekaiforlyok.viewmodel.AjustesViewModel

// Datos de la sección "Acerca de"
const val VERSION_APP = "1.0.0"
const val AUTOR_APP = "peter1323"
const val URL_RELEASES = "https://github.com/peter1323/Sekai-Forlyok/releases"

/** Pantalla de ajustes: tema, idioma y acerca de. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAjustes(vm: AjustesViewModel, alVolver: () -> Unit) {
    val t = LocalTextos.current
    val ajustes by vm.ajustes.collectAsState()
    val abrirEnlace = LocalUriHandler.current  // Abre el navegador (en Android y en Windows)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t.ajustes) },
                navigationIcon = {
                    IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, t.volver) }
                },
            )
        },
    ) { margen ->
        Column(
            Modifier.padding(margen).padding(16.dp).widthIn(max = 600.dp).verticalScroll(rememberScrollState()),
        ) {
            Desplegable(
                t.tema, Tema.entries, ajustes.tema,
                {
                    when (it) {
                        Tema.SISTEMA -> t.temaSistema
                        Tema.CLARO -> t.temaClaro
                        Tema.OSCURO -> t.temaOscuro
                    }
                },
                vm::cambiarTema,
            )
            // Los nombres de idioma se escriben siempre en su propio idioma
            Desplegable(
                t.idioma, Idioma.entries, ajustes.idioma,
                { if (it == Idioma.ES) "Español" else "English" },
                vm::cambiarIdioma,
            )

            Spacer(Modifier.height(24.dp))
            Text(t.acercaDe, style = MaterialTheme.typography.titleMedium)
            Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Sekai Forlyok", style = MaterialTheme.typography.titleLarge)
                    Text("${t.version} $VERSION_APP")
                    Text("${t.autor}: $AUTOR_APP")
                    Text(t.plataformas, Modifier.padding(top = 8.dp))
                    OutlinedButton(onClick = { abrirEnlace.openUri(URL_RELEASES) }, modifier = Modifier.padding(top = 12.dp)) {
                        Text(t.descargas)
                    }
                }
            }
        }
    }
}
