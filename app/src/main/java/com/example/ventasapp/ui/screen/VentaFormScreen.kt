package com.example.ventasapp.ui.screen

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
import androidx.compose.runtime.remember
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
 * Recursos nativos que usa esta pantalla (rúbrica IE 2.4.1):
 *  1. CÁMARA: se pide el permiso en tiempo de ejecución y la foto se guarda con FileProvider.
 *  2. GALERÍA: selector de fotos del sistema (Photo Picker), que no necesita permisos.
 *  3. VIBRACIÓN: respuesta háptica cuando una validación falla.
 *
 * Importante: esta pantalla NO válida. Solo muestra los errores que deja el ViewModel.
 */
@Composable
fun VentaFormScreen(
    ventaId: Long,
    viewModel: VentaFormViewModel,
    onVolver: () -> Unit
) {
    val estado by viewModel.estado.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val haptica = LocalHapticFeedback.current
    val contexto = LocalContext.current

    // rememberSaveable sobrevive a la rotación de pantalla: así el formulario no se reinicia al girar.
    var inicializado by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!inicializado) {
            viewModel.iniciar(ventaId)
            inicializado = true
        }
    }

    // Escucha los mensajes del ViewModel: los muestra en un Snackbar y vibra si son errores.
    LaunchedEffect(Unit) {
        viewModel.eventos.collect { evento ->
            if (evento.esError) haptica.performHapticFeedback(HapticFeedbackType.LongPress)
            snackbar.showSnackbar(evento.texto)
        }
    }

    // ---- Recurso nativo: CÁMARA ----
    val lanzadorCamara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        viewModel.onFotoCapturada(exito)
    }
    val abrirCamara: () -> Unit = {
        try {
            lanzadorCamara.launch(viewModel.prepararFotoCamara())
        } catch (e: ActivityNotFoundException) {
            viewModel.avisarCamaraNoDisponible()
        }
    }
    val lanzadorPermisoCamara = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) abrirCamara() else viewModel.avisarPermisoCamaraDenegado()
    }
    // Antes de pedir un permiso se muestra un diálogo PROPIO en español que explica para qué se usa.
    // "Camara" o "Galería" = diálogo abierto; null = ninguno.
    var permisoPendiente by rememberSaveable { mutableStateOf<String?>(null) }

    fun tienePermiso(permiso: String) =
        ContextCompat.checkSelfPermission(contexto, permiso) == PackageManager.PERMISSION_GRANTED

    val alPulsarCamara: () -> Unit = {
        if (tienePermiso(Manifest.permission.CAMERA)) abrirCamara() else permisoPendiente = "Camara"
    }

    // ---- Recurso nativo: GALERÍA (galería propia de la app, con todos sus textos en español) ----
    // Android 13 o superior usa READ_MEDIA_IMAGES; versiones anteriores, READ_EXTERNAL_STORAGE.
    val permisoGaleria = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var mostrarGaleria by rememberSaveable { mutableStateOf(false) }
    val lanzadorPermisoGaleria = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) mostrarGaleria = true else viewModel.avisarPermisoGaleriaDenegado()
    }
    val alPulsarGaleria: () -> Unit = {
        if (tienePermiso(permisoGaleria)) mostrarGaleria = true else permisoPendiente = "Galería"
    }

    PantallaBase(
        titulo = when {
            estado.estadoOriginal == EstadoVenta.OBSERVADA -> "Corregir venta"
            estado.esEdicion -> "Editar borrador"
            else -> "Nueva venta"
        },
        onVolver = onVolver,
        snackbarHostState = snackbar
    ) { padding ->
        when {
            estado.cargando -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            estado.noAutorizado -> EstadoVacio(
                icono = Icons.Default.Warning,
                titulo = "No se puede editar esta venta",
                mensaje = "No existe, pertenece a otro vendedor o ya fue enviada y no admite cambios.",
                modifier = Modifier.padding(padding)
            )

            else -> Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Si el encargado pidió una corrección, se muestra su observación arriba.
                AnimatedVisibility(visible = estado.observacionPrevia != null) {
                    BannerMensaje(
                        texto = "Observación del encargado: ${estado.observacionPrevia.orEmpty()}",
                        esError = true,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // ---------------- Datos de la operación ----------------
                TituloSeccion("Datos de la operación", Icons.Default.Description)

                CampoSeleccion(
                    valor = estado.tipoConvenio?.etiqueta ?: "",
                    etiqueta = "Tipo de convenio o servicio",
                    icono = Icons.Default.Description,
                    opciones = TipoConvenio.entries.map { it.etiqueta },
                    onSeleccion = { viewModel.onTipoChange(TipoConvenio.entries[it]) },
                    error = estado.errores.tipoConvenio
                )
                val prestadores = estado.tipoConvenio?.prestadores ?: emptyList()
                CampoSeleccion(
                    valor = estado.prestador,
                    etiqueta = "Prestador asociado (ficticio)",
                    icono = Icons.Default.Business,
                    opciones = prestadores,
                    onSeleccion = { viewModel.onPrestadorChange(prestadores[it]) },
                    error = estado.errores.prestador,
                    ayuda = if (estado.tipoConvenio == null) "Primero elige el tipo de convenio" else null,
                    habilitado = estado.tipoConvenio != null
                )
                CampoFecha(
                    fechaMillis = estado.fechaOperacion,
                    etiqueta = "Fecha de la operación o cita",
                    onFechaSeleccionada = viewModel::onFechaChange,
                    error = estado.errores.fecha,
                    ayuda = "Puede ser una fecha pasada o una cita a futuro (hasta 1 año)",
                    // El calendario deja elegir hasta 1 año hacia adelante (citas a futuro).
                    limiteMaximo = Validadores.fechaLimiteFutura(Formato.hoyComoFechaUtc())
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

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                // ---------------- Cliente ficticio ----------------
                TituloSeccion("Cliente (datos ficticios)", Icons.Default.Person)

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
                // El campo de mascota solo aparece para convenios veterinarios (campo condicional).
                AnimatedVisibility(visible = estado.tipoConvenio?.requiereMascota == true) {
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

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                // ---------------- Respaldo fotográfico ----------------
                TituloSeccion("Respaldo fotográfico", Icons.Default.Image)
                BannerMensaje(
                    texto = "Usa solo comprobantes Ficticios. No fotografíes documentos reales ni personas. " +
                        "Formatos JPG, PNG o WEBP, máximo 2 MB.",
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                ImagenRespaldo(
                    ruta = estado.respaldoPath,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )

                AnimatedVisibility(visible = estado.errores.respaldo != null) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Text(
                            estado.errores.respaldo.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                AnimatedVisibility(visible = estado.procesandoImagen) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Procesando imagen...", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = alPulsarCamara, modifier = Modifier.weight(1f), enabled = !estado.procesandoImagen) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Cámara")
                    }
                    OutlinedButton(
                        onClick = alPulsarGaleria,
                        modifier = Modifier.weight(1f),
                        enabled = !estado.procesandoImagen
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Galería")
                    }
                }
                AnimatedVisibility(visible = estado.respaldoPath != null) {
                    TextButton(onClick = viewModel::quitarRespaldo) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Quitar imagen")
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                // ---------------- Acciones ----------------
                val esCorreccion = estado.estadoOriginal == EstadoVenta.OBSERVADA
                OutlinedButton(
                    onClick = { viewModel.guardarBorrador(onVolver) },
                    enabled = !estado.guardando && !estado.procesandoImagen,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (esCorreccion) "Guardar cambios sin enviar" else "Guardar borrador")
                }
                BotonCarga(
                    texto = if (esCorreccion) "Enviar corrección" else "Enviar a revisión",
                    cargando = estado.guardando,
                    onClick = { viewModel.enviarARevision(onVolver) },
                    habilitado = !estado.procesandoImagen,
                    icono = Icons.AutoMirrored.Filled.Send,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // ---- Explicación (en español) previa al permiso del sistema ----
    permisoPendiente?.let { cual ->
        val esCamara = cual == "Camara"
        AlertDialog(
            onDismissRequest = { permisoPendiente = null },
            title = { Text(if (esCamara) "Permiso de cámara" else "Permiso para ver tus fotos") },
            text = {
                Text(
                    if (esCamara) {
                        "Para tomar la foto del comprobante, la aplicación necesita usar la cámara del " +
                            "dispositivo. Solo se usa para fotografiar comprobantes ficticios. " +
                            "A continuación, Android te pedirá confirmar el permiso."
                    } else {
                        "Para elegir una imagen de la galería, la aplicación necesita permiso de lectura " +
                            "de las fotos del dispositivo. Solo se usa para elegir el respaldo de la venta. " +
                            "A continuación, Android te pedirá confirmar el permiso."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    permisoPendiente = null
                    if (esCamara) lanzadorPermisoCamara.launch(Manifest.permission.CAMERA)
                    else lanzadorPermisoGaleria.launch(permisoGaleria)
                }) { Text("Continuar") }
            },
            dismissButton = { TextButton(onClick = { permisoPendiente = null }) { Text("Ahora no") } }
        )
    }

    // ---- Galería propia (pantalla completa) ----
    if (mostrarGaleria) {
        SelectorGaleria(
            onElegida = { uri ->
                mostrarGaleria = false
                viewModel.onImagenGaleria(uri)
            },
            onCerrar = { mostrarGaleria = false }
        )
    }
}
