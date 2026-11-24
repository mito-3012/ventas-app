package com.example.ventasapp.model.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ComisionDao {

    @Query("SELECT * FROM comisiones")
    fun observarTodas(): Flow<List<ComisionEntity>>

    /** Crea el valor inicial solo si todavía no existe (no pisa lo que ya configuró el administrador). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarSiNoExiste(comision: ComisionEntity)

    /** Inserta o reemplaza (upsert) el porcentaje de un convenio. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(comision: ComisionEntity)
}
