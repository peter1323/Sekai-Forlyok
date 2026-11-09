package com.sekaiforlyok

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sekaiforlyok.ui.App

/** Arranque de la app en Android. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        iniciarAndroid(this)  // Prepara la carpeta de datos y el selector de archivos
        enableEdgeToEdge()
        setContent { App() }
    }
}
