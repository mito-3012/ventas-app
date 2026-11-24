package com.example.ventasapp.model.data.remote

import com.example.ventasapp.model.Post
import com.example.ventasapp.model.PostNuevo
import com.example.ventasapp.model.PostParcial
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** Contrato HTTP de la API pública JSONPlaceholder. */
interface ApiService {
    // GET: obtiene todas las publicaciones disponibles.
    @GET("posts")
    suspend fun getPosts(): List<Post>

    // GET por identificador: consulta una publicación determinada.
    @GET("posts/{id}")
    suspend fun getPost(@Path("id") id: Int): Post

    // POST: envía los datos para crear una publicación nueva.
    @POST("posts")
    suspend fun crearPost(@Body post: PostNuevo): Post

    // PUT: reemplaza el contenido completo de la publicación indicada.
    @PUT("posts/{id}")
    suspend fun reemplazarPost(@Path("id") id: Int, @Body post: Post): Post

    // PATCH: actualiza parcialmente la publicación indicada.
    @PATCH("posts/{id}")
    suspend fun actualizarParcial(@Path("id") id: Int, @Body cambios: PostParcial): Post

    // DELETE: solicita eliminar la publicación indicada.
    @DELETE("posts/{id}")
    suspend fun eliminarPost(@Path("id") id: Int): Response<Unit>
}
