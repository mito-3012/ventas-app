package com.example.ventasapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ventasapp.model.AccionRevision
import com.example.ventasapp.model.DetalleUi
import com.example.ventasapp.model.EstadoVenta
import com.example.ventasapp.model.PostNuevo
import com.example.ventasapp.model.RevisionUiState
import com.example.ventasapp.model.TipoConvenio
import com.example.ventasapp.model.data.local.HistorialEntity
import com.example.ventasapp.model.data.local.VentaEntity
import com.example.ventasapp.model.data.remote.RetrofitInstance
import com.example.ventasapp.model.data.repository.PostRepository
import com.example.ventasapp.repositorio
import com.example.ventasapp.util.ErroresRed
import com.example.ventasapp.util.EventoUi
import com.example.ventasapp.util.Formato
import com.example.ventasapp.util.ReglaNegocioException
import com.example.ventasapp.util.Validadores
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

@OptIn(ExperimentalCoroutinesApi::class)
class DetalleVentaViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = repositorio
    private val ventaId = MutableStateFlow(0L)

    val detalle: StateFlow<DetalleUi> = combine(
        repo.sesion.usuario,
        ventaId
    ) { usuario, id -> usuario to id }
        .flatMapLatest { (usuario, id) ->
            repo.observarVentaAutorizada(id, usuario)
                .map { venta -> if (venta == null) DetalleUi.NoDisponible else DetalleUi.Listo(venta) }
                .onStart { emit(DetalleUi.Cargando) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleUi.Cargando)

    val historial: StateFlow<List<HistorialEntity>> = detalle
        .flatMapLatest { estado ->
            if (estado is DetalleUi.Listo) repo.historial(estado.venta.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val comisiones: StateFlow<Map<TipoConvenio, Double>> = repo.comisiones()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TipoConvenio.porcentajesPorDefecto())

    // Repositorio remoto utilizado para enviar el resumen de la venta al servicio de pruebas.
    private val postRepository = PostRepository(RetrofitInstance.api)
    // Indica si existe una derivación en curso y evita que se envíen solicitudes duplicadas.
    private val _derivando = MutableStateFlow(false)
    val derivando: StateFlow<Boolean> = _derivando

    private val _revision = MutableStateFlow(RevisionUiState())
    val revision: StateFlow<RevisionUiState> = _revision

    private val _eventos = Channel<EventoUi>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    fun cargar(id: Long) {
        if (ventaId.value != id) {
            _revision.value = RevisionUiState()
            ventaId.value = id
        }
    }

    fun onObservacionChange(valor: String) {
        _revision.update { it.copy(observacion = valor.take(300), errores = it.errores.copy(observacion = null)) }
    }

    fun usarSugerencia(texto: String) {
        _revision.update { it.copy(observacion = texto, errores = it.errores.copy(observacion = null)) }
    }

    fun solicitarAccion(accion: AccionRevision) {
        val observacion = _revision.value.observacion
        val error = if (accion.requiereObservacion) Validadores.observacion(observacion) else null
        if (error != null) {
            _revision.update { it.copy(errores = it.errores.copy(observacion = error)) }
            _eventos.trySend(EventoUi("Escribe una observación para continuar.", true))
            return
        }
        _revision.update { it.copy(accionPendiente = accion, errores = it.errores.copy(observacion = null)) }
    }

    fun cancelarAccion() {
        _revision.update { it.copy(accionPendiente = null) }
    }

    fun confirmarAccion() {
        val accion = _revision.value.accionPendiente ?: return
        val usuario = repo.sesion.usuario.value ?: return
        val id = ventaId.value
        val observacion = _revision.value.observacion
        viewModelScope.launch {
            _revision.update { it.copy(procesando = true, accionPendiente = null) }
            try {
                repo.revisar(id, accion, observacion, usuario)
                _revision.value = RevisionUiState()
                val mensaje = when (accion) {
                    AccionRevision.VALIDAR -> "Venta validada correctamente."
                    AccionRevision.SOLICITAR_CORRECCION -> "Se solicitó la corrección al vendedor."
                    AccionRevision.RECHAZAR -> "La venta fue rechazada."
                }
                _eventos.trySend(EventoUi(mensaje))
            } catch (e: ReglaNegocioException) {
                _revision.update { it.copy(procesando = false) }
                _eventos.trySend(EventoUi(e.message ?: "No se pudo completar la acción.", true))
            }
        }
    }

    /** Envía un resumen operativo sin datos personales del cliente y registra la respuesta del servicio. */
    fun derivarAlPrestador() {
        val usuario = repo.sesion.usuario.value ?: return
        val venta = (detalle.value as? DetalleUi.Listo)?.venta ?: return
        if (_derivando.value) return
        if (EstadoVenta.desde(venta.estado) != EstadoVenta.VALIDADA) {
            _eventos.trySend(
                EventoUi(
                    "Solo se pueden derivar al prestador las ventas validadas.",
                    true
                )
            )
            return
        }

        viewModelScope.launch {
            _derivando.value = true
            try {
                // La API de demostración requiere un identificador numérico de usuario.
                val userId = venta.vendedorId.coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()
                // Registra la solicitud mediante POST y recibe la respuesta de la API.
                val confirmacion = postRepository.crearPost(
                    PostNuevo(
                        userId = userId,
                        title = "Derivación de la venta ${venta.codigoAutorizacion}",
                        body = resumenParaPrestador(venta)
                    )
                )
                // Solo después de recibir respuesta se actualiza el estado local de la venta.
                repo.marcarGestionada(
                    ventaId = venta.id,
                    usuario = usuario,
                    referenciaExterna = "Confirmación de la API de pruebas (Partner simulado): publicación N.° ${confirmacion.id}."
                )
                _eventos.trySend(EventoUi("Venta derivada al prestador. Confirmación de la API: N.° ${confirmacion.id}."))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val mensaje = when (e) {
                    is ReglaNegocioException -> e.message ?: "No se pudo derivar la venta."
                    is HttpException -> ErroresRed.mensajeParaCodigoHttp(e.code()) + " La venta no fue derivada."
                    else -> ErroresRed.mensajeDeError(e) + " La venta no fue derivada."
                }
                _eventos.trySend(EventoUi(mensaje, true))
            } finally {
                _derivando.value = false
            }
        }
    }

    // Construye el texto que se enviará al servicio, limitado a información necesaria para la prueba.
    private fun resumenParaPrestador(venta: VentaEntity): String {
        val convenio = TipoConvenio.desde(venta.tipoConvenio)?.etiqueta ?: "Sin definir"
        return "Convenio: $convenio. Prestador: ${venta.prestador}. " +
            "Monto (ficticio): ${Formato.pesos(venta.monto)}. Fecha: ${Formato.fechaOperacion(venta.fechaOperacion)}."
    }

    fun eliminarVenta(onListo: () -> Unit) {
        val usuario = repo.sesion.usuario.value ?: return
        viewModelScope.launch {
            try {
                repo.eliminarVenta(ventaId.value, usuario)
                _eventos.trySend(EventoUi("Venta eliminada correctamente."))
                onListo()
            } catch (e: ReglaNegocioException) {
                _eventos.trySend(EventoUi(e.message ?: "No se pudo eliminar la venta.", true))
            }
        }
    }
}