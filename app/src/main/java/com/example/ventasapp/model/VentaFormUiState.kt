package com.example.ventasapp.model

import com.example.ventasapp.model.data.remote.convenios.ConvenioDto

/**
 * Estado del formulario de venta. Los campos de texto se guardan como String porque es lo que
 * escribe el usuario; la conversión a Int/Long se hace recién al construir la entidad de Room.
 *
 * Importante: este NO es el objeto que se guarda en la base de datos (ese es VentaEntity).
 * Aquí vive lo que se está escribiendo más los errores de validación.
 */
data class VentaFormUiState(
    val ventaId: Long = 0L,
    val estadoOriginal: EstadoVenta = EstadoVenta.BORRADOR,
    /** Observación que dejó el encargado cuando solicitó la corrección (se muestra como aviso). */
    val observacionPrevia: String? = null,

    val tipoConvenio: TipoConvenio? = null,
    val prestador: String = "",
    val fechaOperacion: Long? = null,
    val monto: String = "",
    val codigo: String = "",

    val clienteNombre: String = "",
    val clienteRut: String = "",
    val clienteCorreo: String = "",
    val clienteTelefono: String = "",
    val mascotaNombre: String = "",

    val respaldoPath: String? = null,

    val cargando: Boolean = false,
    val procesandoImagen: Boolean = false,
    val guardando: Boolean = false,
    /** True si la venta pedida no existe o pertenece a otro vendedor. */
    val noAutorizado: Boolean = false,
    val errores: VentaFormErrores = VentaFormErrores(),

    // Campos para mostrar los convenios recibidos desde la API y controlar el estado de carga
    val convenios: List<ConvenioDto> = emptyList(),
    val convenioSeleccionado: ConvenioDto? = null,
    val cargandoConvenios: Boolean = false,
    val errorConvenios: String? = null
) {
    val esEdicion: Boolean get() = ventaId != 0L
}