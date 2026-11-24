// Archivo de build de nivel proyecto: solo declara los plugins (apply false)
// para que cada módulo los aplique con la misma versión.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false   // KSP: genera el código que necesita Room
}
