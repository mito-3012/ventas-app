package com.example.ventasapp.model.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Alertas internas (el caso permite notificaciones dentro de la app).
 *
 * El destino es un usuario puntual (usuarioDestinoId) o, si ese campo es null,
 * todos los usuarios del perfil indicado en perfilDestino (por ejemplo "todos los encargados").
 */
@Entity(
    tableName = "notificaciones",
    indices = [Index("usuarioDestinoId"), Index("perfilDestino")]
)
data class NotificacionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val usuarioDestinoId: Long?,
    val perfilDestino: String,
    val ventaId: Long,
    val mensaje: String,
    val fecha: Long = System.currentTimeMillis(),
    val leida: Boolean = false
)
