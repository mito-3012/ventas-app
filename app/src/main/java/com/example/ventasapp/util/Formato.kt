package com.example.ventasapp.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Funciones para mostrar fechas y montos con formato chileno. */
object Formato {

    private val localeChile: Locale = Locale.forLanguageTag("es-CL")
    private val zonaUtc: TimeZone = TimeZone.getTimeZone("UTC")

    /**
     * Fecha de la operación. El DatePicker de Material entrega la fecha como medianoche en UTC,
     * así que se formatea en UTC; si se usara la zona de Chile se vería el día anterior.
     */
    fun fechaOperacion(millis: Long?): String {
        if (millis == null) return "Sin fecha"
        val formato = SimpleDateFormat("dd/MM/yyyy", localeChile).apply { timeZone = zonaUtc }
        return formato.format(Date(millis))
    }

    /** Fecha y hora de un evento (envío, revisión, historial) en la zona horaria del teléfono. */
    fun fechaHora(millis: Long?): String {
        if (millis == null) return "-"
        return SimpleDateFormat("dd/MM/yyyy HH:mm", localeChile).format(Date(millis))
    }

    /** 1500000 -> "$1.500.000" */
    fun pesos(monto: Int): String = "$" + NumberFormat.getIntegerInstance(localeChile).format(monto)

    /**
     * "Hoy" expresado como medianoche UTC, que es el mismo formato que usa el DatePicker.
     * Se toma el día calendario local (el de la persona) y se reconstruye en UTC.
     */
    fun hoyComoFechaUtc(): Long {
        val local = Calendar.getInstance()
        val utc = Calendar.getInstance(zonaUtc).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }
        return utc.timeInMillis
    }
}
