package com.example.ventasapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ventasapp.model.ModoFormularioPost
import com.example.ventasapp.model.Post
import com.example.ventasapp.model.PostFormErrores
import com.example.ventasapp.model.PostFormUiState
import com.example.ventasapp.model.PostNuevo
import com.example.ventasapp.model.PostUiState
import com.example.ventasapp.model.data.remote.RetrofitInstance
import com.example.ventasapp.model.data.repository.PostRepository
import com.example.ventasapp.repositorio
import com.example.ventasapp.util.ErroresRed
import com.example.ventasapp.util.EventoUi
import com.example.ventasapp.util.Validadores
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

class PostViewModel(app: Application) : AndroidViewModel(app) {
    // Centraliza las operaciones remotas para que la pantalla no dependa directamente de Retrofit.
    private val repository = PostRepository(RetrofitInstance.api)
    private val repo = repositorio

    // Expone el estado de la pantalla como un flujo observable e inmutable para la interfaz.
    private val _estado = MutableStateFlow(PostUiState())
    val estado = _estado.asStateFlow()

    // Mantiene los datos del formulario separados del estado general de la lista.
    private val _formulario = MutableStateFlow(PostFormUiState())
    val formulario = _formulario.asStateFlow()

    // Canaliza avisos de éxito y error para presentarlos mediante mensajes temporales.
    private val canalEventos = Channel<EventoUi>(Channel.BUFFERED)
    val eventos = canalEventos.receiveAsFlow()

    // Evita repetir la carga inicial cuando la pantalla vuelve a mostrarse.
    fun cargarSiCorresponde() {
        if (!_estado.value.cargaIntentada && !_estado.value.cargando) cargarPosts()
    }

