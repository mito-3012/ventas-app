package com.example.ventasapp.model.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.ventasapp.model.AccionRevision
import com.example.ventasapp.model.EstadoVenta
import com.example.ventasapp.model.Perfil
import com.example.ventasapp.model.TipoConvenio
import com.example.ventasapp.model.data.local.ComisionDao
import com.example.ventasapp.model.data.local.ComisionEntity
import com.example.ventasapp.model.data.local.HistorialDao
import com.example.ventasapp.model.data.local.NotificacionDao
import com.example.ventasapp.model.data.local.UsuarioDao
import com.example.ventasapp.model.data.local.UsuarioEntity
import com.example.ventasapp.model.data.local.VentaDao
import com.example.ventasapp.model.data.local.VentaEntity
import com.example.ventasapp.model.data.local.VentasDatabase
import com.example.ventasapp.util.CodigoDuplicadoException
import com.example.ventasapp.util.CorreoDuplicadoException
import com.example.ventasapp.util.ImagenUtils
import com.example.ventasapp.util.ReglaNegocioException
import com.example.ventasapp.util.Seguridad
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf

class VentasRepositoryTest : StringSpec({

    lateinit var db: VentasDatabase
    lateinit var usuarioDao: UsuarioDao
    lateinit var ventaDao: VentaDao
    lateinit var historialDao: HistorialDao
    lateinit var notificacionDao: NotificacionDao
    lateinit var comisionDao: ComisionDao
    lateinit var sesion: SesionManager
    lateinit var contexto: Context
    lateinit var repository: VentasRepository

    val vendedor = UsuarioEntity(
        id = 1L,
        nombre = "Camila Rojas",
        correo = "vendedor1@demo.cl",
        claveHash = "hash",
        sal = "sal",
        perfil = Perfil.VENDEDOR.name,
        zona = "Centro",
        activo = true
    )

    val encargado = UsuarioEntity(
        id = 2L,
        nombre = "Valentina Soto",
        correo = "encargado@demo.cl",
        claveHash = "hash",
        sal = "sal",
        perfil = Perfil.ENCARGADO.name,
        zona = "Matriz",
        activo = true
    )

    val admin = UsuarioEntity(
        id = 3L,
        nombre = "Isidora Campos",
        correo = "admin@demo.cl",
        claveHash = "hash",
        sal = "sal",
        perfil = Perfil.ADMINISTRADOR.name,
        zona = "Matriz",
        activo = true
    )

    fun ventaBase(
        id: Long = 10L,
        vendedorId: Long = 1L,
        estado: EstadoVenta = EstadoVenta.BORRADOR,
        respaldoPath: String? = "/ruta/foto.jpg",
        monto: Int = 100_000,
        codigo: String = "AUTH123"
    ) = VentaEntity(
        id = id,
        vendedorId = vendedorId,
        vendedorNombre = "Camila Rojas",
        tipoConvenio = TipoConvenio.DENTAL.name,
        prestador = "Clínica Dental",
        fechaOperacion = 1700000000000L,
        monto = monto,
        codigoAutorizacion = codigo,
        clienteNombre = "Cliente Test",
        clienteRut = "11.111.111-1",
        clienteCorreo = "cliente@test.cl",
        clienteTelefono = "912345678",
        mascotaNombre = "",
        respaldoPath = respaldoPath,
        estado = estado.name
    )

    beforeTest {
        db = mockk(relaxed = true)
        usuarioDao = mockk(relaxed = true)
        ventaDao = mockk(relaxed = true)
        historialDao = mockk(relaxed = true)
        notificacionDao = mockk(relaxed = true)
        comisionDao = mockk(relaxed = true)
        sesion = mockk(relaxed = true)
        contexto = mockk(relaxed = true)

        every { db.usuarioDao() } returns usuarioDao
        every { db.ventaDao() } returns ventaDao
        every { db.historialDao() } returns historialDao
        every { db.notificacionDao() } returns notificacionDao
        every { db.comisionDao() } returns comisionDao

        // Simula ejecución directa del bloque dentro de db.withTransaction { ... }
        mockkStatic("androidx.room.RoomDatabaseKt")
        val txSlot = slot<suspend () -> Any?>()
        coEvery { db.withTransaction(capture(txSlot)) } coAnswers {
            txSlot.captured.invoke()
        }

        mockkObject(Seguridad)
        mockkObject(ImagenUtils)

        repository = VentasRepository(db, sesion, contexto)
    }

    afterTest {
        unmockkAll()
    }

    // -------------------------------------------------------------------------
    // AUTENTICACIÓN Y REGISTRO
    // -------------------------------------------------------------------------

    "iniciarSesion debe retornar el usuario cuando credenciales son válidas y está activo" {
        coEvery { usuarioDao.buscarPorCorreo("vendedor1@demo.cl") } returns vendedor
        every { Seguridad.verificar("Ventas123", "sal", "hash") } returns true

        val resultado = repository.iniciarSesion("  VENDEDOR1@demo.cl ", "Ventas123")

        resultado shouldBe vendedor
    }

    "iniciarSesion debe lanzar ReglaNegocioException si usuario no existe o clave falla" {
        coEvery { usuarioDao.buscarPorCorreo("noexiste@demo.cl") } returns null

        shouldThrow<ReglaNegocioException> {
            repository.iniciarSesion("noexiste@demo.cl", "1234")
        }

        coEvery { usuarioDao.buscarPorCorreo("vendedor1@demo.cl") } returns vendedor
        every { Seguridad.verificar("mala", "sal", "hash") } returns false

        shouldThrow<ReglaNegocioException> {
            repository.iniciarSesion("vendedor1@demo.cl", "mala")
        }
    }

    "iniciarSesion debe lanzar ReglaNegocioException si la cuenta está desactivada" {
        val inactivo = vendedor.copy(activo = false)
        coEvery { usuarioDao.buscarPorCorreo("vendedor1@demo.cl") } returns inactivo
        every { Seguridad.verificar("Ventas123", "sal", "hash") } returns true

        shouldThrow<ReglaNegocioException> {
            repository.iniciarSesion("vendedor1@demo.cl", "Ventas123")
        }
    }

    "registrarVendedor debe insertar nuevo vendedor si el correo no está duplicado" {
        coEvery { usuarioDao.buscarPorCorreo("nuevo@demo.cl") } returns null
        every { Seguridad.generarSal() } returns "salNueva"
        every { Seguridad.hashClave("Clave123", "salNueva") } returns "hashNuevo"
        coEvery { usuarioDao.insertar(any()) } returns 99L

        val id = repository.registrarVendedor("Juan", "nuevo@demo.cl", "Clave123", "Sur")

        id shouldBe 99L
        coVerify { usuarioDao.insertar(match { it.perfil == Perfil.VENDEDOR.name && it.correo == "nuevo@demo.cl" }) }
    }

    "registrarVendedor debe lanzar CorreoDuplicadoException si el correo ya existe" {
        coEvery { usuarioDao.buscarPorCorreo("vendedor1@demo.cl") } returns vendedor

        shouldThrow<CorreoDuplicadoException> {
            repository.registrarVendedor("Camila", "vendedor1@demo.cl", "1234", "Centro")
        }
    }

    // -------------------------------------------------------------------------
    // CONSULTAS Y PERMISOS DE VISUALIZACIÓN
    // -------------------------------------------------------------------------

    "observarVentaAutorizada debe filtrar según perfil del usuario" {
        val borrador = ventaBase(id = 10L, vendedorId = 1L, estado = EstadoVenta.BORRADOR)
        val pendiente = ventaBase(id = 11L, vendedorId = 1L, estado = EstadoVenta.PENDIENTE_REVISION)

        every { ventaDao.observarPorId(10L) } returns flowOf(borrador)
        every { ventaDao.observarPorId(11L) } returns flowOf(pendiente)

        // Vendedor dueño sí ve su borrador
        repository.observarVentaAutorizada(10L, vendedor).first() shouldBe borrador
        // Encargado NO ve borradores, pero SÍ pendientes
        repository.observarVentaAutorizada(10L, encargado).first() shouldBe null
        repository.observarVentaAutorizada(11L, encargado).first() shouldBe pendiente
        // Admin NO ve ventas
        repository.observarVentaAutorizada(11L, admin).first() shouldBe null
    }

    "comisiones debe combinar porcentajes guardados con los valores por defecto" {
        every { comisionDao.observarTodas() } returns flowOf(
            listOf(ComisionEntity(TipoConvenio.DENTAL.name, 0.25))
        )

        val mapa = repository.comisiones().first()

        mapa[TipoConvenio.DENTAL] shouldBe 0.25
        mapa[TipoConvenio.VETERINARIO] shouldBe TipoConvenio.VETERINARIO.porcentajeComision
    }

    // -------------------------------------------------------------------------
    // COMANDOS DEL VENDEDOR (GUARDAR, ENVIAR, ELIMINAR)
    // -------------------------------------------------------------------------

    "guardarCambios debe crear borrador y registrar historial cuando id es 0" {
        val nueva = ventaBase(id = 0L)
        coEvery { ventaDao.contarCodigo("AUTH123", 0L) } returns 0
        coEvery { ventaDao.insertar(any()) } returns 50L

        val idCreado = repository.guardarCambios(nueva, vendedor)

        idCreado shouldBe 50L
        coVerify { ventaDao.insertar(match { it.estado == EstadoVenta.BORRADOR.name && it.vendedorId == 1L }) }
        coVerify { historialDao.insertar(match { it.ventaId == 50L && it.estadoNuevo == EstadoVenta.BORRADOR.name }) }
    }

    "guardarCambios debe lanzar CodigoDuplicadoException si el código ya existe" {
        val nueva = ventaBase(id = 0L, codigo = "DUP999")
        coEvery { ventaDao.contarCodigo("DUP999", 0L) } returns 1

        shouldThrow<CodigoDuplicadoException> {
            repository.guardarCambios(nueva, vendedor)
        }
    }

    "enviarVenta debe validar respaldo y monto, cambiar estado a PENDIENTE_REVISION y notificar" {
        val borrador = ventaBase(id = 10L, estado = EstadoVenta.BORRADOR, respaldoPath = "/foto.jpg", monto = 50_000)
        coEvery { ventaDao.contarCodigo(any(), any()) } returns 0
        coEvery { ventaDao.obtenerPorId(10L) } returns borrador

        val id = repository.enviarVenta(borrador, vendedor)

        id shouldBe 10L
        coVerify { ventaDao.actualizar(match { it.estado == EstadoVenta.PENDIENTE_REVISION.name }) }
        coVerify { notificacionDao.insertar(match { it.perfilDestino == Perfil.ENCARGADO.name }) }
    }

    "enviarVenta debe pasar a CORREGIDA si la venta estaba OBSERVADA" {
        val observada = ventaBase(id = 10L, estado = EstadoVenta.OBSERVADA, respaldoPath = "/foto.jpg", monto = 50_000)
        coEvery { ventaDao.contarCodigo(any(), any()) } returns 0
        coEvery { ventaDao.obtenerPorId(10L) } returns observada

        repository.enviarVenta(observada, vendedor)

        coVerify { ventaDao.actualizar(match { it.estado == EstadoVenta.CORREGIDA.name }) }
    }

    "enviarVenta debe fallar si falta respaldo o el monto es <= 0" {
        val sinFoto = ventaBase(respaldoPath = null)
        shouldThrow<ReglaNegocioException> {
            repository.enviarVenta(sinFoto, vendedor)
        }

        val montoCero = ventaBase(monto = 0)
        shouldThrow<ReglaNegocioException> {
            repository.enviarVenta(montoCero, vendedor)
        }
    }

    "eliminarVenta debe borrar venta, historial y foto de respaldo cuando es borrador del vendedor" {
        val borrador = ventaBase(id = 10L, vendedorId = 1L, estado = EstadoVenta.BORRADOR, respaldoPath = "/foto.jpg")
        coEvery { ventaDao.obtenerPorId(10L) } returns borrador
        every { ImagenUtils.eliminarRespaldo(contexto, "/foto.jpg") } returns true

        repository.eliminarVenta(10L, vendedor)

        coVerify { ventaDao.eliminar(10L) }
        coVerify { historialDao.eliminarDeVenta(10L) }
        coVerify { ImagenUtils.eliminarRespaldo(contexto, "/foto.jpg") }
    }

    // -------------------------------------------------------------------------
    // COMANDOS DEL ENCARGADO Y ADMINISTRADOR
    // -------------------------------------------------------------------------

    "revisar debe validar venta pendiente, guardar historial y notificar al vendedor" {
        val pendiente = ventaBase(id = 10L, vendedorId = 1L, estado = EstadoVenta.PENDIENTE_REVISION)
        coEvery { ventaDao.obtenerPorId(10L) } returns pendiente

        repository.revisar(10L, AccionRevision.VALIDAR, "", encargado)

        coVerify { ventaDao.actualizar(match { it.estado == EstadoVenta.VALIDADA.name && it.revisorId == encargado.id }) }
        coVerify { notificacionDao.insertar(match { it.usuarioDestinoId == 1L }) }
    }

    "revisar debe exigir observación cuando se rechaza o solicita corrección" {
        shouldThrow<ReglaNegocioException> {
            repository.revisar(10L, AccionRevision.RECHAZAR, "   ", encargado)
        }
    }

    "marcarGestionada debe derivar una venta VALIDADA y registrar referencia externa" {
        val validada = ventaBase(id = 10L, estado = EstadoVenta.VALIDADA)
        coEvery { ventaDao.obtenerPorId(10L) } returns validada

        repository.marcarGestionada(10L, encargado, "REF-API-99")

        coVerify { ventaDao.actualizar(match { it.estado == EstadoVenta.GESTIONADA.name }) }
        coVerify { historialDao.insertar(match { it.observacion == "REF-API-99" }) }
    }

    "actualizarComision debe guardar porcentaje válido y rechazar fuera de rango" {
        repository.actualizarComision(TipoConvenio.DENTAL, 0.15, admin)
        coVerify { comisionDao.guardar(ComisionEntity(TipoConvenio.DENTAL.name, 0.15)) }

        shouldThrow<ReglaNegocioException> {
            repository.actualizarComision(TipoConvenio.DENTAL, 0.50, admin)
        }
    }

    "cambiarEstadoUsuario no debe permitir que el admin se desactive a sí mismo" {
        shouldThrow<ReglaNegocioException> {
            repository.cambiarEstadoUsuario(admin.id, false, admin)
        }
    }
})
