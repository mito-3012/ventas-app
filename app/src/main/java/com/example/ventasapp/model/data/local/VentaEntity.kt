package com.example.ventasapp.model.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ventas",
    indices = [Index("vendedorId"), Index("estado")]
)
data class VentaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    val vendedorId: Long,
    /** Copia del nombre del vendedor al momento de crear la venta; evita un JOIN en cada listado. */
    val vendedorNombre: String,

    val tipoConvenio: String,
    val prestador: String,
    val fechaOperacion: Long?,
    val monto: Int,
    val codigoAutorizacion: String,

    // Datos del cliente: SIEMPRE ficticios (restricción de protección de datos del caso).
    val clienteNombre: String,
    val clienteRut: String,
    val clienteCorreo: String,
    val clienteTelefono: String,
    val mascotaNombre: String = "",

    /** Ruta del archivo de imagen en el almacenamiento privado de la app (null = sin respaldo). */
    val respaldoPath: String? = null,

    val estado: String,
    val observacion: String? = null,
    val motivoRechazo: String? = null,

    val fechaEnvio: Long? = null,
    val fechaRevision: Long? = null,
    val revisorId: Long? = null,
    val revisorNombre: String? = null,
    val fechaActualizacion: Long = System.currentTimeMillis()
)
