package com.example.ventasapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ventasapp.model.data.local.NotificacionEntity
import com.example.ventasapp.repositorio
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel de las alertas internas (el caso permite notificaciones dentro de la app). */
@OptIn(ExperimentalCoroutinesApi::class)
class AlertasViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = repositorio

    val notificaciones: StateFlow<List<NotificacionEntity>> = repo.sesion.usuario
        .flatMapLatest { usuario ->
            if (usuario == null) flowOf(emptyList()) else repo.notificaciones(usuario)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Número que se muestra en la insignia (badge) de la barra inferior. */
    val noLeidas: StateFlow<Int> = repo.sesion.usuario
        .flatMapLatest { usuario ->
            if (usuario == null) flowOf(0) else repo.cantidadNoLeidas(usuario)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun marcarTodasLeidas() {
        val usuario = repo.sesion.usuario.value ?: return
        viewModelScope.launch { repo.marcarNotificacionesLeidas(usuario) }
    }
}