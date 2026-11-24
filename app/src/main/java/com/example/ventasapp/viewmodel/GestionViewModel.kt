package com.example.ventasapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ventasapp.model.FiltrosVentas
import com.example.ventasapp.model.Indicadores
import com.example.ventasapp.model.aplicarFiltros
import com.example.ventasapp.model.data.local.UsuarioEntity
import com.example.ventasapp.model.data.local.VentaEntity
import com.example.ventasapp.repositorio
import com.example.ventasapp.util.EventoUi
import com.example.ventasapp.util.ReglaNegocioException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel del encargado: ve todas las ventas recibidas, las filtra por vendedor, fecha y estado,
 * y calcula los indicadores generales del proceso.
 */
class GestionViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = repositorio

    private val _filtros = MutableStateFlow(FiltrosVentas())
    val filtros: StateFlow<FiltrosVentas> = _filtros

    private val _eventos = Channel<EventoUi>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    /** Ventas enviadas por los vendedores (nunca incluye borradores). */
    val recibidas: StateFlow<List<VentaEntity>> = repo.ventasRecibidas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filtradas: StateFlow<List<VentaEntity>> = combine(recibidas, _filtros) { lista, filtros ->
        lista.aplicarFiltros(filtros)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Opciones del filtro "vendedor". */
    val vendedores: StateFlow<List<UsuarioEntity>> = repo.vendedores()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val indicadores: StateFlow<Indicadores> = combine(
        recibidas,
        repo.comisiones()
    ) { ventas, porcentajes ->
        Indicadores.desde(ventas, porcentajes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Indicadores())

    init {
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

    /** Borra las fotos de prueba (el caso exige poder eliminarlas al terminar el proyecto). */
    fun eliminarRespaldosDePrueba() {
        val usuario = repo.sesion.usuario.value ?: return
        viewModelScope.launch {
            try {
                val borrados = repo.eliminarRespaldosDePrueba(usuario)
                _eventos.trySend(EventoUi("Se eliminaron $borrados respaldos de prueba."))
            } catch (e: ReglaNegocioException) {
                _eventos.trySend(EventoUi(e.message ?: "No se pudo completar la acción.", true))
            }
        }
    }
}