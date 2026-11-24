package com.example.ventasapp.model

/** Estado del panel de revisión que usa el encargado en el detalle de la venta. */
data class RevisionUiState(
    val observacion: String = "",
    val errores: RevisionErrores = RevisionErrores(),
    /** Acción que espera confirmación en el AlertDialog (null = no hay diálogo abierto). */
    val accionPendiente: AccionRevision? = null,
    val procesando: Boolean = false
)
