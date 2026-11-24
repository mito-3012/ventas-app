package com.example.ventasapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ventasapp.model.RegistroUsuarioErrores
import com.example.ventasapp.model.RegistroUsuarioUiState
import com.example.ventasapp.repositorio
import com.example.ventasapp.util.CorreoDuplicadoException
import com.example.ventasapp.util.ReglaNegocioException
import com.example.ventasapp.util.Validadores
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel del formulario "Crear cuenta de vendedor". Es la evolución directa del
 * UsuarioViewModel de la guía 11: mismo patrón de estado, validación global y guardado en Room.
 */
class RegistroUsuarioViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = repositorio

    private val _estado = MutableStateFlow(RegistroUsuarioUiState())
    val estado: StateFlow<RegistroUsuarioUiState> = _estado

    fun reiniciar() {
        _estado.value = RegistroUsuarioUiState()
    }

    // Cada onXxxChange guarda el valor nuevo y borra el error de ESE campo (el usuario está corrigiendo).
    fun onNombreChange(v: String) = _estado.update { it.copy(nombre = v.take(60), errores = it.errores.copy(nombre = null)) }
    fun onCorreoChange(v: String) = _estado.update { it.copy(correo = v.take(60), errores = it.errores.copy(correo = null)) }
    fun onClaveChange(v: String) = _estado.update { it.copy(clave = v.take(40), errores = it.errores.copy(clave = null)) }
    fun onConfirmarClaveChange(v: String) =
        _estado.update { it.copy(confirmarClave = v.take(40), errores = it.errores.copy(confirmarClave = null)) }
    fun onZonaChange(v: String) = _estado.update { it.copy(zona = v.take(40), errores = it.errores.copy(zona = null)) }
    fun onTerminosChange(v: Boolean) = _estado.update { it.copy(aceptaTerminos = v, errores = it.errores.copy(terminos = null)) }
    fun alternarMostrarClave() = _estado.update { it.copy(mostrarClave = !it.mostrarClave) }

    /** Validación global del formulario (equivalente a validarFormulario() de la guía). */
    fun validarFormulario(): Boolean {
        val e = _estado.value
        val errores = RegistroUsuarioErrores(
            nombre = Validadores.nombrePersona(e.nombre),
            correo = Validadores.correo(e.correo),
            clave = Validadores.clave(e.clave),
            confirmarClave = Validadores.confirmacionClave(e.clave, e.confirmarClave),
            zona = Validadores.obligatorio(e.zona, "Indica tu zona o sucursal"),
            terminos = Validadores.terminosAceptados(e.aceptaTerminos)
        )
        _estado.update { it.copy(errores = errores) }
        return listOfNotNull(
            errores.nombre, errores.correo, errores.clave,
            errores.confirmarClave, errores.zona, errores.terminos
        ).isEmpty()
    }

    /** Valida y guarda la cuenta en Room. [onListo] se ejecuta solo si se guardó correctamente. */
    fun registrar(onListo: () -> Unit) {
        if (_estado.value.cargando || !validarFormulario()) return

        viewModelScope.launch {
            _estado.update { it.copy(cargando = true) }
            val datos = _estado.value
            try {
                repo.registrarVendedor(datos.nombre, datos.correo, datos.clave, datos.zona)
                _estado.update { it.copy(cargando = false) }
                onListo()
            } catch (e: CorreoDuplicadoException) {
                // El error de correo duplicado se pinta en el campo correo, no como mensaje general.
                _estado.update { it.copy(cargando = false, errores = it.errores.copy(correo = e.message)) }
            } catch (e: ReglaNegocioException) {
                _estado.update { it.copy(cargando = false, errores = it.errores.copy(correo = e.message)) }
            } catch (e: Exception) {
                _estado.update {
                    it.copy(cargando = false, errores = it.errores.copy(correo = "No se pudo crear la cuenta. Inténtalo de nuevo."))
                }
            }
        }
    }
}