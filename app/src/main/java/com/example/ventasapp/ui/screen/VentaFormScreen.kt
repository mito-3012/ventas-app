
package com.example.ventasapp.ui.screen
import androidx.compose.runtime.saveable.rememberSaveable
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ventasapp.model.EstadoVenta
import com.example.ventasapp.model.TipoConvenio
import com.example.ventasapp.ui.components.BannerMensaje
import com.example.ventasapp.ui.components.BotonCarga
import com.example.ventasapp.ui.components.CampoFecha
import com.example.ventasapp.ui.components.CampoFormulario
import com.example.ventasapp.ui.components.CampoSeleccion
import com.example.ventasapp.ui.components.EstadoVacio
import com.example.ventasapp.ui.components.ImagenRespaldo
import com.example.ventasapp.ui.components.PantallaBase
import com.example.ventasapp.ui.components.SelectorGaleria
import com.example.ventasapp.ui.components.TituloSeccion
import com.example.ventasapp.util.Formato
import com.example.ventasapp.util.Validadores
import com.example.ventasapp.viewmodel.VentaFormViewModel

/**
 * Formulario para registrar, editar o corregir una venta.
 *
 * Recursos nativos:
 * 1. Cámara.
 * 2. Galería.
 * 3. Vibración para validaciones.
 *
 * La validación de datos se realiza en el ViewModel.
 */
