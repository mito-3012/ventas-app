package com.example.ventasapp.model

import com.example.ventasapp.model.data.local.VentaEntity

/**
 * Criterios de búsqueda/filtro de ventas. Los usan tanto el vendedor como el encargado.
 * Cada criterio en null (o vacío) significa "sin filtrar por ese campo".
 */
data class FiltrosVentas(
    val texto: String = "",
    val estado: EstadoVenta? = null,
    val vendedorId: Long? = null,
    /** Fecha de la operación (medianoche UTC en milisegundos), tal como la entrega el DatePicker. */
    val fechaOperacion: Long? = null
) {
    val hayFiltrosActivos: Boolean
        get() = texto.isNotBlank() || estado != null || vendedorId != null || fechaOperacion != null
}

/**
 * Aplica los filtros a una lista de ventas. Es una función pura (sin Android), por eso se puede
 * probar con una prueba unitaria normal.
 */
fun List<VentaEntity>.aplicarFiltros(filtros: FiltrosVentas): List<VentaEntity> {
    val busqueda = filtros.texto.trim()
    return filter { venta ->
        (busqueda.isEmpty() || venta.codigoAutorizacion.contains(busqueda, ignoreCase = true)) &&
            (filtros.estado == null || venta.estado == filtros.estado.name) &&
            (filtros.vendedorId == null || venta.vendedorId == filtros.vendedorId) &&
            (filtros.fechaOperacion == null || venta.fechaOperacion == filtros.fechaOperacion)
    }
}
