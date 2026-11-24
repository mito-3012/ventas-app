package com.example.ventasapp.model

data class VentaFormErrores(
    val tipoConvenio: String? = null,
    val prestador: String? = null,
    val fecha: String? = null,
    val monto: String? = null,
    val codigo: String? = null,
    val clienteNombre: String? = null,
    val clienteRut: String? = null,
    val clienteCorreo: String? = null,
    val clienteTelefono: String? = null,
    val mascota: String? = null,
    val respaldo: String? = null
) {
    /** true si hay al menos un error en el formulario. */
    val hayErrores: Boolean
        get() = listOfNotNull(
            tipoConvenio, prestador, fecha, monto, codigo, clienteNombre, clienteRut,
            clienteCorreo, clienteTelefono, mascota, respaldo
        ).isNotEmpty()
}
