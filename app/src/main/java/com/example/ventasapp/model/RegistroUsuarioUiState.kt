package com.example.ventasapp.model

/** Estado del formulario de creación de cuenta ficticia de vendedor (extiende la guía 11). */
data class RegistroUsuarioUiState(
    val nombre: String = "",
    val correo: String = "",
    val clave: String = "",
    val confirmarClave: String = "",
    val zona: String = "",
    val aceptaTerminos: Boolean = false,
    val mostrarClave: Boolean = false,
    val cargando: Boolean = false,
    val errores: RegistroUsuarioErrores = RegistroUsuarioErrores()
)
