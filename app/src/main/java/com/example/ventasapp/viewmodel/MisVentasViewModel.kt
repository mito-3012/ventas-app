package com.example.ventasapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ventasapp.model.FiltrosVentas
import com.example.ventasapp.model.Indicadores
import com.example.ventasapp.model.aplicarFiltros
import com.example.ventasapp.model.data.local.VentaEntity
import com.example.ventasapp.repositorio
import com.example.ventasapp.util.EventoUi
import com.example.ventasapp.util.ReglaNegocioException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel del vendedor: su historial de ventas, filtros y resumen.
 *
 * Cadena reactiva:
 *   usuario actual -> consulta Room (solo SUS ventas) -> filtros -> lista que ve la pantalla
 * Si cambia cualquier eslabón (nueva venta guardada, otro filtro, otro usuario) todo se recalcula solo.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MisVentasViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = repositorio

    /** Mensajes de una sola vez para el Snackbar (venta eliminada, error de permisos...). */
    private val _eventos = Channel<EventoUi>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    private val _filtros = MutableStateFlow(FiltrosVentas())
    val filtros: StateFlow<FiltrosVentas> = _filtros

    /** Todas las ventas del vendedor conectado, sin filtrar. flatMapLatest reacciona si cambia el usuario. */
    val todas: StateFlow<List<VentaEntity>> = repo.sesion.usuario
        .flatMapLatest { usuario ->
            if (usuario == null) flowOf(emptyList()) else repo.ventasDeVendedor(usuario.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Lo que muestra la lista: todas las ventas con los filtros aplicados. */
    val filtradas: StateFlow<List<VentaEntity>> = combine(todas, _filtros) { lista, filtros ->
        lista.aplicarFiltros(filtros)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Resumen ficticio para la pantalla de inicio (cantidades por estado y comisión estimada). */
    val resumen: StateFlow<Indicadores> = combine(todas, repo.comisiones()) { ventas, porcentajes ->
        Indicadores.desde(
            ventas,
            porcentajes
        )   // usa los porcentajes que configuró el administrador
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Indicadores())

    init {
        // Si entra otro usuario, los filtros del anterior no deben quedar aplicados.
        viewModelScope.launch {
            repo.sesion.usuario.collect { _filtros.value = FiltrosVentas() }
        }
    }

    fun onFiltrosChange(nuevos: FiltrosVentas) {
        _filtros.value = nuevos
    }

    fun limpiarFiltros() {
        _filtros.value = FiltrosVentas()
    }

    /** Elimina una venta (con su historial y su foto). Los permisos los valida el repositorio. */
    fun eliminarVenta(ventaId: Long) {
        val usuario = repo.sesion.usuario.value ?: return
        viewModelScope.launch {
            try {
                repo.eliminarVenta(ventaId, usuario)
                _eventos.trySend(EventoUi("Venta eliminada correctamente."))
            } catch (e: ReglaNegocioException) {
                _eventos.trySend(EventoUi(e.message ?: "No se pudo eliminar la venta.", true))
            }
        }
    }
}