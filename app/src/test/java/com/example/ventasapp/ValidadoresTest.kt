package com.example.ventasapp

import com.example.ventasapp.model.EstadoVenta
import com.example.ventasapp.model.FiltrosVentas
import com.example.ventasapp.model.Indicadores
import com.example.ventasapp.model.Perfil
import com.example.ventasapp.model.TipoConvenio
import com.example.ventasapp.model.aplicarFiltros
import com.example.ventasapp.model.data.local.VentaEntity
import com.example.ventasapp.util.Seguridad
import com.example.ventasapp.util.Validadores
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias de la lógica de negocio. Como la validación vive en Kotlin puro (no dentro de
 * los Composables), se prueba sin emulador: basta con ejecutar "Run tests" en Android Studio.
 */
class ValidadoresTest {

    // ------------------------------------------------------------ Correo y clave
    @Test fun correoValidoNoTieneError() = assertNull(Validadores.correo("ana@correo.cl"))
    @Test fun correoSinArrobaTieneError() = assertNotNull(Validadores.correo("ana.correo.cl"))
    @Test fun correoVacioEsObligatorio() = assertEquals("Campo obligatorio", Validadores.correo("  "))

    @Test fun claveCortaTieneError() = assertNotNull(Validadores.clave("ab1"))
    @Test fun claveSinNumerosTieneError() = assertNotNull(Validadores.clave("sololetras"))
    @Test fun claveCorrectaNoTieneError() = assertNull(Validadores.clave("Ventas123"))
    @Test fun clavesDistintasNoCoinciden() = assertNotNull(Validadores.confirmacionClave("Abc123", "Abc124"))

    // ------------------------------------------------------------ RUT
    @Test fun rutValidoConPuntosYGuion() = assertNull(Validadores.rut("11.111.111-1"))
    @Test fun rutValidoSinFormato() = assertNull(Validadores.rut("222222222"))
    @Test fun rutConDigitoVerificadorMalo() = assertNotNull(Validadores.rut("11.111.111-2"))
    @Test fun rutDemasiadoCorto() = assertNotNull(Validadores.rut("1-9"))
    @Test fun rutSeFormateaConPuntos() = assertEquals("11.111.111-1", Validadores.formatearRut("111111111"))
    @Test fun digitoVerificadorConK() = assertEquals('K', Validadores.digitoVerificador("10000013"))

    // ------------------------------------------------------------ Monto, código, teléfono
    @Test fun montoValido() = assertNull(Validadores.monto("150000"))
    @Test fun montoCeroEsInvalido() = assertNotNull(Validadores.monto("0"))
    @Test fun montoConLetrasEsInvalido() = assertNotNull(Validadores.monto("12a"))
    @Test fun montoSobreElMaximoEsInvalido() = assertNotNull(Validadores.monto("10000001"))

    @Test fun codigoValido() = assertNull(Validadores.codigoAutorizacion("AB12CD34"))
    @Test fun codigoConMinusculasEsInvalido() = assertNotNull(Validadores.codigoAutorizacion("ab12cd34"))
    @Test fun codigoCortoEsInvalido() = assertNotNull(Validadores.codigoAutorizacion("AB12"))

    @Test fun telefonoCelularValido() = assertNull(Validadores.telefono("912345678"))
    @Test fun telefonoConPrefijoValido() = assertNull(Validadores.telefono("+56912345678"))
    @Test fun telefonoFijoEsInvalido() = assertNotNull(Validadores.telefono("221234567"))

    // ------------------------------------------------------------ Fechas
    private val unDia = 24L * 60 * 60 * 1000
    private val hoy = 1_800_000_000_000L - (1_800_000_000_000L % unDia)   // medianoche UTC simulada

    @Test fun fechaDeHoyEsValida() = assertNull(Validadores.fechaOperacion(hoy, hoy))
    // Citas a futuro: se permite hasta 1 año hacia adelante.
    @Test fun fechaFuturaCercanaEsValida() = assertNull(Validadores.fechaOperacion(hoy + 30 * unDia, hoy))
    @Test fun fechaFuturaDeUnAnioEsValida() = assertNull(Validadores.fechaOperacion(hoy + 365 * unDia, hoy))
    @Test fun fechaFuturaSobreUnAnioEsInvalida() = assertNotNull(Validadores.fechaOperacion(hoy + 366 * unDia, hoy))
    @Test fun limiteDelCalendarioEsHoyMasUnAnio() = assertEquals(hoy + 365 * unDia, Validadores.fechaLimiteFutura(hoy))
    @Test fun fechaMuyAntiguaEsInvalida() = assertNotNull(Validadores.fechaOperacion(hoy - 61 * unDia, hoy))
    @Test fun fechaNulaEsObligatoria() = assertNotNull(Validadores.fechaOperacion(null, hoy))