@Composable
fun VentaFormScreen(
    ventaId: Long,
    viewModel: VentaFormViewModel,
    onVolver: () -> Unit
) {
    val estado by viewModel.estado.collectAsState()
    val snackbar = androidx.compose.runtime.remember {
        SnackbarHostState()
    }

    val haptica = LocalHapticFeedback.current
    val contexto = LocalContext.current

    // Evita reiniciar el formulario al girar la pantalla.
    var inicializado by rememberSaveable {
        mutableStateOf(false)
    }

    LaunchedEffect(ventaId) {
        if (!inicializado) {
            viewModel.iniciar(ventaId)
            inicializado = true
        }
    }

    // Escucha eventos del ViewModel.
    LaunchedEffect(Unit) {
        viewModel.eventos.collect { evento ->
            if (evento.esError) {
                haptica.performHapticFeedback(
                    HapticFeedbackType.LongPress
                )
            }

            snackbar.showSnackbar(evento.texto)
        }
    }

    // ---------------------------------------------------------
    // CÁMARA
    // ---------------------------------------------------------

    val lanzadorCamara = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { exito ->
        viewModel.onFotoCapturada(exito)
    }

    val abrirCamara: () -> Unit = {
        try {
            lanzadorCamara.launch(
                viewModel.prepararFotoCamara()
            )
        } catch (e: ActivityNotFoundException) {
            viewModel.avisarCamaraNoDisponible()
        }
    }

    val lanzadorPermisoCamara =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { concedido ->
            if (concedido) {
                abrirCamara()
            } else {
                viewModel.avisarPermisoCamaraDenegado()
            }
        }

    var permisoPendiente by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    fun tienePermiso(permiso: String): Boolean {
        return ContextCompat.checkSelfPermission(
            contexto,
            permiso
        ) == PackageManager.PERMISSION_GRANTED
    }

    val alPulsarCamara: () -> Unit = {
        if (tienePermiso(Manifest.permission.CAMERA)) {
            abrirCamara()
        } else {
            permisoPendiente = "Camara"
        }
    }

    // ---------------------------------------------------------
    // GALERÍA
    // ---------------------------------------------------------

    val permisoGaleria =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    var mostrarGaleria by rememberSaveable {
        mutableStateOf(false)
    }

    val lanzadorPermisoGaleria =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { concedido ->
            if (concedido) {
                mostrarGaleria = true
            } else {
                viewModel.avisarPermisoGaleriaDenegado()
            }
        }

    val alPulsarGaleria: () -> Unit = {
        if (tienePermiso(permisoGaleria)) {
            mostrarGaleria = true
        } else {
            permisoPendiente = "Galería"
        }
    }

    // ---------------------------------------------------------
    // PANTALLA PRINCIPAL
    // ---------------------------------------------------------

    PantallaBase(
        titulo = when {
            estado.estadoOriginal == EstadoVenta.OBSERVADA ->
                "Corregir venta"

            estado.esEdicion ->
                "Editar borrador"

            else ->
                "Nueva venta"
        },
        onVolver = onVolver,
        snackbarHostState = snackbar
    ) { padding ->

        when {
            estado.cargando -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            estado.noAutorizado -> {
                EstadoVacio(
                    icono = Icons.Default.Warning,
                    titulo = "No se puede editar esta venta",
                    mensaje = "No existe, pertenece a otro vendedor o ya fue enviada y no admite cambios.",
                    modifier = Modifier.padding(padding)
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {

                    // -------------------------------------------------
                    // OBSERVACIÓN DE CORRECCIÓN
                    // -------------------------------------------------

                    AnimatedVisibility(
                        visible = estado.observacionPrevia != null
                    ) {
                        BannerMensaje(
                            texto = "Observación del encargado: ${estado.observacionPrevia.orEmpty()}",
                            esError = true,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // -------------------------------------------------
                    // DATOS DE LA OPERACIÓN
                    // -------------------------------------------------

                    TituloSeccion(
                        "Datos de la operación",
                        Icons.Default.Description
                    )

                    // -------------------------------------------------
                    // CATÁLOGO DE CONVENIOS DE LA API
                    // -------------------------------------------------

                    TituloSeccion(
                        "Convenios disponibles",
                        Icons.Default.Business
                    )

                    AnimatedVisibility(
                        visible = estado.cargandoConvenios
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )

                            Text(
                                text = "Consultando convenios...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Si la API falla, el formulario continúa
                    // funcionando con los datos locales.
                    AnimatedVisibility(
                        visible = estado.errorConvenios != null
                    ) {
                        BannerMensaje(
                            texto = estado.errorConvenios
                                ?: "No se pudo consultar el catálogo. Puedes continuar con los convenios locales.",
                            esError = false,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    // Selector de convenios externos.
                    if (estado.convenios.isNotEmpty()) {
                        CampoSeleccion(
                            valor = estado.convenioSeleccionado?.nombre ?: "",
                            etiqueta = "Seleccionar convenio externo",
                            icono = Icons.Default.Business,
                            opciones = estado.convenios.map { it.nombre },
                            onSeleccion = { indice ->
                                estado.convenios.getOrNull(indice)?.let {
                                    viewModel.seleccionarConvenio(it)
                                }
                            },
                            ayuda = "Selecciona un convenio obtenido desde la API."
                        )

                        // Detalles del convenio seleccionado.
                        AnimatedVisibility(
                            visible = estado.convenioSeleccionado != null
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                estado.convenioSeleccionado?.let { convenio ->

                                    Text(
                                        text = convenio.nombre,
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Text(
                                        text = "ID del convenio: ${convenio.id}",
                                        style = MaterialTheme.typography.bodySmall
                                    )

                                    Text(
                                        text = "Descripción: ${convenio.descripcion}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    Text(
                                        text = "Condiciones: ${convenio.condiciones}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    if (convenio.prestadores.isNotEmpty()) {
                                        Text(
                                            text = "Prestadores: ${convenio.prestadores.joinToString(", ")}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    } else if (!estado.cargandoConvenios &&
                        estado.errorConvenios == null
                    ) {
                        Text(
                            text = "No hay convenios externos disponibles. Puedes continuar usando los convenios locales.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // -------------------------------------------------
                    // TIPO DE CONVENIO LOCAL
                    // -------------------------------------------------

                    CampoSeleccion(
                        valor = estado.tipoConvenio?.etiqueta ?: "",
                        etiqueta = "Tipo de convenio o servicio",
                        icono = Icons.Default.Description,
                        opciones = TipoConvenio.entries.map {
                            it.etiqueta
                        },
                        onSeleccion = { indice ->
                            TipoConvenio.entries.getOrNull(indice)?.let {
                                viewModel.onTipoChange(it)
                            }
                        },
                        error = estado.errores.tipoConvenio
                    )

                    val prestadores =
                        estado.tipoConvenio?.prestadores ?: emptyList()

                    CampoSeleccion(
                        valor = estado.prestador,
                        etiqueta = "Prestador asociado (ficticio)",
                        icono = Icons.Default.Business,
                        opciones = prestadores,
                        onSeleccion = { indice ->
                            prestadores.getOrNull(indice)?.let {
                                viewModel.onPrestadorChange(it)
                            }
                        },
                        error = estado.errores.prestador,
                        ayuda = if (estado.tipoConvenio == null) {
                            "Primero elige el tipo de convenio"
                        } else {
                            null
                        },
                        habilitado = estado.tipoConvenio != null
                    )

                    // -------------------------------------------------
                    // FECHA Y MONTO
                    // -------------------------------------------------

                    CampoFecha(
                        fechaMillis = estado.fechaOperacion,
                        etiqueta = "Fecha de la operación o cita",
                        onFechaSeleccionada = viewModel::onFechaChange,
                        error = estado.errores.fecha,
                        ayuda = "Puede ser una fecha pasada o una cita a futuro (hasta 1 año)",
                        limiteMaximo = Validadores.fechaLimiteFutura(
                            Formato.hoyComoFechaUtc()
                        )
                    )

                    CampoFormulario(
                        valor = estado.monto,
                        onValorChange = viewModel::onMontoChange,
                        etiqueta = "Monto de la venta (ficticio)",
                        icono = Icons.Default.AttachMoney,
                        error = estado.errores.monto,
                        ayuda = "Solo números, en pesos y sin puntos",
                        teclado = KeyboardType.Number
                    )

                    CampoFormulario(
                        valor = estado.codigo,
                        onValorChange = viewModel::onCodigoChange,
                        etiqueta = "Código de autorización (ficticio)",
                        icono = Icons.Default.VpnKey,
                        error = estado.errores.codigo,
                        ayuda = "6 a 12 caracteres: mayúsculas y números",
                        capitalizacion = KeyboardCapitalization.Characters
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // -------------------------------------------------
                    // CLIENTE
                    // -------------------------------------------------

                    TituloSeccion(
                        "Cliente (datos ficticios)",
                        Icons.Default.Person
                    )

                    CampoFormulario(
                        valor = estado.clienteNombre,
                        onValorChange = viewModel::onClienteNombreChange,
                        etiqueta = "Nombre del cliente",
                        icono = Icons.Default.Person,
                        error = estado.errores.clienteNombre,
                        capitalizacion = KeyboardCapitalization.Words
                    )

                    CampoFormulario(
                        valor = estado.clienteRut,
                        onValorChange = viewModel::onClienteRutChange,
                        etiqueta = "RUT del cliente (ficticio)",
                        icono = Icons.Default.Badge,
                        error = estado.errores.clienteRut,
                        ayuda = "Ejemplo de prueba: 11.111.111-1"
                    )

                    CampoFormulario(
                        valor = estado.clienteCorreo,
                        onValorChange = viewModel::onClienteCorreoChange,
                        etiqueta = "Correo del cliente (ficticio)",
                        icono = Icons.Default.Email,
                        error = estado.errores.clienteCorreo,
                        teclado = KeyboardType.Email
                    )

                    CampoFormulario(
                        valor = estado.clienteTelefono,
                        onValorChange = viewModel::onClienteTelefonoChange,
                        etiqueta = "Teléfono del cliente (ficticio)",
                        icono = Icons.Default.Phone,
                        error = estado.errores.clienteTelefono,
                        ayuda = "Ejemplo: 912345678",
                        teclado = KeyboardType.Phone
                    )

                    // El campo de mascota aparece solo
                    // cuando el tipo de convenio lo requiere.
                    AnimatedVisibility(
                        visible = estado.tipoConvenio?.requiereMascota == true
                    ) {
                        CampoFormulario(
                            valor = estado.mascotaNombre,
                            onValorChange = viewModel::onMascotaChange,
                            etiqueta = "Nombre de la mascota (ficticio)",
                            icono = Icons.Default.Pets,
                            error = estado.errores.mascota,
                            capitalizacion = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // -------------------------------------------------
                    // RESPALDO FOTOGRÁFICO
                    // -------------------------------------------------

                    TituloSeccion(
                        "Respaldo fotográfico",
                        Icons.Default.Image
                    )

                    BannerMensaje(
                        texto = "Usa solo comprobantes ficticios. No fotografíes documentos reales ni personas. Formatos JPG, PNG o WEBP, máximo 2 MB.",
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    ImagenRespaldo(
                        ruta = estado.respaldoPath,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )

                    AnimatedVisibility(
                        visible = estado.errores.respaldo != null
                    ) {
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )

                            Text(
                                text = estado.errores.respaldo.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = estado.procesandoImagen
                    ) {
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )

                            Text(
                                text = "Procesando imagen...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = alPulsarCamara,
                            modifier = Modifier.weight(1f),
                            enabled = !estado.procesandoImagen
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )

                            Spacer(Modifier.size(6.dp))

                            Text("Cámara")
                        }

                        OutlinedButton(
                            onClick = alPulsarGaleria,
                            modifier = Modifier.weight(1f),
                            enabled = !estado.procesandoImagen
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )

                            Spacer(Modifier.size(6.dp))

                            Text("Galería")
                        }
                    }

                    AnimatedVisibility(
                        visible = estado.respaldoPath != null
                    ) {
                        TextButton(
                            onClick = viewModel::quitarRespaldo
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )

                            Spacer(Modifier.size(6.dp))

                            Text("Quitar imagen")
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // -------------------------------------------------
                    // ACCIONES
                    // -------------------------------------------------

                    val esCorreccion =
                        estado.estadoOriginal == EstadoVenta.OBSERVADA

                    OutlinedButton(
                        onClick = {
                            viewModel.guardarBorrador(onVolver)
                        },
                        enabled = !estado.guardando &&
                                !estado.procesandoImagen,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (esCorreccion) {
                                "Guardar cambios sin enviar"
                            } else {
                                "Guardar borrador"
                            }
                        )
                    }

                    BotonCarga(
                        texto = if (esCorreccion) {
                            "Enviar corrección"
                        } else {
                            "Enviar a revisión"
                        },
                        cargando = estado.guardando,
                        onClick = {
                            viewModel.enviarARevision(onVolver)
                        },
                        habilitado = !estado.procesandoImagen,
                        icono = Icons.AutoMirrored.Filled.Send,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    // ---------------------------------------------------------
    // DIÁLOGO EXPLICATIVO DE PERMISOS
    // ---------------------------------------------------------

    permisoPendiente?.let { cual ->

        val esCamara = cual == "Camara"

        AlertDialog(
            onDismissRequest = {
                permisoPendiente = null
            },
            title = {
                Text(
                    if (esCamara) {
                        "Permiso de cámara"
                    } else {
                        "Permiso para ver tus fotos"
                    }
                )
            },
            text = {
                Text(
                    if (esCamara) {
                        "Para tomar la foto del comprobante, la aplicación necesita usar la cámara del dispositivo. Solo se usa para fotografiar comprobantes ficticios. A continuación, Android te pedirá confirmar el permiso."
                    } else {
                        "Para elegir una imagen de la galería, la aplicación necesita permiso de lectura de las fotos del dispositivo. Solo se usa para elegir el respaldo de la venta. A continuación, Android te pedirá confirmar el permiso."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        permisoPendiente = null

                        if (esCamara) {
                            lanzadorPermisoCamara.launch(
                                Manifest.permission.CAMERA
                            )
                        } else {
                            lanzadorPermisoGaleria.launch(
                                permisoGaleria
                            )
                        }
                    }
                ) {
                    Text("Continuar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        permisoPendiente = null
                    }
                ) {
                    Text("Ahora no")
                }
            }
        )
    }

    // ---------------------------------------------------------
    // SELECTOR DE GALERÍA
    // ---------------------------------------------------------

    if (mostrarGaleria) {
        SelectorGaleria(
            onElegida = { uri ->
                mostrarGaleria = false
                viewModel.onImagenGaleria(uri)
            },
            onCerrar = {
                mostrarGaleria = false
            }
        )
    }
}
