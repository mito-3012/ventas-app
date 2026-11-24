package com.example.ventasapp.model.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.ventasapp.model.AccionRevision
import com.example.ventasapp.model.EstadoVenta
import com.example.ventasapp.model.Perfil
import com.example.ventasapp.model.TipoConvenio
import com.example.ventasapp.model.data.local.ComisionEntity
import com.example.ventasapp.model.data.local.HistorialEntity
import com.example.ventasapp.model.data.local.NotificacionEntity
import com.example.ventasapp.model.data.local.UsuarioEntity
import com.example.ventasapp.model.data.local.VentaEntity
import com.example.ventasapp.model.data.local.VentasDatabase
import com.example.ventasapp.util.CodigoDuplicadoException
import com.example.ventasapp.util.CorreoDuplicadoException
import com.example.ventasapp.util.Formato
import com.example.ventasapp.util.ImagenUtils
import com.example.ventasapp.util.ReglaNegocioException
import com.example.ventasapp.util.Seguridad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Repositorio: único lugar que habla con Room y que aplica las REGLAS DE NEGOCIO del caso.
 *
 * Por qué existe (la guía menciona que en proyectos más grandes "puede interponerse un Repository"):
 *  - Los ViewModels no conocen los DAOs ni el SQL.
 *  - Las reglas críticas se aplican acá y no solo en la pantalla. Así, aunque alguien modificara la
 *    UI, no se podría validar una venta propia ni editar una venta ya validada.
 *  - Cada cambio de estado deja historial y alerta dentro de UNA transacción: o se guarda todo
 *    junto o no se guarda nada (nunca queda una venta cambiada sin su registro).
 */
