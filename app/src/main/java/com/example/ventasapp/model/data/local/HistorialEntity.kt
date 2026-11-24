package com.example.ventasapp.model.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Cada fila es un cambio relevante de una venta: quién, cuándo, de qué estado a cuál. */
@Entity(
    tableName = "historial",
    indices = [Index("ventaId")]
)
data class HistorialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ventaId: Long,
    val estadoAnterior: String?,
    val estadoNuevo: String,
    val accion: String,
    val usuarioId: Long,
    val usuarioNombre: String,
    val fecha: Long = System.currentTimeMillis(),
    val observacion: String? = null
)
