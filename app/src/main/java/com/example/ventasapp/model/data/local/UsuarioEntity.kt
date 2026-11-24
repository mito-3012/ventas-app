package com.example.ventasapp.model.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * ENTIDADES DE ROOM (tablas de la base de datos).
 *
 * Recordatorio de la guía de persistencia: una Entity NO es lo mismo que el UiState.
 *  - UiState  = lo que se está escribiendo en el formulario + sus errores (vive en memoria).
 *  - Entity   = lo que se guarda de forma permanente en el teléfono.
 *
 * Los enums (perfil, estado, tipo de convenio) se guardan como texto con el nombre del enum.
 * Es simple, legible al revisar la base y no requiere TypeConverters.
 */

@Entity(
    tableName = "usuarios",
    // Índice único: dos cuentas no pueden compartir correo.
    indices = [Index(value = ["correo"], unique = true)]
)
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val correo: String,
    /** Hash PBKDF2 de la contraseña. La contraseña original jamás se guarda. */
    val claveHash: String,
    /** Sal aleatoria propia de este usuario (ver util/Seguridad). */
    val sal: String,
    val perfil: String,
    val zona: String,
    val activo: Boolean = true,
    val fechaCreacion: Long = System.currentTimeMillis()
)
