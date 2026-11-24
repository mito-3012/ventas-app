package com.example.ventasapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ventasapp.model.NuevoUsuarioErrores
import com.example.ventasapp.model.NuevoUsuarioUiState
import com.example.ventasapp.model.Perfil
import com.example.ventasapp.model.data.local.UsuarioEntity
import com.example.ventasapp.repositorio
import com.example.ventasapp.util.CorreoDuplicadoException
import com.example.ventasapp.util.EventoUi
import com.example.ventasapp.util.ReglaNegocioException
import com.example.ventasapp.util.Validadores
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel de la administración de usuarios (perfil ADMINISTRADOR): crear cuentas con un perfil,
 * activarlas o desactivarlas y asignar perfiles. Los permisos se vuelven a validar en el
 * repositorio, así que aunque alguien abriera esta pantalla sin ser administrador, no podría operar.
 */
class UsuariosViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = repositorio

    val usuarios: StateFlow<List<UsuarioEntity>> = repo.usuarios()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _formulario = MutableStateFlow(NuevoUsuarioUiState())
    val formulario: StateFlow<NuevoUsuarioUiState> = _formulario

    private val _eventos = Channel<EventoUi>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    // ------------------------------------------------------------------ Formulario "Nuevo usuario"

    fun abrirFormulario() {
        _formulario.value = NuevoUsuarioUiState(visible = true)
    }

    fun cerrarFormulario() {
        _formulario.value = NuevoUsuarioUiState()
    }

    fun onNombreChange(v: String) = _formulario.update { it.copy(nombre = v.take(60), errores = it.errores.copy(nombre = null)) }
    fun onCorreoChange(v: String) = _formulario.update { it.copy(correo = v.take(60), errores = it.errores.copy(correo = null)) }
    fun onZonaChange(v: String) = _formulario.update { it.copy(zona = v.take(40), errores = it.errores.copy(zona = null)) }
    fun onClaveChange(v: String) = _formulario.update { it.copy(clave = v.take(40), errores = it.errores.copy(clave = null)) }
    fun onPerfilChange(perfil: Perfil) = _formulario.update { it.copy(perfil = perfil) }
    fun alternarMostrarClave() = _formulario.update { it.copy(mostrarClave = !it.mostrarClave) }

    /** Validación global del formulario; deja los mensajes en el estado. */
    private fun validarFormulario(): Boolean {
        val f = _formulario.value
        val errores = NuevoUsuarioErrores(
            nombre = Validadores.nombrePersona(f.nombre),
            correo = Validadores.correo(f.correo),
            zona = Validadores.obligatorio(f.zona, "Indica la zona o sucursal"),
            clave = Validadores.clave(f.clave)
        )
        _formulario.update { it.copy(errores = errores) }
        return listOfNotNull(errores.nombre, errores.correo, errores.zona, errores.clave).isEmpty()
    }

    fun crearUsuario() {
        val admin = repo.sesion.usuario.value ?: return
        if (_formulario.value.cargando || !validarFormulario()) return

        viewModelScope.launch {
            _formulario.update { it.copy(cargando = true) }
            val f = _formulario.value
            try {
                repo.crearUsuario(f.nombre, f.correo, f.clave, f.perfil, f.zona, admin)
                _formulario.value = NuevoUsuarioUiState()   // cierra el diálogo y limpia la clave
                _eventos.trySend(EventoUi("Usuario creado: ${f.nombre.trim()} (${f.perfil.etiqueta})."))
            } catch (e: CorreoDuplicadoException) {
                _formulario.update { it.copy(cargando = false, errores = it.errores.copy(correo = e.message)) }
            } catch (e: ReglaNegocioException) {
                _formulario.update { it.copy(cargando = false) }
                _eventos.trySend(EventoUi(e.message ?: "No se pudo crear el usuario.", true))
            } catch (e: Exception) {
                _formulario.update { it.copy(cargando = false) }
                _eventos.trySend(EventoUi("Ocurrió un error inesperado al crear el usuario.", true))
            }
        }
    }

    // ------------------------------------------------------------------ Acciones sobre usuarios existentes

    fun cambiarActivo(usuarioId: Long, activo: Boolean) {
        val admin = repo.sesion.usuario.value ?: return
        viewModelScope.launch {
            try {
                repo.cambiarEstadoUsuario(usuarioId, activo, admin)
                _eventos.trySend(EventoUi(if (activo) "Usuario activado." else "Usuario desactivado."))
            } catch (e: ReglaNegocioException) {
                _eventos.trySend(EventoUi(e.message ?: "No se pudo cambiar el estado.", true))
            }
        }
    }

    fun cambiarPerfil(usuarioId: Long, perfil: Perfil) {
        val admin = repo.sesion.usuario.value ?: return
        viewModelScope.launch {
            try {
                repo.cambiarPerfilUsuario(usuarioId, perfil, admin)
                _eventos.trySend(EventoUi("Perfil actualizado a ${perfil.etiqueta}."))
            } catch (e: ReglaNegocioException) {
                _eventos.trySend(EventoUi(e.message ?: "No se pudo cambiar el perfil.", true))
            }
        }
    }
}