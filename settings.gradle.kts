rootProject.name = "SekaiForlyok"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

// composeApp: todo el código compartido + la app de escritorio
// androidApp: solo la Activity que arranca la app en Android
include(":composeApp", ":androidApp")
