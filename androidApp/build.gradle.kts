plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "com.sekaiforlyok"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.sekaiforlyok"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Se firma con la clave de depuración para poder instalar el .apk directamente.
            // Para publicar en Google Play hay que crear una clave propia (ver README).
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.androidx.activity.compose)
}