    // ------------------------------------------------------------ Validaciones de publicaciones de la API REST
    @Test fun tituloPostVacioEsInvalido() = assertNotNull(Validadores.tituloPublicacion("  "))
    @Test fun tituloPostValidoNoTieneError() = assertNull(Validadores.tituloPublicacion("Aviso importante"))
    @Test fun cuerpoPostDemasiadoCortoEsInvalido() = assertNotNull(Validadores.cuerpoPublicacion("abc"))
    @Test fun cuerpoPostValidoNoTieneError() = assertNull(Validadores.cuerpoPublicacion("Contenido de prueba para la API"))

    // ------------------------------------------------------------ Observación del encargado
    @Test fun observacionVaciaEsInvalida() = assertNotNull(Validadores.observacion("   "))
    @Test fun observacionMuyCortaEsInvalida() = assertNotNull(Validadores.observacion("mal"))
    @Test fun observacionCorrecta() = assertNull(Validadores.observacion("La foto no se lee bien."))

    // ------------------------------------------------------------ Seguridad
    @Test
    fun hashDeClaveVerificaCorrectamente() {
        val sal = Seguridad.generarSal()
        val hash = Seguridad.hashClave("Ventas123", sal)
        assertTrue(Seguridad.verificar("Ventas123", sal, hash))
        assertFalse(Seguridad.verificar("Ventas124", sal, hash))
    }

    @Test
    fun laClaveNoSeGuardaComoTextoVisible() {
        val sal = Seguridad.generarSal()
        assertFalse(Seguridad.hashClave("Ventas123", sal).contains("Ventas123"))
    }

    // ------------------------------------------------------------ Filtros e indicadores
    private fun venta(id: Long, codigo: String, estado: EstadoVenta, vendedor: Long, monto: Int = 100_000) = VentaEntity(
        id = id, vendedorId = vendedor, vendedorNombre = "Vendedor $vendedor",
        tipoConvenio = TipoConvenio.DENTAL.name, prestador = "Prestador", fechaOperacion = hoy, monto = monto,
        codigoAutorizacion = codigo, clienteNombre = "Cliente", clienteRut = "11.111.111-1",
        clienteCorreo = "c@c.cl", clienteTelefono = "912345678", estado = estado.name
    )

    private val ventas = listOf(
        venta(1, "AAA111", EstadoVenta.PENDIENTE_REVISION, 1),
        venta(2, "BBB222", EstadoVenta.VALIDADA, 1, monto = 200_000),
        venta(3, "CCC333", EstadoVenta.VALIDADA, 2, monto = 100_000)
    )

    @Test fun filtroPorCodigoBuscaParcialmenteSinImportarMayusculas() =
        assertEquals(listOf(2L), ventas.aplicarFiltros(FiltrosVentas(texto = "bb2")).map { it.id })

    @Test fun filtroPorEstado() =
        assertEquals(listOf(2L, 3L), ventas.aplicarFiltros(FiltrosVentas(estado = EstadoVenta.VALIDADA)).map { it.id })

    @Test fun filtroPorVendedor() =
        assertEquals(listOf(3L), ventas.aplicarFiltros(FiltrosVentas(vendedorId = 2)).map { it.id })

    @Test fun filtrosCombinados() =
        assertEquals(
            listOf(2L),
            ventas.aplicarFiltros(FiltrosVentas(estado = EstadoVenta.VALIDADA, vendedorId = 1)).map { it.id }
        )

    @Test
    fun indicadoresCalculanMontoYComisionSoloDeVentasValidadas() {
        val indicadores = Indicadores.desde(ventas)
        assertEquals(3, indicadores.totalVentas)
        assertEquals(1, indicadores.pendientesDeRevision)
        assertEquals(300_000, indicadores.montoValidado)
        // Comisión dental ficticia 10% sobre 300.000
        assertEquals(30_000, indicadores.comisionEstimada)
        assertEquals(2, indicadores.pendientesConPrestador)
    }

