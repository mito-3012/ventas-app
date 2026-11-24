package com.example.ventasapp.model

/**
 * Estados por los que pasa una venta. Es el corazón de la trazabilidad del caso.
 *
 * Flujo normal:
 *   BORRADOR -> PENDIENTE_REVISION -> VALIDADA -> GESTIONADA
 * Flujos alternativos:
 *   PENDIENTE_REVISION -> OBSERVADA -> (vendedor corrige) -> CORREGIDA -> VALIDADA
 *   PENDIENTE_REVISION / CORREGIDA -> RECHAZADA
 */
enum class EstadoVenta(val etiqueta: String) {
    BORRADOR("Borrador"),
    PENDIENTE_REVISION("Pendiente de revisión"),
    OBSERVADA("Observada"),
    CORREGIDA("Corregida"),
    VALIDADA("Validada"),
    RECHAZADA("Rechazada"),
    GESTIONADA("Gestionada con el prestador");

    /** El vendedor solo puede editar mientras es borrador o cuando el encargado la observó. */
    val esEditablePorVendedor: Boolean
        get() = this == BORRADOR || this == OBSERVADA

    /** El encargado solo puede resolver ventas que están esperando su revisión. */
    val esRevisablePorEncargado: Boolean
        get() = this == PENDIENTE_REVISION || this == CORREGIDA

    /** Una venta validada (o ya gestionada) cuenta para comisiones. Una vez validada no se modifica. */
    val estaValidada: Boolean
        get() = this == VALIDADA || this == GESTIONADA

    /**
     * Regla ÚNICA de eliminación: la usan la interfaz (para mostrar u ocultar el botón) y el
     * repositorio (para aceptar o rechazar la operación), así nunca se contradicen.
     *  - El vendedor elimina sus ventas en borrador, observadas o rechazadas.
     *  - El encargado solo elimina ventas rechazadas.
     *  - Una venta en revisión, validada o gestionada no se elimina (trazabilidad del caso).
     */
    fun puedeEliminar(perfil: Perfil): Boolean = when (perfil) {
        Perfil.VENDEDOR -> this == BORRADOR || this == OBSERVADA || this == RECHAZADA
        Perfil.ENCARGADO -> this == RECHAZADA
        Perfil.ADMINISTRADOR -> false
    }

    companion object {
        fun desde(nombre: String): EstadoVenta = entries.firstOrNull { it.name == nombre } ?: BORRADOR
    }
}

/**
 * Acciones que puede ejecutar el encargado al revisar. Cada acción lleva asociado el estado
 * al que pasa la venta y si exige o no una observación escrita.
 */
enum class AccionRevision(
    val estadoResultante: EstadoVenta,
    val etiqueta: String,
    val requiereObservacion: Boolean
) {
    VALIDAR(EstadoVenta.VALIDADA, "Validar venta", false),
    SOLICITAR_CORRECCION(EstadoVenta.OBSERVADA, "Solicitar corrección", true),
    RECHAZAR(EstadoVenta.RECHAZADA, "Rechazar venta", true)
}
