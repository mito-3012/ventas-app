package com.example.ventasapp.model

// Almacena los mensajes de validación asociados a cada campo del formulario.
data class PostFormErrores(
    val titulo: String? = null,
    val cuerpo: String? = null
)
