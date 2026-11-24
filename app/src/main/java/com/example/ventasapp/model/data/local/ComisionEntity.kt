package com.example.ventasapp.model.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Porcentaje de comisión (ficticio) por tipo de convenio. Lo edita el administrador.
 * Se guarda como fracción: 0.10 equivale a 10%.
 */
@Entity(tableName = "comisiones")
data class ComisionEntity(
    @PrimaryKey val tipoConvenio: String,
    val porcentaje: Double
)
