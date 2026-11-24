package com.example.ventasapp.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ventasapp.model.EstadoVenta
import com.example.ventasapp.model.TipoConvenio
import com.example.ventasapp.model.VentaFormErrores
import com.example.ventasapp.model.VentaFormUiState
import com.example.ventasapp.model.data.local.UsuarioEntity
import com.example.ventasapp.model.data.local.VentaEntity
import com.example.ventasapp.repositorio
import com.example.ventasapp.util.CodigoDuplicadoException
import com.example.ventasapp.util.EventoUi
import com.example.ventasapp.util.Formato
import com.example.ventasapp.util.ImagenUtils
import com.example.ventasapp.util.ReglaNegocioException
import com.example.ventasapp.util.Validadores
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * ViewModel del formulario de venta (crear, editar, corregir).
 *
 * Aquí está TODA la validación del formulario. La pantalla solo pinta el estado y reenvía
 * eventos: no contiene ninguna regla (rúbrica IE 2.2.1).
 *
 * Dos niveles de validación:
 *  - Borrador: exige solo el tipo de convenio y revisa el FORMATO de lo que ya se escribió.
 *  - Envío:    exige todos los campos, la fecha y el respaldo fotográfico.
 */
class VentaFormViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = repositorio

    private val _estado = MutableStateFlow(VentaFormUiState())
    val estado: StateFlow<VentaFormUiState> = _estado

    /** Mensajes de una sola vez (Snackbar). Ver EventoUi. */
    private val _eventos = Channel<EventoUi>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    /**
     * Fotos creadas en ESTE formulario que todavía no están guardadas en una venta. Si el usuario
     * las reemplaza, las quita o abandona el formulario, se borran del teléfono para no dejar
     * archivos huérfanos.
     */
    private val respaldosNuevos = mutableSetOf<String>()

    /** Archivo temporal donde la cámara está escribiendo la foto actual. */
    private var archivoCamaraPendiente: File? = null

    // ------------------------------------------------------------------ Carga inicial

    /** ventaId = 0 -> formulario vacío (venta nueva). Distinto de 0 -> carga la venta para editarla. */
    fun iniciar(ventaId: Long) {
        descartarRespaldosNuevos()   // fotos de un formulario anterior que nunca se guardó
        if (ventaId == 0L) {
            _estado.value = VentaFormUiState()
            return
        }
        val usuario = repo.sesion.usuario.value ?: return
        viewModelScope.launch {
            _estado.value = VentaFormUiState(cargando = true)
            val venta = repo.obtenerVentaAutorizada(ventaId, usuario)
            // Si no existe, es de otro vendedor o ya no es editable, se bloquea el formulario.
            if (venta == null || !EstadoVenta.desde(venta.estado).esEditablePorVendedor) {
                _estado.value = VentaFormUiState(noAutorizado = true)
            } else {
                _estado.value = venta.aEstadoFormulario()
            }
        }
    }

    private fun VentaEntity.aEstadoFormulario(): VentaFormUiState {
        val estadoVenta = EstadoVenta.desde(estado)
        return VentaFormUiState(
            ventaId = id,
            estadoOriginal = estadoVenta,
            observacionPrevia = if (estadoVenta == EstadoVenta.OBSERVADA) observacion else null,
            tipoConvenio = TipoConvenio.desde(tipoConvenio),
            prestador = prestador,
            fechaOperacion = fechaOperacion,
            monto = if (monto > 0) monto.toString() else "",
            codigo = codigoAutorizacion,
            clienteNombre = clienteNombre,
            clienteRut = clienteRut,
            clienteCorreo = clienteCorreo,
            clienteTelefono = clienteTelefono,
            mascotaNombre = mascotaNombre,
            respaldoPath = respaldoPath
        )
    }

    // ------------------------------------------------------------------ Eventos de los campos
    // Cada función guarda el valor (ya "limpio") y borra el error de ese campo.

    fun onTipoChange(tipo: TipoConvenio) = _estado.update {
        it.copy(
            tipoConvenio = tipo,
            prestador = tipo.prestadores.first(),          // se sugiere el primer prestador del convenio
            mascotaNombre = if (tipo.requiereMascota) it.mascotaNombre else "",
            errores = it.errores.copy(tipoConvenio = null, prestador = null, mascota = null)
        )
    }

    fun onPrestadorChange(valor: String) =
        _estado.update { it.copy(prestador = valor, errores = it.errores.copy(prestador = null)) }

    fun onFechaChange(millis: Long) =
        _estado.update { it.copy(fechaOperacion = millis, errores = it.errores.copy(fecha = null)) }

    /** Solo dígitos: así es imposible escribir signos, puntos o letras en el monto. */
    fun onMontoChange(valor: String) =
        _estado.update { it.copy(monto = valor.filter(Char::isDigit).take(8), errores = it.errores.copy(monto = null)) }

    /** Fuerza mayúsculas y descarta cualquier carácter que no sea A-Z o 0-9. */
    fun onCodigoChange(valor: String) {
        val limpio = valor.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }.take(12)
        _estado.update { it.copy(codigo = limpio, errores = it.errores.copy(codigo = null)) }
    }

    fun onClienteNombreChange(valor: String) =
        _estado.update { it.copy(clienteNombre = valor.take(60), errores = it.errores.copy(clienteNombre = null)) }

    fun onClienteRutChange(valor: String) {
        val limpio = valor.filter { it.isDigit() || it == '.' || it == '-' || it == 'k' || it == 'K' }.take(12)
        _estado.update { it.copy(clienteRut = limpio, errores = it.errores.copy(clienteRut = null)) }
    }

    fun onClienteCorreoChange(valor: String) =
        _estado.update { it.copy(clienteCorreo = valor.replace(" ", "").take(60), errores = it.errores.copy(clienteCorreo = null)) }

    fun onClienteTelefonoChange(valor: String) {
        val limpio = valor.filter { it.isDigit() || it == '+' }.take(12)
        _estado.update { it.copy(clienteTelefono = limpio, errores = it.errores.copy(clienteTelefono = null)) }
    }

    fun onMascotaChange(valor: String) =
        _estado.update { it.copy(mascotaNombre = valor.take(30), errores = it.errores.copy(mascota = null)) }

    // ------------------------------------------------------------------ Recursos nativos: cámara y galería

    /** Crea el archivo temporal y entrega la URI segura que la cámara usará para guardar la foto. */
    fun prepararFotoCamara(): Uri {
        val contexto = getApplication<Application>()
        val archivo = ImagenUtils.crearArchivoCamara(contexto)
        archivoCamaraPendiente = archivo
        return ImagenUtils.uriSegura(contexto, archivo)
    }

    /** La cámara terminó. [exito] es false si el usuario canceló. */
    fun onFotoCapturada(exito: Boolean) {
        val archivo = archivoCamaraPendiente ?: return
        archivoCamaraPendiente = null
        if (!exito) {
            archivo.delete()
            return
        }
        procesarImagen { ImagenUtils.procesarDesdeArchivo(getApplication<Application>(), archivo) }
    }

    /** El selector de galería terminó. [uri] es null si el usuario no eligió nada. */
    fun onImagenGaleria(uri: Uri?) {
        if (uri == null) return
        procesarImagen { ImagenUtils.procesarDesdeUri(getApplication<Application>(), uri) }
    }

    fun avisarPermisoCamaraDenegado() = emitir(
        "Sin el permiso de cámara no se puede tomar la foto. Puedes elegir una imagen desde la galería.", true
    )

    fun avisarCamaraNoDisponible() = emitir(
        "No se encontró una aplicación de cámara en este dispositivo. Usa la galería.", true
    )

    fun avisarPermisoGaleriaDenegado() = emitir(
        "Sin el permiso de fotos no se puede abrir la galería. Puedes activarlo en Ajustes > Aplicaciones " +
            "> Gestión de ventas > Permisos, o tomar la foto con la cámara.", true
    )

    /** Quita la foto del formulario. Si era una foto nueva (no guardada), también se borra del teléfono. */
    fun quitarRespaldo() {
        val anterior = _estado.value.respaldoPath
        _estado.update { it.copy(respaldoPath = null) }
        descartarSiEsNueva(anterior)
    }

    /** Borra del teléfono una foto creada en este formulario (si lo es). Las ya guardadas no se tocan aquí. */
    private fun descartarSiEsNueva(ruta: String?) {
        if (ruta != null && respaldosNuevos.remove(ruta)) {
            viewModelScope.launch(Dispatchers.IO) {
                ImagenUtils.eliminarRespaldo(getApplication<Application>(), ruta)
            }
        }
    }

    private fun descartarRespaldosNuevos() {
        val pendientes = respaldosNuevos.toList()
        respaldosNuevos.clear()
        if (pendientes.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                pendientes.forEach { ImagenUtils.eliminarRespaldo(getApplication<Application>(), it) }
            }
        }
    }

    /** Procesa la imagen (decodificar, comprimir, copiar) en un hilo de IO para no trabar la pantalla. */
    private fun procesarImagen(tarea: () -> String) {
        viewModelScope.launch {
            _estado.update { it.copy(procesandoImagen = true, errores = it.errores.copy(respaldo = null)) }
            try {
                val ruta = withContext(Dispatchers.IO) { tarea() }
                val anterior = _estado.value.respaldoPath
                _estado.update { it.copy(respaldoPath = ruta, procesandoImagen = false) }
                respaldosNuevos.add(ruta)
                descartarSiEsNueva(anterior)   // la foto reemplazada ya no sirve
            } catch (e: ReglaNegocioException) {
                _estado.update { it.copy(procesandoImagen = false, errores = it.errores.copy(respaldo = e.message)) }
                emitir(e.message ?: "No se pudo procesar la imagen.", true)
            } catch (e: Exception) {
                _estado.update {
                    it.copy(procesandoImagen = false, errores = it.errores.copy(respaldo = "No se pudo procesar la imagen."))
                }
            }
        }
    }

    // ------------------------------------------------------------------ Validación

    /**
     * Valida el formulario y deja los mensajes en el estado. Devuelve true si no hay errores.
     * @param completo true = validación de envío (todo obligatorio); false = validación de borrador.
     */
    private fun validarFormulario(completo: Boolean): Boolean {
        val e = _estado.value

        // Si es borrador, un campo vacío se acepta, pero si trae texto debe tener buen formato.
        fun opcional(valor: String, validador: (String) -> String?): String? =
            if (completo || valor.isNotBlank()) validador(valor) else null

        val errores = VentaFormErrores(
            tipoConvenio = Validadores.seleccion(e.tipoConvenio, "Selecciona el tipo de convenio"),
            prestador = if (completo) Validadores.obligatorio(
                e.prestador,
                "Selecciona un prestador"
            ) else null,
            fecha = if (completo || e.fechaOperacion != null) {
                Validadores.fechaOperacion(e.fechaOperacion, Formato.hoyComoFechaUtc())
            } else null,
            monto = opcional(e.monto, Validadores::monto),
            codigo = opcional(e.codigo, Validadores::codigoAutorizacion),
            clienteNombre = opcional(e.clienteNombre, Validadores::nombrePersona),
            clienteRut = opcional(e.clienteRut, Validadores::rut),
            clienteCorreo = opcional(e.clienteCorreo, Validadores::correo),
            clienteTelefono = opcional(e.clienteTelefono, Validadores::telefono),
            mascota = if (e.tipoConvenio?.requiereMascota == true) {
                opcional(e.mascotaNombre, Validadores::nombrePersona)
            } else null,
            respaldo = if (completo) {
                Validadores.seleccion(e.respaldoPath, "Adjunta una foto del comprobante (ficticio)")
            } else null
        )
        _estado.update { it.copy(errores = errores) }
        return !errores.hayErrores
    }

    // ------------------------------------------------------------------ Guardado

    fun guardarBorrador(onListo: () -> Unit) = ejecutarGuardado(enviar = false, onListo = onListo)

    fun enviarARevision(onListo: () -> Unit) = ejecutarGuardado(enviar = true, onListo = onListo)

    private fun ejecutarGuardado(enviar: Boolean, onListo: () -> Unit) {
        val usuario = repo.sesion.usuario.value ?: return
        val actual = _estado.value
        if (actual.guardando || actual.procesandoImagen) return

        if (!validarFormulario(completo = enviar)) {
            emitir("Revisa los campos marcados en rojo.", true)
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(guardando = true) }
            try {
                val entidad = construirEntidad(usuario)
                if (enviar) repo.enviarVenta(entidad, usuario) else repo.guardarCambios(entidad, usuario)
                _estado.update { it.copy(guardando = false) }
                respaldosNuevos.clear()   // la foto quedó guardada en la venta: ya no es "nueva"
                emitir(if (enviar) "Venta enviada a revisión." else "Borrador guardado correctamente.", false)
                onListo()
            } catch (e: CodigoDuplicadoException) {
                // El duplicado se pinta directamente en el campo del código.
                _estado.update { it.copy(guardando = false, errores = it.errores.copy(codigo = e.message)) }
                emitir("Código de autorización duplicado.", true)
            } catch (e: ReglaNegocioException) {
                _estado.update { it.copy(guardando = false) }
                emitir(e.message ?: "No se pudo guardar.", true)
            } catch (e: Exception) {
                _estado.update { it.copy(guardando = false) }
                emitir("Ocurrió un error inesperado al guardar.", true)
            }
        }
    }

    /** Convierte lo que se escribió (UiState) en lo que se guarda (Entity). */
    private fun construirEntidad(usuario: UsuarioEntity): VentaEntity {
        val e = _estado.value
        return VentaEntity(
            id = e.ventaId,
            vendedorId = usuario.id,
            vendedorNombre = usuario.nombre,
            tipoConvenio = e.tipoConvenio?.name ?: "",
            prestador = e.prestador,
            fechaOperacion = e.fechaOperacion,
            monto = e.monto.toIntOrNull() ?: 0,
            codigoAutorizacion = e.codigo,
            clienteNombre = e.clienteNombre.trim(),
            clienteRut = if (e.clienteRut.isBlank()) "" else Validadores.formatearRut(e.clienteRut),
            clienteCorreo = e.clienteCorreo.trim().lowercase(),
            clienteTelefono = e.clienteTelefono,
            mascotaNombre = e.mascotaNombre.trim(),
            respaldoPath = e.respaldoPath,
            estado = e.estadoOriginal.name
        )
    }

    private fun emitir(texto: String, esError: Boolean) {
        _eventos.trySend(EventoUi(texto, esError))
    }
}