package com.example.ventasapp.model


// Estado observable de la pantalla: publicaciones, carga, errores y operación en curso.
data class PostUiState(
    val posts: List<Post> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null,
    val cargaIntentada: Boolean = false,
    val procesandoId: Int? = null
)
