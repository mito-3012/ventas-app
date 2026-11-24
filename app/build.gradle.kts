import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.ventasapp"

    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.ventasapp"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

dependencies {

    // JETPACK COMPOSE
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)

    // LIFECYCLE Y VIEWMODEL
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // API REST - RETROFIT
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)

    // HTTP - OKHTTP
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // CORRUTINAS
    implementation(libs.kotlinx.coroutines.android)

    // NAVEGACIÓN
    implementation(libs.androidx.navigation.compose)

    // ROOM - BASE DE DATOS LOCAL
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // PRUEBAS UNITARIAS - JUNIT 5
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.0")

    // JUNIT 4
    testImplementation(libs.junit)

    // KOTEST
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")

    // MOCKK
    testImplementation("io.mockk:mockk:1.13.10")

    // CORRUTINAS PARA PRUEBAS
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")

    // PRUEBAS DE INTERFAZ
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// CONFIGURACIÓN DE JUNIT 5
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}