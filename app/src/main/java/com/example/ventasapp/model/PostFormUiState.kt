package com.example.ventasapp.model

// Conserva los valores del formulario y permite que Compose refleje sus cambios.
data class PostFormUiState(
    val visible: Boolean = false,
    val modo: ModoFormularioPost = ModoFormularioPost.CREAR,
    val postId: Int? = null,
    val userId: Int = 1,
    val titulo: String = "",
    val cuerpo: String = "",
    val guardando: Boolean = false,
    val errores: PostFormErrores = PostFormErrores()
)
