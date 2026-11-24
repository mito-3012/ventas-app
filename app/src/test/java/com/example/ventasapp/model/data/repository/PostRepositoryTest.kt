package com.example.ventasapp.model.data.repository

import com.example.ventasapp.model.Post
import com.example.ventasapp.model.PostNuevo
import com.example.ventasapp.model.PostParcial
import com.example.ventasapp.model.data.remote.ApiService
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import retrofit2.Response

class PostRepositoryTest : StringSpec({

    lateinit var api: ApiService
    lateinit var repository: PostRepository

    beforeTest {
        api = mockk()
        repository = PostRepository(api)
    }

    "obtenerPosts debe retornar la lista obtenida desde ApiService" {
        val lista = listOf(Post(1, 1, "Título 1", "Cuerpo 1"))
        coEvery { api.getPosts() } returns lista

        repository.obtenerPosts() shouldBe lista
    }

    "obtenerPost debe retornar un post específico por id" {
        val post = Post(1, 5, "Detalle", "Contenido")
        coEvery { api.getPost(5) } returns post

        repository.obtenerPost(5) shouldBe post
    }

    "crearPost debe enviar PostNuevo y retornar el Post creado" {
        val nuevo = PostNuevo(1, "Nuevo", "Texto")
        val creado = Post(1, 101, "Nuevo", "Texto")
        coEvery { api.crearPost(nuevo) } returns creado

        repository.crearPost(nuevo) shouldBe creado
    }

    "reemplazarPost debe invocar PUT con el id del post" {
        val actualizado = Post(1, 10, "Editado", "Nuevo cuerpo")
        coEvery { api.reemplazarPost(10, actualizado) } returns actualizado

        repository.reemplazarPost(actualizado) shouldBe actualizado
    }

    "actualizarTitulo debe invocar PATCH con PostParcial" {
        val parcheado = Post(1, 10, "Solo título", "Cuerpo")
        coEvery { api.actualizarParcial(10, PostParcial(title = "Solo título")) } returns parcheado

        repository.actualizarTitulo(10, "Solo título") shouldBe parcheado
    }

    "eliminarPost debe retornar true cuando Response.isSuccessful es verdadero" {
        coEvery { api.eliminarPost(10) } returns Response.success(Unit)

        repository.eliminarPost(10) shouldBe true
        coVerify { api.eliminarPost(10) }
    }
})
