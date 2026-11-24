package com.example.ventasapp.model

/**
 * Tipos de convenio que comercializa la agencia (según el caso). Todos los prestadores son ficticios.
 *
 * @param porcentajeComision porcentaje FICTICIO usado solo para la simulación de comisiones.
 * @param requiereMascota si es true, el formulario pide el nombre (ficticio) de la mascota.
 */
enum class TipoConvenio(
    val etiqueta: String,
    val porcentajeComision: Double,
    val requiereMascota: Boolean,
    val prestadores: List<String>
) {
    DENTAL(
        "Convenio dental", 0.10, false,
        listOf("Clínica Dental Sonrisa (ficticia)", "Dental Norte (ficticia)")
    ),
    AUTOMOTRIZ(
        "Servicio automotriz", 0.08, false,
        listOf("Taller Rápido Centro (ficticio)", "AutoCare Sur (ficticio)")
    ),
    VETERINARIO(
        "Servicio veterinario", 0.12, true,
        listOf("Veterinaria Patitas (ficticia)", "VetSalud Oriente (ficticia)")
    ),
    OTRO(
        "Otro beneficio", 0.05, false,
        listOf("Prestador General (ficticio)")
    );

    /**
     * Comisión simulada (en pesos) para un monto dado. No tiene ningún efecto real.
     * [porcentaje] es el valor configurado por el administrador; si no se entrega, se usa el
     * porcentaje predeterminado del convenio.
     */
    fun comisionDe(monto: Int, porcentaje: Double = porcentajeComision): Int = (monto * porcentaje).toInt()

    companion object {
        /** Devuelve null cuando el texto está vacío (borrador que aún no eligió convenio). */
        fun desde(nombre: String): TipoConvenio? = entries.firstOrNull { it.name == nombre }

        /** Porcentajes iniciales (antes de que el administrador los modifique). */
        fun porcentajesPorDefecto(): Map<TipoConvenio, Double> = entries.associateWith { it.porcentajeComision }
    }
}
