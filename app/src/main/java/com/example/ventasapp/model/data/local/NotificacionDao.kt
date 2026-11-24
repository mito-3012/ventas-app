package com.example.ventasapp.model.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificacionDao {

    @Insert
    suspend fun insertar(notificacion: NotificacionEntity)

    @Query(
        "SELECT * FROM notificaciones " +
            "WHERE usuarioDestinoId = :usuarioId OR (usuarioDestinoId IS NULL AND perfilDestino = :perfil) " +
            "ORDER BY fecha DESC, id DESC"
    )
    fun observarPara(usuarioId: Long, perfil: String): Flow<List<NotificacionEntity>>

    @Query(
        "SELECT COUNT(*) FROM notificaciones " +
            "WHERE leida = 0 AND (usuarioDestinoId = :usuarioId OR (usuarioDestinoId IS NULL AND perfilDestino = :perfil))"
    )
    fun observarNoLeidas(usuarioId: Long, perfil: String): Flow<Int>

    @Query(
        "UPDATE notificaciones SET leida = 1 " +
            "WHERE usuarioDestinoId = :usuarioId OR (usuarioDestinoId IS NULL AND perfilDestino = :perfil)"
    )
    suspend fun marcarTodasLeidas(usuarioId: Long, perfil: String)
}