class VentasRepository(
    private val db: VentasDatabase,
    val sesion: SesionManager,
    private val contexto: Context
) {
    private val usuarioDao = db.usuarioDao()
    private val ventaDao = db.ventaDao()
    private val historialDao = db.historialDao()
    private val notificacionDao = db.notificacionDao()
    private val comisionDao = db.comisionDao()

    /** Evita que dos corrutinas siembren los datos de demostración al mismo tiempo. */
    private val candadoSiembra = Mutex()

    // =========================================================================
    // AUTENTICACIÓN
    // =========================================================================

    suspend fun iniciarSesion(correo: String, clave: String): UsuarioEntity {
        val mensajeGenerico = "Correo o contraseña incorrectos."
        val usuario = usuarioDao.buscarPorCorreo(correo.trim().lowercase())
            ?: throw ReglaNegocioException(mensajeGenerico)

        // El hash PBKDF2 es costoso a propósito: se calcula en Default para no trabar la interfaz.
        val claveCorrecta = withContext(Dispatchers.Default) {
            Seguridad.verificar(clave, usuario.sal, usuario.claveHash)
        }
        if (!claveCorrecta) throw ReglaNegocioException(mensajeGenerico)
        if (!usuario.activo) throw ReglaNegocioException("La cuenta está desactivada. Contacta al administrador.")
        return usuario
    }

    /** Crea una cuenta de VENDEDOR. Los encargados no se pueden autoasignar desde la app. */
    suspend fun registrarVendedor(nombre: String, correo: String, clave: String, zona: String): Long {
        val correoNormalizado = correo.trim().lowercase()
        if (usuarioDao.buscarPorCorreo(correoNormalizado) != null) throw CorreoDuplicadoException()
        val usuario = construirUsuario(nombre.trim(), correoNormalizado, clave, Perfil.VENDEDOR, zona.trim())
        return usuarioDao.insertar(usuario)
    }

    private suspend fun construirUsuario(
        nombre: String, correo: String, clave: String, perfil: Perfil, zona: String
    ): UsuarioEntity = withContext(Dispatchers.Default) {
        val sal = Seguridad.generarSal()
        UsuarioEntity(
            nombre = nombre,
            correo = correo,
            claveHash = Seguridad.hashClave(clave, sal),
            sal = sal,
            perfil = perfil.name,
            zona = zona
        )
    }

    // =========================================================================
    // CONSULTAS (devuelven Flow: la UI se actualiza sola)
    // =========================================================================

    fun ventasDeVendedor(vendedorId: Long): Flow<List<VentaEntity>> = ventaDao.observarDeVendedor(vendedorId)

    fun ventasRecibidas(): Flow<List<VentaEntity>> = ventaDao.observarRecibidas()

    fun vendedores(): Flow<List<UsuarioEntity>> = usuarioDao.observarVendedores()

    /** Lista de usuarios para la administración. */
    fun usuarios(): Flow<List<UsuarioEntity>> = usuarioDao.observarTodos()

    /**
     * Porcentajes de comisión vigentes por tipo de convenio. Si aún no hay un valor guardado para
     * un convenio, usa el predeterminado, así nunca falta una entrada en el mapa.
     */
    fun comisiones(): Flow<Map<TipoConvenio, Double>> = comisionDao.observarTodas().map { filas ->
        TipoConvenio.entries.associateWith { tipo ->
            filas.firstOrNull { it.tipoConvenio == tipo.name }?.porcentaje ?: tipo.porcentajeComision
        }
    }

    fun historial(ventaId: Long): Flow<List<HistorialEntity>> = historialDao.observarDeVenta(ventaId)

    /**
     * Observa una venta SOLO si el usuario tiene permiso para verla; si no, emite null.
     * Así un vendedor no puede abrir ventas ajenas ni siquiera forzando el id.
     */
    fun observarVentaAutorizada(ventaId: Long, usuario: UsuarioEntity?): Flow<VentaEntity?> {
        if (usuario == null) return flowOf(null)
        return ventaDao.observarPorId(ventaId).map { venta -> venta?.takeIf { puedeVer(it, usuario) } }
    }

    suspend fun obtenerVentaAutorizada(ventaId: Long, usuario: UsuarioEntity): VentaEntity? =
        ventaDao.obtenerPorId(ventaId)?.takeIf { puedeVer(it, usuario) }

    private fun puedeVer(venta: VentaEntity, usuario: UsuarioEntity): Boolean =
        when (Perfil.desde(usuario.perfil)) {
            Perfil.VENDEDOR -> venta.vendedorId == usuario.id
            Perfil.ENCARGADO -> venta.estado != EstadoVenta.BORRADOR.name
            Perfil.ADMINISTRADOR -> false      // administra cuentas y comisiones, no ve ventas
        }

    // =========================================================================
    // COMANDOS DEL VENDEDOR
    // =========================================================================

    /**
     * Guarda una venta sin enviarla (crea un borrador o actualiza una venta editable).
     *
     * IMPORTANTE: los campos "sensibles" (dueño, estado, datos de revisión) se toman de lo que ya
     * está guardado y NO de lo que llega desde la pantalla. Así el formulario nunca puede cambiar
     * el estado de una venta por su cuenta.
     */
    suspend fun guardarCambios(venta: VentaEntity, usuario: UsuarioEntity): Long {
        exigirPerfil(usuario, Perfil.VENDEDOR, "Solo un vendedor puede registrar ventas.")
        return db.withTransaction {
            val ahora = System.currentTimeMillis()
            verificarCodigoNoDuplicado(venta)

            if (venta.id == 0L) {
                val nueva = venta.copy(
                    vendedorId = usuario.id,
                    vendedorNombre = usuario.nombre,
                    estado = EstadoVenta.BORRADOR.name,
                    fechaActualizacion = ahora
                )
                val id = ventaDao.insertar(nueva)
                registrarHistorial(id, null, EstadoVenta.BORRADOR, "Borrador creado", usuario, null)
                id
            } else {
                val existente = obtenerEditable(venta.id, usuario)
                ventaDao.actualizar(
                    venta.copy(
                        vendedorId = existente.vendedorId,
                        vendedorNombre = existente.vendedorNombre,
                        estado = existente.estado,
                        observacion = existente.observacion,
                        motivoRechazo = existente.motivoRechazo,
                        fechaEnvio = existente.fechaEnvio,
                        fechaRevision = existente.fechaRevision,
                        revisorId = existente.revisorId,
                        revisorNombre = existente.revisorNombre,
                        fechaActualizacion = ahora
                    )
                )
                val estado = EstadoVenta.desde(existente.estado)
                registrarHistorial(venta.id, estado, estado, "Cambios guardados", usuario, null)
                venta.id
            }
        }
    }

    /** Guarda y envía la venta a revisión. Si estaba observada, pasa a CORREGIDA. */
    suspend fun enviarVenta(venta: VentaEntity, usuario: UsuarioEntity): Long {
        exigirPerfil(usuario, Perfil.VENDEDOR, "Solo un vendedor puede enviar ventas.")
        if (venta.respaldoPath.isNullOrBlank()) throw ReglaNegocioException("Debes adjuntar un respaldo antes de enviar.")
        if (venta.monto <= 0) throw ReglaNegocioException("El monto de la venta no es válido.")

        // 1) Se guardan los datos (también valida dueño, estado editable y código duplicado).
        val id = guardarCambios(venta, usuario)

        // 2) Se cambia el estado y se avisa al encargado.
        return db.withTransaction {
            val existente = ventaDao.obtenerPorId(id) ?: throw ReglaNegocioException("La venta no existe.")
            val estadoAnterior = EstadoVenta.desde(existente.estado)
            val esCorreccion = estadoAnterior == EstadoVenta.OBSERVADA
            val nuevoEstado = if (esCorreccion) EstadoVenta.CORREGIDA else EstadoVenta.PENDIENTE_REVISION
            val ahora = System.currentTimeMillis()

            ventaDao.actualizar(existente.copy(estado = nuevoEstado.name, fechaEnvio = ahora, fechaActualizacion = ahora))
            registrarHistorial(
                id, estadoAnterior, nuevoEstado,
                if (esCorreccion) "Corrección enviada a revisión" else "Venta enviada a revisión",
                usuario, null
            )
            notificar(
                destinoId = null, perfil = Perfil.ENCARGADO, ventaId = id,
                mensaje = (if (esCorreccion) "Corrección enviada: " else "Nueva venta recibida: ") +
                    "${existente.codigoAutorizacion} (${usuario.nombre})"
            )
            id
        }
    }

    /**
     * Elimina una venta. Qué estados se pueden eliminar lo define EstadoVenta.puedeEliminar, la
     * misma regla que usa la interfaz para mostrar el botón.
     *
     * Además de la fila de la venta se borran su historial y la FOTO del respaldo guardada en el
     * teléfono (así no quedan archivos huérfanos ocupando espacio).
     */
    suspend fun eliminarVenta(ventaId: Long, usuario: UsuarioEntity) {
        val perfil = Perfil.desde(usuario.perfil)

        val eliminada = db.withTransaction {
            val venta = ventaDao.obtenerPorId(ventaId) ?: throw ReglaNegocioException("La venta no existe.")
            val estado = EstadoVenta.desde(venta.estado)
            val codigo = venta.codigoAutorizacion.ifBlank { "sin código" }

            if (perfil == Perfil.VENDEDOR && venta.vendedorId != usuario.id) {
                throw ReglaNegocioException("No puedes eliminar ventas de otros vendedores.")
            }
            if (!estado.puedeEliminar(perfil)) {
                throw ReglaNegocioException(
                    when (perfil) {
                        Perfil.VENDEDOR -> "No se puede eliminar una venta en estado \"${estado.etiqueta}\". " +
                            "Solo se eliminan borradores, ventas observadas o rechazadas."
                        Perfil.ENCARGADO -> "El encargado solo puede eliminar ventas rechazadas."
                        else -> "Tu perfil no tiene permiso para eliminar ventas."
                    }
                )
            }

            ventaDao.eliminar(ventaId)
            historialDao.eliminarDeVenta(ventaId)

            // Se avisa a la otra parte. Un borrador nunca fue visto por el encargado: no se avisa.
            when (perfil) {
                Perfil.VENDEDOR ->
                    if (estado != EstadoVenta.BORRADOR) {
                        notificar(null, Perfil.ENCARGADO, ventaId, "El vendedor ${usuario.nombre} eliminó la venta $codigo.")
                    }
                Perfil.ENCARGADO ->
                    notificar(venta.vendedorId, Perfil.VENDEDOR, ventaId, "El encargado eliminó tu venta rechazada $codigo.")
                else -> Unit
            }
            venta
        }

        // La foto se borra DESPUÉS de confirmar la transacción: si algo falla antes, no se pierde la imagen.
        withContext(Dispatchers.IO) { ImagenUtils.eliminarRespaldo(contexto, eliminada.respaldoPath) }
    }

    private suspend fun obtenerEditable(ventaId: Long, usuario: UsuarioEntity): VentaEntity {
        val existente = ventaDao.obtenerPorId(ventaId) ?: throw ReglaNegocioException("La venta no existe.")
        if (existente.vendedorId != usuario.id) throw ReglaNegocioException("No puedes modificar ventas de otros vendedores.")
        val estado = EstadoVenta.desde(existente.estado)
        if (!estado.esEditablePorVendedor) {
            throw ReglaNegocioException(
                "La venta está en estado \"${estado.etiqueta}\" y ya no se puede modificar."
            )
        }
        return existente
    }

    private suspend fun verificarCodigoNoDuplicado(venta: VentaEntity) {
        if (venta.codigoAutorizacion.isNotBlank() &&
            ventaDao.contarCodigo(venta.codigoAutorizacion, venta.id) > 0
        ) {
            throw CodigoDuplicadoException()
        }
    }

    // =========================================================================
    // COMANDOS DEL ADMINISTRADOR
    // =========================================================================

    /** Crea una cuenta con el perfil que elija el administrador. */
    suspend fun crearUsuario(
        nombre: String, correo: String, clave: String, perfil: Perfil, zona: String, admin: UsuarioEntity
    ): Long {
        exigirPerfil(admin, Perfil.ADMINISTRADOR, "Solo el administrador puede crear usuarios.")
        val correoNormalizado = correo.trim().lowercase()
        if (usuarioDao.buscarPorCorreo(correoNormalizado) != null) throw CorreoDuplicadoException()
        return usuarioDao.insertar(construirUsuario(nombre.trim(), correoNormalizado, clave, perfil, zona.trim()))
    }

    /** Activa o desactiva una cuenta. Una cuenta desactivada no puede iniciar sesión. */
    suspend fun cambiarEstadoUsuario(usuarioId: Long, activo: Boolean, admin: UsuarioEntity) {
        exigirPerfil(admin, Perfil.ADMINISTRADOR, "Solo el administrador puede activar o desactivar usuarios.")
        if (usuarioId == admin.id) throw ReglaNegocioException("No puedes desactivar tu propia cuenta.")
        val usuario = usuarioDao.buscarPorId(usuarioId) ?: throw ReglaNegocioException("El usuario no existe.")
        usuarioDao.actualizar(usuario.copy(activo = activo))
    }

    /** Asigna otro perfil a un usuario existente. */
    suspend fun cambiarPerfilUsuario(usuarioId: Long, perfil: Perfil, admin: UsuarioEntity) {
        exigirPerfil(admin, Perfil.ADMINISTRADOR, "Solo el administrador puede asignar perfiles.")
        if (usuarioId == admin.id) throw ReglaNegocioException("No puedes cambiar tu propio perfil.")
        val usuario = usuarioDao.buscarPorId(usuarioId) ?: throw ReglaNegocioException("El usuario no existe.")
        usuarioDao.actualizar(usuario.copy(perfil = perfil.name))
    }

    /** Guarda el porcentaje (como fracción: 0.10 = 10%) de comisión ficticia de un convenio. */
    suspend fun actualizarComision(tipo: TipoConvenio, porcentaje: Double, admin: UsuarioEntity) {
        exigirPerfil(admin, Perfil.ADMINISTRADOR, "Solo el administrador puede configurar las comisiones.")
        if (porcentaje < 0.0 || porcentaje > 0.30) throw ReglaNegocioException("El porcentaje debe estar entre 0% y 30%.")
        comisionDao.guardar(ComisionEntity(tipo.name, porcentaje))
    }

    // =========================================================================
    // COMANDOS DEL ENCARGADO
    // =========================================================================

    /** Valida, observa o rechaza una venta. Aplica TODAS las reglas de control de acceso del caso. */
    suspend fun revisar(ventaId: Long, accion: AccionRevision, observacion: String, usuario: UsuarioEntity) {
        exigirPerfil(usuario, Perfil.ENCARGADO, "Solo el encargado de gestión puede revisar ventas.")
        val texto = observacion.trim()
        if (accion.requiereObservacion && texto.isEmpty()) {
            throw ReglaNegocioException("Debes escribir una observación para esta acción.")
        }

        db.withTransaction {
            val venta = ventaDao.obtenerPorId(ventaId) ?: throw ReglaNegocioException("La venta no existe.")
            if (venta.vendedorId == usuario.id) throw ReglaNegocioException("No puedes validar tus propias ventas.")
            val estadoActual = EstadoVenta.desde(venta.estado)
            if (!estadoActual.esRevisablePorEncargado) {
                throw ReglaNegocioException("Esta venta no está pendiente de revisión (estado actual: ${estadoActual.etiqueta}).")
            }

            val ahora = System.currentTimeMillis()
            ventaDao.actualizar(
                venta.copy(
                    estado = accion.estadoResultante.name,
                    observacion = texto.ifEmpty { venta.observacion },
                    motivoRechazo = if (accion == AccionRevision.RECHAZAR) texto else venta.motivoRechazo,
                    revisorId = usuario.id,
                    revisorNombre = usuario.nombre,
                    fechaRevision = ahora,
                    fechaActualizacion = ahora
                )
            )

            val (descripcion, aviso) = when (accion) {
                AccionRevision.VALIDAR -> "Venta validada" to "Tu venta ${venta.codigoAutorizacion} fue validada."
                AccionRevision.SOLICITAR_CORRECCION ->
                    "Corrección solicitada" to "Tu venta ${venta.codigoAutorizacion} fue observada: $texto"
                AccionRevision.RECHAZAR ->
                    "Venta rechazada" to "Tu venta ${venta.codigoAutorizacion} fue rechazada: $texto"
            }
            registrarHistorial(ventaId, estadoActual, accion.estadoResultante, descripcion, usuario, texto.ifEmpty { null })
            notificar(venta.vendedorId, Perfil.VENDEDOR, ventaId, aviso)
        }
    }

    /** Marca la venta como derivada y conserva la referencia recibida del servicio externo. */
    suspend fun marcarGestionada(
        ventaId: Long,
        usuario: UsuarioEntity,
        referenciaExterna: String? = null
    ) {
        exigirPerfil(usuario, Perfil.ENCARGADO, "Solo el encargado de gestión puede derivar ventas.")
        db.withTransaction {
            val venta = ventaDao.obtenerPorId(ventaId) ?: throw ReglaNegocioException("La venta no existe.")
            if (venta.estado != EstadoVenta.VALIDADA.name) {
                throw ReglaNegocioException("Solo se pueden derivar al prestador las ventas validadas.")
            }
            ventaDao.actualizar(venta.copy(estado = EstadoVenta.GESTIONADA.name, fechaActualizacion = System.currentTimeMillis()))
            registrarHistorial(
                ventaId, EstadoVenta.VALIDADA, EstadoVenta.GESTIONADA,
                "Derivada al prestador (API REST de pruebas)", usuario, referenciaExterna
            )
            notificar(venta.vendedorId, Perfil.VENDEDOR, ventaId, "Tu venta ${venta.codigoAutorizacion} fue gestionada con el prestador.")
        }
    }

    /** Elimina las fotos de prueba del teléfono (exigencia del caso al finalizar el proyecto). */
    suspend fun eliminarRespaldosDePrueba(usuario: UsuarioEntity): Int {
        if (Perfil.desde(usuario.perfil) !in setOf(Perfil.ENCARGADO, Perfil.ADMINISTRADOR)) {
            throw ReglaNegocioException("Solo el encargado o el administrador pueden limpiar los respaldos.")
        }
        return withContext(Dispatchers.IO) {
            val borrados = ImagenUtils.eliminarTodosLosRespaldos(contexto)
            ventaDao.limpiarRutasDeRespaldo()
            borrados
        }
    }

    /**
     * Borra del teléfono las fotos que ya no pertenecen a ninguna venta (por ejemplo, las que se
     * tomaron en un formulario y luego se descartaron). Se ejecuta al abrir la aplicación.
     */
    suspend fun eliminarRespaldosHuerfanos(): Int = withContext(Dispatchers.IO) {
        ImagenUtils.eliminarHuerfanos(contexto, ventaDao.rutasDeRespaldo().toSet())
    }

    // =========================================================================
    // ALERTAS
    // =========================================================================

    fun notificaciones(usuario: UsuarioEntity): Flow<List<NotificacionEntity>> =
        notificacionDao.observarPara(usuario.id, usuario.perfil)

    fun cantidadNoLeidas(usuario: UsuarioEntity): Flow<Int> =
        notificacionDao.observarNoLeidas(usuario.id, usuario.perfil)

    suspend fun marcarNotificacionesLeidas(usuario: UsuarioEntity) =
        notificacionDao.marcarTodasLeidas(usuario.id, usuario.perfil)

    // =========================================================================
    // UTILIDADES INTERNAS
    // =========================================================================

    private fun exigirPerfil(usuario: UsuarioEntity, requerido: Perfil, mensaje: String) {
        if (Perfil.desde(usuario.perfil) != requerido) throw ReglaNegocioException(mensaje)
    }

    private suspend fun registrarHistorial(
        ventaId: Long,
        anterior: EstadoVenta?,
        nuevo: EstadoVenta,
        accion: String,
        usuario: UsuarioEntity,
        observacion: String?
    ) {
        historialDao.insertar(
            HistorialEntity(
                ventaId = ventaId,
                estadoAnterior = anterior?.name,
                estadoNuevo = nuevo.name,
                accion = accion,
                usuarioId = usuario.id,
                usuarioNombre = usuario.nombre,
                observacion = observacion
            )
        )
    }

    private suspend fun notificar(destinoId: Long?, perfil: Perfil, ventaId: Long, mensaje: String) {
        notificacionDao.insertar(
            NotificacionEntity(
                usuarioDestinoId = destinoId,
                perfilDestino = perfil.name,
                ventaId = ventaId,
                mensaje = mensaje
            )
        )
    }

    // =========================================================================
    // DATOS DE DEMOSTRACIÓN (todo ficticio)
    // =========================================================================

    /**
     * Prepara los datos de demostración. Es idempotente (se puede llamar muchas veces):
     *  - Crea las cuentas de ejemplo que FALTEN, buscándolas por correo. Así una base que ya existía
     *    recibe también el perfil administrador sin perder sus datos.
     *  - Deja un porcentaje de comisión inicial por cada convenio (sin pisar los ya configurados).
     *  - Las ventas de ejemplo solo se crean si la base estaba completamente vacía.
     */
    suspend fun sembrarDatosDemoSiCorresponde() = candadoSiembra.withLock {
        val baseVacia = usuarioDao.contar() == 0

        val vendedor1 = asegurarCuenta("Camila Rojas (ficticia)", "vendedor1@demo.cl", "Ventas123", Perfil.VENDEDOR, "Sucursal Centro")
        val vendedor2 = asegurarCuenta("Diego Fuentes (ficticio)", "vendedor2@demo.cl", "Ventas123", Perfil.VENDEDOR, "Sucursal Norte")
        val encargado = asegurarCuenta("Valentina Soto (ficticia)", "encargado@demo.cl", "Gestion123", Perfil.ENCARGADO, "Casa Matriz")
        asegurarCuenta("Isidora Campos (ficticia)", "admin@demo.cl", "Admin12345", Perfil.ADMINISTRADOR, "Casa Matriz")

        TipoConvenio.entries.forEach { comisionDao.insertarSiNoExiste(ComisionEntity(it.name, it.porcentajeComision)) }

        if (!baseVacia) return@withLock

        sembrarVenta(vendedor2, encargado, "DEMO0001", TipoConvenio.DENTAL, EstadoVenta.PENDIENTE_REVISION, 180_000, "Paula Medina (ficticia)", "11.111.111-1", "", 2)
        sembrarVenta(vendedor2, encargado, "DEMO0002", TipoConvenio.VETERINARIO, EstadoVenta.PENDIENTE_REVISION, 95_000, "Joaquín Pérez (ficticio)", "22.222.222-2", "Luna (ficticia)", 1)
        sembrarVenta(vendedor2, encargado, "DEMO0003", TipoConvenio.AUTOMOTRIZ, EstadoVenta.VALIDADA, 250_000, "Marcela Díaz (ficticia)", "11.111.111-1", "", 6)
        sembrarVenta(
            vendedor1, encargado, "DEMO0004", TipoConvenio.DENTAL, EstadoVenta.OBSERVADA, 120_000,
            "Andrés Silva (ficticio)", "22.222.222-2", "", 3,
            observacion = "El comprobante no se lee bien. Adjunta una foto más nítida del respaldo."
        )
    }

    /** Devuelve la cuenta con ese correo; si no existe, la crea. */
    private suspend fun asegurarCuenta(
        nombre: String, correo: String, clave: String, perfil: Perfil, zona: String
    ): UsuarioEntity {
        usuarioDao.buscarPorCorreo(correo)?.let { return it }
        val nueva = construirUsuario(nombre, correo, clave, perfil, zona)
        return nueva.copy(id = usuarioDao.insertar(nueva))
    }

    private suspend fun sembrarVenta(
        vendedor: UsuarioEntity,
        encargado: UsuarioEntity,
        codigo: String,
        tipo: TipoConvenio,
        estado: EstadoVenta,
        monto: Int,
        cliente: String,
        rut: String,
        mascota: String,
        diasAtras: Int,
        observacion: String? = null
    ) {
        val unDia = 24L * 60 * 60 * 1000
        val ahora = System.currentTimeMillis()
        val id = ventaDao.insertar(
            VentaEntity(
                vendedorId = vendedor.id,
                vendedorNombre = vendedor.nombre,
                tipoConvenio = tipo.name,
                prestador = tipo.prestadores.first(),
                fechaOperacion = Formato.hoyComoFechaUtc() - diasAtras * unDia,
                monto = monto,
                codigoAutorizacion = codigo,
                clienteNombre = cliente,
                clienteRut = rut,
                clienteCorreo = "cliente.demo@ejemplo.cl",
                clienteTelefono = "912345678",
                mascotaNombre = mascota,
                respaldoPath = null,
                estado = estado.name,
                observacion = observacion,
                fechaEnvio = ahora - diasAtras * unDia,
                fechaActualizacion = ahora - diasAtras * unDia
            )
        )
        registrarHistorial(id, EstadoVenta.BORRADOR, EstadoVenta.PENDIENTE_REVISION, "Venta enviada a revisión", vendedor, null)
        when (estado) {
            EstadoVenta.VALIDADA -> registrarHistorial(id, EstadoVenta.PENDIENTE_REVISION, estado, "Venta validada", encargado, null)
            EstadoVenta.OBSERVADA -> {
                registrarHistorial(id, EstadoVenta.PENDIENTE_REVISION, estado, "Corrección solicitada", encargado, observacion)
                notificar(vendedor.id, Perfil.VENDEDOR, id, "Tu venta $codigo fue observada: $observacion")
            }
            EstadoVenta.PENDIENTE_REVISION ->
                notificar(null, Perfil.ENCARGADO, id, "Nueva venta recibida: $codigo (${vendedor.nombre})")
            else -> Unit
        }
    }
}
