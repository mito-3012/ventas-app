package com.example.ventasapp.model.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/*
 * DAOs (Data Access Object): aquí se declaran las consultas SQL.
 *  - Las funciones "suspend" se ejecutan fuera del hilo principal (corrutinas).
 *  - Las que devuelven Flow se vuelven a emitir solas cada vez que cambia la tabla; así la UI
 *    se actualiza sin que nadie tenga que "refrescar" a mano.
 */

@Dao
interface UsuarioDao {

    @Insert
    suspend fun insertar(usuario: UsuarioEntity): Long

    @Query("SELECT * FROM usuarios WHERE correo = :correo LIMIT 1")
    suspend fun buscarPorCorreo(correo: String): UsuarioEntity?

    @Query("SELECT * FROM usuarios WHERE id = :id")
    suspend fun buscarPorId(id: Long): UsuarioEntity?

    @Update
    suspend fun actualizar(usuario: UsuarioEntity)

    @Query("SELECT COUNT(*) FROM usuarios")
    suspend fun contar(): Int

    /** Lista completa para la pantalla de administración de usuarios. */
    @Query("SELECT * FROM usuarios ORDER BY perfil, nombre")
    fun observarTodos(): Flow<List<UsuarioEntity>>

    @Query("SELECT * FROM usuarios WHERE perfil = 'VENDEDOR' AND activo = 1 ORDER BY nombre")
    fun observarVendedores(): Flow<List<UsuarioEntity>>
}
