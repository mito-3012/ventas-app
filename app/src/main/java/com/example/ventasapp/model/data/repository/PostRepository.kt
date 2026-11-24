package com.example.ventasapp.model.data.repository

import com.example.ventasapp.model.Post
import com.example.ventasapp.model.PostNuevo
import com.example.ventasapp.model.PostParcial
import com.example.ventasapp.model.data.remote.ApiService

/** Capa repository: la UI no conoce Retrofit ni las rutas HTTP. */
class PostRepository(private val api: ApiService) {
    // Recupera la colección completa de publicaciones disponible en la API.
    suspend fun obtenerPosts(): List<Post> = api.getPosts()
    // Consulta una publicación específica utilizando su identificador.
    suspend fun obtenerPost(id: Int): Post = api.getPost(id)
    // Envía los datos de una nueva publicación mediante el método POST.
    suspend fun crearPost(post: PostNuevo): Post = api.crearPost(post)
    // Sustituye el contenido de una publicación existente mediante PUT.
    suspend fun reemplazarPost(post: Post): Post = api.reemplazarPost(post.id, post)
    // Modifica exclusivamente el título, sin reemplazar el resto de los datos.
    suspend fun actualizarTitulo(id: Int, titulo: String): Post =
        api.actualizarParcial(id, PostParcial(title = titulo))
    // Informa si el servidor respondió satisfactoriamente a la solicitud DELETE.
    suspend fun eliminarPost(id: Int): Boolean = api.eliminarPost(id).isSuccessful
}
