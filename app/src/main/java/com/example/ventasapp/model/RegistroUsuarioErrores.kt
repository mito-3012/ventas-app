package com.example.ventasapp.model

data class RegistroUsuarioErrores(
    val nombre: String? = null,
    val correo: String? = null,
    val clave: String? = null,
    val confirmarClave: String? = null,
    val zona: String? = null,
    val terminos: String? = null
)