    // Ejecuta la consulta GET y actualiza el estado con los resultados o el error obtenido.
    fun cargarPosts() {
        if (_estado.value.cargando) return
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null, cargaIntentada = true) }
            try {
                val posts = repository.obtenerPosts()
                _estado.update { it.copy(posts = posts, cargando = false, error = null) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _estado.update { it.copy(cargando = false, error = mensajeDe(e)) }
                canalEventos.trySend(EventoUi(mensajeDe(e), true))
            }
        }
    }

    // Abre un formulario vacío y asocia la publicación al usuario que inició sesión.
    fun abrirFormularioCrear() {
        val userId = (repo.sesion.usuario.value?.id ?: 1L).coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()
        _formulario.value =
            PostFormUiState(visible = true, modo = ModoFormularioPost.CREAR, userId = userId)
    }

    // Carga los datos actuales para permitir el reemplazo completo mediante PUT.
    fun abrirFormularioReemplazar(post: Post) {
        _formulario.value = PostFormUiState(
            visible = true, modo = ModoFormularioPost.REEMPLAZAR,
            postId = post.id, userId = post.userId, titulo = post.title, cuerpo = post.body
        )
    }

    // Prepara el formulario para modificar únicamente el título mediante PATCH.
    fun abrirFormularioActualizarTitulo(post: Post) {
        _formulario.value = PostFormUiState(
            visible = true, modo = ModoFormularioPost.ACTUALIZAR_TITULO,
            postId = post.id, userId = post.userId, titulo = post.title, cuerpo = post.body
        )
    }

    fun onTituloChange(valor: String) {
        _formulario.update { it.copy(titulo = valor.take(120), errores = it.errores.copy(titulo = null)) }
    }

    fun onCuerpoChange(valor: String) {
        _formulario.update { it.copy(cuerpo = valor.take(2000), errores = it.errores.copy(cuerpo = null)) }
    }

    fun cerrarFormulario() {
        if (!_formulario.value.guardando) _formulario.value = PostFormUiState()
    }

    // Valida los campos antes de enviar la operación correspondiente a la API.
    fun guardarFormulario() {
        val f = _formulario.value
        val errorTitulo = Validadores.tituloPublicacion(f.titulo)
        val errorCuerpo = if (f.modo == ModoFormularioPost.ACTUALIZAR_TITULO) null
            else Validadores.cuerpoPublicacion(f.cuerpo)
        if (errorTitulo != null || errorCuerpo != null) {
            _formulario.update { it.copy(errores = PostFormErrores(errorTitulo, errorCuerpo)) }
            canalEventos.trySend(EventoUi("Revisa los campos del formulario.", true))
            return
        }
        if (f.guardando) return

        viewModelScope.launch {
            _formulario.update { it.copy(guardando = true) }
            try {
                when (f.modo) {
                    // En JSONPlaceholder las escrituras son demostrativas; el resultado se refleja localmente.
                    ModoFormularioPost.CREAR -> {
                        val creado = repository.crearPost(
                            PostNuevo(
                                f.userId,
                                f.titulo.trim(),
                                f.cuerpo.trim()
                            )
                        )
                        // JSONPlaceholder simula POST: no persiste el registro. Se conserva en memoria para mostrarlo.
                        val siguienteIdLocal = maxOf(101, (_estado.value.posts.maxOfOrNull { it.id } ?: 100) + 1)
                        val local = creado.copy(id = siguienteIdLocal)
                        _estado.update { it.copy(posts = listOf(local) + it.posts) }
                        canalEventos.trySend(EventoUi("Publicación creada. La API de pruebas simula el guardado."))
                    }
                    // PUT envía la publicación completa y actualiza el elemento mostrado en pantalla.
                    ModoFormularioPost.REEMPLAZAR -> {
                        val id = f.postId ?: return@launch
                        val actualizado = repository.reemplazarPost(
                            Post(
                                f.userId,
                                id,
                                f.titulo.trim(),
                                f.cuerpo.trim()
                            )
                        )
                        _estado.update { state -> state.copy(posts = state.posts.map { if (it.id == id) actualizado else it }) }
                        canalEventos.trySend(EventoUi("Publicación reemplazada correctamente."))
                    }
                    // PATCH envía solo el título y conserva los demás datos de la publicación.
                    ModoFormularioPost.ACTUALIZAR_TITULO -> {
                        val id = f.postId ?: return@launch
                        val actualizado = repository.actualizarTitulo(id, f.titulo.trim())
                        _estado.update { state -> state.copy(posts = state.posts.map { if (it.id == id) it.copy(title = actualizado.title) else it }) }
                        canalEventos.trySend(EventoUi("Título actualizado correctamente."))
                    }
                }
                _formulario.value = PostFormUiState()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _formulario.update { it.copy(guardando = false) }
                canalEventos.trySend(EventoUi(mensajeDe(e), true))
            }
        }
    }

    // Solicita la eliminación y retira el elemento de la lista cuando la respuesta es satisfactoria.
    fun eliminarPost(id: Int) {
        if (_estado.value.procesandoId != null) return
        viewModelScope.launch {
            _estado.update { it.copy(procesandoId = id, error = null) }
            try {
                if (repository.eliminarPost(id)) {
                    _estado.update { state -> state.copy(posts = state.posts.filterNot { it.id == id }, procesandoId = null) }
                    canalEventos.trySend(EventoUi("Publicación eliminada correctamente."))
                } else {
                    _estado.update { it.copy(procesandoId = null) }
                    canalEventos.trySend(EventoUi("El servidor no confirmó la eliminación.", true))
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _estado.update { it.copy(procesandoId = null) }
                canalEventos.trySend(EventoUi(mensajeDe(e), true))
            }
        }
    }

    // Traduce excepciones HTTP y de conectividad a mensajes comprensibles para el usuario.
    private fun mensajeDe(error: Throwable): String = when (error) {
        is HttpException -> ErroresRed.mensajeParaCodigoHttp(error.code())
        else -> ErroresRed.mensajeDeError(error)
    }
}