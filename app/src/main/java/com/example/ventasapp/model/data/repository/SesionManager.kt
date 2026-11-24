package com.example.ventasapp.model.data.repository

import com.example.ventasapp.model.data.local.UsuarioEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Guarda en memoria quién tiene la sesión iniciada.
 *
 * Vive en la clase Application, así que todos los ViewModels leen EL MISMO usuario actual.
 * Es solo en memoria a propósito: al cerrar la app hay que volver a ingresar (más seguro para un MVP).
 */
class SesionManager {
    private val _usuario = MutableStateFlow<UsuarioEntity?>(null)

    /** null = nadie ha iniciado sesión. */
    val usuario: StateFlow<UsuarioEntity?> = _usuario

    fun iniciar(usuario: UsuarioEntity) {
        _usuario.value = usuario
    }

    fun cerrar() {
        _usuario.value = null
    }
}
