package com.example.ventasapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ventasapp.model.LoginErrores
import com.example.ventasapp.model.LoginUiState
import com.example.ventasapp.model.Perfil
import com.example.ventasapp.model.data.local.UsuarioEntity
import com.example.ventasapp.repositorio
import com.example.ventasapp.util.ReglaNegocioException
import com.example.ventasapp.util.Validadores
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel de la sesión: maneja el formulario de login y expone quién está conectado.
 *
 * Mismo esquema de la guía 11:
 *  - _login (privado, mutable)  ->  login (público, solo lectura)
 *  - funciones onXxxChange que actualizan el estado con copy() y limpian el error del campo
 *  - validación en el ViewModel, no en la pantalla
 */
class LoginViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = repositorio

    /** Usuario con sesión iniciada (null si no hay). Lo leen la navegación y las pantallas. */
    val usuario: StateFlow<UsuarioEntity?> = repo.sesion.usuario

    private val _login = MutableStateFlow(LoginUiState())
    val login: StateFlow<LoginUiState> = _login

    init {
        // Crea cuentas y ventas de demostración la primera vez que se abre la app.
        viewModelScope.launch {
            repo.sembrarDatosDemoSiCorresponde()
            repo.eliminarRespaldosHuerfanos()   // borra fotos que quedaron sin venta (formularios descartados)
        }
    }

    fun onCorreoChange(valor: String) {
        _login.update {
            it.copy(correo = valor.take(60), errores = it.errores.copy(correo = null, general = null))
        }
    }

    fun onClaveChange(valor: String) {
        _login.update {
            it.copy(clave = valor.take(40), errores = it.errores.copy(clave = null, general = null))
        }
    }

    fun alternarMostrarClave() {
        _login.update { it.copy(mostrarClave = !it.mostrarClave) }
    }

    /** Atajo para la demostración: rellena las credenciales de una cuenta de prueba. */
    fun autocompletar(correo: String, clave: String) {
        _login.update { it.copy(correo = correo, clave = clave, errores = LoginErrores()) }
    }

    /** Valida el formulario y deja los mensajes en el estado. Devuelve true si todo está bien. */
    private fun validarFormulario(): Boolean {
        val actual = _login.value
        val errores = LoginErrores(
            correo = Validadores.correo(actual.correo),
            clave = Validadores.obligatorio(actual.clave, "Introduce tu contraseña")
        )
        _login.update { it.copy(errores = errores) }
        return errores.correo == null && errores.clave == null
    }

    fun iniciarSesion(onExito: (Perfil) -> Unit) {
        if (_login.value.cargando || !validarFormulario()) return

        viewModelScope.launch {
            _login.update { it.copy(cargando = true, errores = it.errores.copy(general = null)) }
            try {
                repo.sembrarDatosDemoSiCorresponde()
                val datos = _login.value
                val usuarioValidado = repo.iniciarSesion(datos.correo, datos.clave)
                repo.sesion.iniciar(usuarioValidado)
                _login.value = LoginUiState()   // se borra la clave de la memoria
                onExito(Perfil.desde(usuarioValidado.perfil))
            } catch (e: ReglaNegocioException) {
                _login.update { it.copy(cargando = false, errores = it.errores.copy(general = e.message)) }
            } catch (e: Exception) {
                _login.update {
                    it.copy(cargando = false, errores = it.errores.copy(general = "Ocurrió un error inesperado. Inténtalo de nuevo."))
                }
            }
        }
    }

    fun cerrarSesion() {
        repo.sesion.cerrar()
        _login.value = LoginUiState()
    }
}