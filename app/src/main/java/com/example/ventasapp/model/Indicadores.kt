package com.example.ventasapp.model

import com.example.ventasapp.model.data.local.VentaEntity

/** Resumen por vendedor para la tabla de indicadores del encargado. */
data class ResumenVendedor(
    val vendedorNombre: String,
    val ventasValidadas: Int,
    val montoValidado: Int,
    val comisionEstimada: Int
)

/**
 * Indicadores generales del proceso. Todo se calcula en memoria a partir de la lista de ventas,
 * así que se actualiza solo cuando cambia la base de datos (porque la lista viene de un Flow).
 */
data class Indicadores(
    val totalVentas: Int = 0,
    val porEstado: Map<EstadoVenta, Int> = emptyMap(),
    val pendientesDeRevision: Int = 0,
    val montoValidado: Int = 0,
    val comisionEstimada: Int = 0,
    /** Ventas validadas que todavía no se derivan (simulado) al prestador. */
    val pendientesConPrestador: Int = 0,
    val porVendedor: List<ResumenVendedor> = emptyList()
) {
    companion object {
        fun desde(
            ventas: List<VentaEntity>,
            porcentajes: Map<TipoConvenio, Double> = TipoConvenio.porcentajesPorDefecto()
        ): Indicadores {
            val validadas = ventas.filter { EstadoVenta.desde(it.estado).estaValidada }

            // Usa el porcentaje configurado por el administrador para cada tipo de convenio.
            fun comision(v: VentaEntity): Int =
                TipoConvenio.desde(v.tipoConvenio)?.let { it.comisionDe(v.monto, porcentajes[it] ?: it.porcentajeComision) } ?: 0

            return Indicadores(
                totalVentas = ventas.size,
                porEstado = EstadoVenta.entries.associateWith { estado ->
                    ventas.count { it.estado == estado.name }
                },
                pendientesDeRevision = ventas.count { EstadoVenta.desde(it.estado).esRevisablePorEncargado },
                montoValidado = validadas.sumOf { it.monto },
                comisionEstimada = validadas.sumOf { comision(it) },
                pendientesConPrestador = ventas.count { it.estado == EstadoVenta.VALIDADA.name },
                porVendedor = validadas.groupBy { it.vendedorNombre }.map { (nombre, lista) ->
                    ResumenVendedor(
                        vendedorNombre = nombre,
                        ventasValidadas = lista.size,
                        montoValidado = lista.sumOf { it.monto },
                        comisionEstimada = lista.sumOf { comision(it) }
                    )
                }.sortedByDescending { it.montoValidado }
            )
        }
    }
}
