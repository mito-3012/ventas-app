package com.example.ventasapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ventasapp.model.ConfiguracionUiState
import com.example.ventasapp.model.TipoConvenio
import com.example.ventasapp.repositorio
import com.example.ventasapp.util.EventoUi
import com.example.ventasapp.util.ReglaNegocioException
import com.example.ventasapp.util.Validadores
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** ViewModel de la configuración (perfil ADMINISTRADOR): porcentajes de comisión ficticios. */
class ConfiguracionViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = repositorio

    private val _estado = MutableStateFlow(ConfiguracionUiState())
    val estado: StateFlow<ConfiguracionUiState> = _estado

    private val _eventos = Channel<EventoUi>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    init {
        // Carga una sola vez los porcentajes vigentes para rellenar los campos.
        viewModelScope.launch {
            val actuales = repo.comisiones().first()
            _estado.value = ConfiguracionUiState(valores = actuales.mapValues { formatear(it.value) })
        }
    }

    /** Solo se aceptan dígitos, coma y punto, hasta 5 caracteres. */
    fun onValorChange(tipo: TipoConvenio, texto: String) {
        val limpio = texto.filter { it.isDigit() || it == ',' || it == '.' }.take(5)
        _estado.update {
            it.copy(valores = it.valores + (tipo to limpio), errores = it.errores + (tipo to null))
        }
    }

    fun guardar(tipo: TipoConvenio) {
        val admin = repo.sesion.usuario.value ?: return
        val texto = _estado.value.valores[tipo].orEmpty()
        val error = Validadores.porcentajeComision(texto)
        if (error != null) {
            _estado.update { it.copy(errores = it.errores + (tipo to error)) }
            _eventos.trySend(EventoUi("Revisa el porcentaje ingresado.", true))
            return
        }
        val porcentaje = texto.trim().replace(',', '.').toDouble()

        viewModelScope.launch {
            try {
                repo.actualizarComision(tipo, porcentaje / 100.0, admin)
                _estado.update { it.copy(valores = it.valores + (tipo to formatear(porcentaje / 100.0))) }
                _eventos.trySend(EventoUi("Comisión de \"${tipo.etiqueta}\" actualizada a ${formatear(porcentaje / 100.0)}%."))
            } catch (e: ReglaNegocioException) {
                _eventos.trySend(EventoUi(e.message ?: "No se pudo guardar el porcentaje.", true))
            }
        }
    }

    /** Borra las fotos de prueba del teléfono (el caso exige poder eliminarlas al terminar). */
    fun eliminarRespaldosDePrueba() {
        val admin = repo.sesion.usuario.value ?: return
        viewModelScope.launch {
            try {
                val borrados = repo.eliminarRespaldosDePrueba(admin)
                _eventos.trySend(EventoUi("Se eliminaron $borrados respaldos de prueba."))
            } catch (e: ReglaNegocioException) {
                _eventos.trySend(EventoUi(e.message ?: "No se pudo completar la acción.", true))
            }
        }
    }

    /** 0.075 -> "7,5"; 0.10 -> "10". */
    private fun formatear(fraccion: Double): String {
        val porcentaje = Math.round(fraccion * 10_000) / 100.0
        return if (porcentaje % 1.0 == 0.0) porcentaje.toInt().toString() else porcentaje.toString().replace('.', ',')
    }
}
