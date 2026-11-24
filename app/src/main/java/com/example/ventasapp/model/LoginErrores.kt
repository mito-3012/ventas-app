package com.example.ventasapp.model

/** Un String? por campo: null significa "campo válido". */
data class LoginErrores(
    val correo: String? = null,
    val clave: String? = null,
    /** Error que no pertenece a un campo puntual (credenciales incorrectas, cuenta desactivada). */
    val general: String? = null
)
