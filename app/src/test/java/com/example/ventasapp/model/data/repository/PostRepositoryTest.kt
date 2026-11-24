package com.example.ventasapp.model.data.repository



import com.example.ventasapp.model.Post
import com.example.ventasapp.model.data.remote.ApiService
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest



class PostRepositoryTest : StringSpec({



    "getPosts debe retornar una lista de publicaciones" {



        val fakePosts = listOf(

            Post(

                userId = 1,

                id = 1,

                title = "Titulo 1",

                body = "Contenido 1"

            ),

            Post(

                userId = 2,

                id = 2,

                title = "Titulo 2",

                body = "Contenido 2"

            )

        )



        val mockApi = mockk<ApiService>()



        coEvery {

            mockApi.getPosts()

        } returns fakePosts



        // Aquí se debe conectar con el constructor real

        // de PostRepository para ejecutar la prueba.

    }

})