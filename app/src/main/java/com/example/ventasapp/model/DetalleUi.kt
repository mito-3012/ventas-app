package com.example.ventasapp.model

import com.example.ventasapp.model.data.local.VentaEntity

/**
 * Los tres estados posibles de la pantalla de detalle. Con una "sealed interface" el compilador
 * obliga a la pantalla a manejar todos los casos (cargando, sin acceso, con datos).
 */
sealed interface DetalleUi {
    data object Cargando : DetalleUi
    /** La venta no existe o el usuario no tiene permiso para verla. */
    data object NoDisponible : DetalleUi
    data class Listo(val venta: VentaEntity) : DetalleUi
}
