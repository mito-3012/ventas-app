package com.example.ventasapp.model


/** Estado del formulario "Nuevo usuario" que usa el administrador (mismo patrón de la guía 11). */
data class NuevoUsuarioUiState(
    /** true = el diálogo del formulario está abierto. */
    val visible: Boolean = false,
    val nombre: String = "",
    val correo: String = "",
    val zona: String = "",
    val clave: String = "",
    val perfil: Perfil = Perfil.VENDEDOR,
    val mostrarClave: Boolean = false,
    val cargando: Boolean = false,
    val errores: NuevoUsuarioErrores = NuevoUsuarioErrores()
)
