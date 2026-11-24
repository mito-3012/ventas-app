package com.example.ventasapp.model.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistorialDao {

    @Insert
    suspend fun insertar(registro: HistorialEntity)

    @Query("SELECT * FROM historial WHERE ventaId = :ventaId ORDER BY fecha DESC, id DESC")
    fun observarDeVenta(ventaId: Long): Flow<List<HistorialEntity>>

    @Query("DELETE FROM historial WHERE ventaId = :ventaId")
    suspend fun eliminarDeVenta(ventaId: Long)
}
