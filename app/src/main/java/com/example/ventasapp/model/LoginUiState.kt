package com.example.ventasapp.model

/** Estado completo de la pantalla de inicio de sesión (mismo patrón de la guía 11). */
data class LoginUiState(
    val correo: String = "",
    val clave: String = "",
    val mostrarClave: Boolean = false,
    val cargando: Boolean = false,
    val errores: LoginErrores = LoginErrores()
)
