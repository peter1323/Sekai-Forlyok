import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    // Destino Android (como librería; la app la arranca el módulo androidApp)
    android {
        namespace = "com.sekaiforlyok.shared"
        compileSdk = 37
        minSdk = 24
    }

    // Destino escritorio (Windows)
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)
            implementation(libs.compose.icons)
            implementation(libs.lifecycle.viewmodel)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.io)
            implementation(libs.filekit.dialogs)
        }
        androidMain.dependencies {
            // Para recibir la Activity en iniciarAndroid()
            implementation(libs.androidx.activity.compose)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                // Hilo principal de la interfaz en escritorio (lo usan los ViewModel)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
    }
}

// Configuración de la app de escritorio y del instalador .msi
compose.desktop {
    application {
        mainClass = "com.sekaiforlyok.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            // Módulos de Java que el empaquetado no detecta solo. Sin jdk.accessibility la app
            // no arranca en Windows con la accesibilidad de Java activada ("Failed to launch JVM").
            modules("jdk.accessibility", "jdk.unsupported")
            packageName = "SekaiForlyok"
            packageVersion = "1.0.0"
            description = "Creador de personajes y mundos al estilo manhwa"
            windows {
                menu = true
                shortcut = true
                dirChooser = true
                // Identificador fijo para que las versiones nuevas actualicen la instalada
                upgradeUuid = "5b8e3c2a-7f41-4d6e-9a0b-3c1d2e4f5a6b"
            }
        }
    }
}
