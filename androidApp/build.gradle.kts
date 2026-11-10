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
        versionCode = 2
        versionName = "1.1.0"
    }

    // Un APK por tipo de procesador del móvil, y además uno universal que vale para todos
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
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