    @Test
    fun reglasDeEstado() {
        assertTrue(EstadoVenta.BORRADOR.esEditablePorVendedor)
        assertTrue(EstadoVenta.OBSERVADA.esEditablePorVendedor)
        assertFalse(EstadoVenta.VALIDADA.esEditablePorVendedor)   // una venta validada no se modifica
        assertTrue(EstadoVenta.CORREGIDA.esRevisablePorEncargado)
        assertFalse(EstadoVenta.RECHAZADA.esRevisablePorEncargado)
    }

    // ------------------------------------------------------------ Eliminación de ventas por perfil
    @Test
    fun vendedorEliminaBorradorObservadaYRechazada() {
        assertTrue(EstadoVenta.BORRADOR.puedeEliminar(Perfil.VENDEDOR))
        assertTrue(EstadoVenta.OBSERVADA.puedeEliminar(Perfil.VENDEDOR))
        assertTrue(EstadoVenta.RECHAZADA.puedeEliminar(Perfil.VENDEDOR))
    }

    @Test
    fun ventaEnRevisionOValidadaNoSeElimina() {
        for (estado in listOf(EstadoVenta.PENDIENTE_REVISION, EstadoVenta.CORREGIDA, EstadoVenta.VALIDADA, EstadoVenta.GESTIONADA)) {
            assertFalse("$estado no debe eliminarse", estado.puedeEliminar(Perfil.VENDEDOR))
            assertFalse("$estado no debe eliminarse", estado.puedeEliminar(Perfil.ENCARGADO))
        }
    }

    @Test
    fun encargadoSoloEliminaRechazadas() {
        assertTrue(EstadoVenta.RECHAZADA.puedeEliminar(Perfil.ENCARGADO))
        assertFalse(EstadoVenta.BORRADOR.puedeEliminar(Perfil.ENCARGADO))
        assertFalse(EstadoVenta.OBSERVADA.puedeEliminar(Perfil.ENCARGADO))
    }

    @Test
    fun administradorNuncaEliminaVentas() {
        for (estado in EstadoVenta.entries) {
            assertFalse(estado.puedeEliminar(Perfil.ADMINISTRADOR))
        }
    }

    // ------------------------------------------------------------ Perfiles
    @Test fun existenTresPerfiles() = assertEquals(3, Perfil.entries.size)
    @Test fun perfilDesconocidoCaeEnVendedor() = assertEquals(Perfil.VENDEDOR, Perfil.desde("INEXISTENTE"))
    @Test fun perfilSeRecuperaPorNombre() = assertEquals(Perfil.ADMINISTRADOR, Perfil.desde("ADMINISTRADOR"))

    // ------------------------------------------------------------ Comisiones configurables
    @Test
    fun comisionUsaElPorcentajeConfigurado() {
        // Dental por defecto 10%; el administrador lo sube a 20%.
        assertEquals(10_000, TipoConvenio.DENTAL.comisionDe(100_000))
        assertEquals(20_000, TipoConvenio.DENTAL.comisionDe(100_000, 0.20))
    }

    @Test
    fun indicadoresRespetanPorcentajesDelAdministrador() {
        val porcentajes = TipoConvenio.porcentajesPorDefecto() + (TipoConvenio.DENTAL to 0.20)
        val indicadores = Indicadores.desde(ventas, porcentajes)
        // Ventas validadas: 200.000 + 100.000 (dental) con 20%
        assertEquals(60_000, indicadores.comisionEstimada)
    }

    @Test fun porcentajeValidoAceptaComaYPunto() {
        assertNull(Validadores.porcentajeComision("7,5"))
        assertNull(Validadores.porcentajeComision("10"))
        assertNull(Validadores.porcentajeComision("0"))
        assertNull(Validadores.porcentajeComision("30"))
    }

    @Test fun porcentajeInvalido() {
        assertNotNull(Validadores.porcentajeComision(""))
        assertNotNull(Validadores.porcentajeComision("abc"))
        assertNotNull(Validadores.porcentajeComision("31"))
        assertNotNull(Validadores.porcentajeComision("-1"))
    }
}
