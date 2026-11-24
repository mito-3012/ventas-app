package com.example.ventasapp.viewmodel

import android.app.Application
import com.example.ventasapp.model.Perfil
import com.example.ventasapp.model.data.local.UsuarioEntity
import com.example.ventasapp.model.data.repository.SesionManager
import com.example.ventasapp.model.data.repository.VentasRepository
import com.example.ventasapp.repositorio
import com.example.ventasapp.util.ReglaNegocioException
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest : StringSpec({

    val testDispatcher = StandardTestDispatcher()

    lateinit var app: Application
    lateinit var repo: VentasRepository
    lateinit var sesionManager: SesionManager
    lateinit var usuarioFlow: MutableStateFlow<UsuarioEntity?>
    lateinit var viewModel: LoginViewModel

    val usuarioDemo = UsuarioEntity(
        id = 1L,
        nombre = "Camila Rojas",
        correo = "vendedor1@demo.cl",
        claveHash = "hash",
        sal = "sal",
        perfil = Perfil.VENDEDOR.name,
        zona = "Centro",
        activo = true
    )

    beforeTest {
        Dispatchers.setMain(testDispatcher)

        app = mockk(relaxed = true)
        repo = mockk(relaxed = true)
        sesionManager = mockk(relaxed = true)
        usuarioFlow = MutableStateFlow(null)

        every { repo.sesion } returns sesionManager
        every { sesionManager.usuario } returns usuarioFlow

        // Mockea la propiedad de extensión AndroidViewModel.repositorio
        mockkStatic("com.example.ventasapp.VentasApplicationKt")
        every { any<androidx.lifecycle.AndroidViewModel>().repositorio } returns repo

        viewModel = LoginViewModel(app)
    }

    afterTest {
        Dispatchers.resetMain()
        unmockkAll()
    }

    "init debe sembrar datos demo y eliminar respaldos huérfanos" {
        runTest {
            advanceUntilIdle()
            coVerify { repo.sembrarDatosDemoSiCorresponde() }
            coVerify { repo.eliminarRespaldosHuerfanos() }
        }
    }

    "onCorreoChange y onClaveChange deben actualizar el estado respetando límites y limpiando errores" {
        viewModel.onCorreoChange("vendedor1@demo.cl")
        viewModel.onClaveChange("Ventas123")

        viewModel.login.value.correo shouldBe "vendedor1@demo.cl"
        viewModel.login.value.clave shouldBe "Ventas123"
        viewModel.login.value.errores.correo shouldBe null
        viewModel.login.value.errores.clave shouldBe null
    }

    "alternarMostrarClave debe invertir el valor booleano de mostrarClave" {
        val inicial = viewModel.login.value.mostrarClave
        viewModel.alternarMostrarClave()
        viewModel.login.value.mostrarClave shouldBe !inicial
    }

    "autocompletar debe cargar correo y clave limpiando errores previos" {
        viewModel.autocompletar("admin@demo.cl", "Admin12345")

        viewModel.login.value.correo shouldBe "admin@demo.cl"
        viewModel.login.value.clave shouldBe "Admin12345"
        viewModel.login.value.errores.correo shouldBe null
        viewModel.login.value.errores.clave shouldBe null
    }

    "iniciarSesion con campos vacíos debe marcar errores de validación sin llamar al repositorio" {
        runTest {
            var exitoLlamado = false
            viewModel.iniciarSesion { exitoLlamado = true }
            advanceUntilIdle()

            exitoLlamado shouldBe false
            viewModel.login.value.errores.correo.shouldNotBeNull()
            viewModel.login.value.errores.clave.shouldNotBeNull()
            coVerify(exactly = 0) { repo.iniciarSesion(any(), any()) }
        }
    }

    "iniciarSesion con credenciales válidas debe iniciar sesión en SesionManager, limpiar estado y ejecutar onExito" {
        runTest {
            coEvery { repo.iniciarSesion("vendedor1@demo.cl", "Ventas123") } returns usuarioDemo
            viewModel.autocompletar("vendedor1@demo.cl", "Ventas123")

            var perfilDevuelto: Perfil? = null
            viewModel.iniciarSesion { perfil -> perfilDevuelto = perfil }
            advanceUntilIdle()

            perfilDevuelto shouldBe Perfil.VENDEDOR
            verify { sesionManager.iniciar(usuarioDemo) }
            // Verifica que se limpió la clave de memoria tras iniciar sesión
            viewModel.login.value.clave shouldBe ""
            viewModel.login.value.cargando shouldBe false
        }
    }

    "iniciarSesion cuando falla con ReglaNegocioException debe mostrar el mensaje de error general" {
        runTest {
            coEvery { repo.iniciarSesion(any(), any()) } throws ReglaNegocioException("Correo o contraseña incorrectos.")
            viewModel.autocompletar("vendedor1@demo.cl", "ClaveMala")

            var exitoLlamado = false
            viewModel.iniciarSesion { exitoLlamado = true }
            advanceUntilIdle()

            exitoLlamado shouldBe false
            viewModel.login.value.cargando shouldBe false
            viewModel.login.value.errores.general shouldBe "Correo o contraseña incorrectos."
        }
    }

    "cerrarSesion debe cerrar sesión en SesionManager y reiniciar LoginUiState" {
        viewModel.autocompletar("vendedor1@demo.cl", "Ventas123")
        viewModel.cerrarSesion()

        verify { sesionManager.cerrar() }
        viewModel.login.value.correo shouldBe ""
        viewModel.login.value.clave shouldBe ""
    }
})