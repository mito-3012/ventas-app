package com.example.ventasapp.viewmodel



import com.example.ventasapp.model.Post
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest



@OptIn(ExperimentalCoroutinesApi::class)

class PostViewModelTest : StringSpec({



    "postList debe contener los datos esperados después de fetchPosts" {



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



        runTest {

            // Aquí se debe probar el ViewModel real de ventasapp.

        }

    }

})