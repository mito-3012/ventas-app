package com.example.ventasapp.model

/** Modelo de respuesta de JSONPlaceholder. */
// Representa una publicación recibida desde el servidor, incluidos sus identificadores.
data class Post(
    val userId: Int,
    val id: Int,
    val title: String,
    val body: String
)

/** Modelo de creación: el servidor asigna el identificador. */
// Contiene únicamente los datos necesarios para crear una publicación; el servidor asigna el ID.
data class PostNuevo(
    val userId: Int,
    val title: String,
    val body: String
)

/** Modelo para PATCH: solo se envían los campos que se desean modificar. */
// Permite enviar solo los campos que se desean modificar mediante una solicitud PATCH.
data class PostParcial(
    val title: String? = null,
    val body: String? = null
)
