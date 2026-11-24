package com.example.ventasapp.model.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VentaDao {

    @Insert
    suspend fun insertar(venta: VentaEntity): Long

    @Update
    suspend fun actualizar(venta: VentaEntity)

    @Query("SELECT * FROM ventas WHERE id = :id")
    suspend fun obtenerPorId(id: Long): VentaEntity?

    @Query("SELECT * FROM ventas WHERE id = :id")
    fun observarPorId(id: Long): Flow<VentaEntity?>

    /** Control de acceso: el vendedor solo consulta las ventas cuyo vendedorId es el suyo. */
    @Query("SELECT * FROM ventas WHERE vendedorId = :vendedorId ORDER BY fechaActualizacion DESC")
    fun observarDeVendedor(vendedorId: Long): Flow<List<VentaEntity>>

    /** El encargado ve todo lo enviado, pero nunca los borradores de los vendedores. */
    @Query("SELECT * FROM ventas WHERE estado != 'BORRADOR' ORDER BY fechaActualizacion DESC")
    fun observarRecibidas(): Flow<List<VentaEntity>>

    /** Rutas de fotos en uso: sirve para detectar y borrar fotos que quedaron sin venta. */
    @Query("SELECT respaldoPath FROM ventas WHERE respaldoPath IS NOT NULL")
    suspend fun rutasDeRespaldo(): List<String>

    /** Cuenta otras ventas con el mismo código (se excluye la propia al editar). Detecta duplicados. */
    @Query("SELECT COUNT(*) FROM ventas WHERE codigoAutorizacion = :codigo COLLATE NOCASE AND id != :excluirId")
    suspend fun contarCodigo(codigo: String, excluirId: Long): Int

    @Query("DELETE FROM ventas WHERE id = :id")
    suspend fun eliminar(id: Long)

    @Query("UPDATE ventas SET respaldoPath = NULL")
    suspend fun limpiarRutasDeRespaldo()
}
