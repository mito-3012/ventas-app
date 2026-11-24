package com.example.ventasapp.ui.screen




import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.ventasapp.model.AccionRevision
import com.example.ventasapp.model.DetalleUi
import com.example.ventasapp.model.EstadoVenta
import com.example.ventasapp.model.Perfil
import com.example.ventasapp.model.RevisionUiState
import com.example.ventasapp.model.TipoConvenio
import com.example.ventasapp.model.data.local.HistorialEntity
import com.example.ventasapp.model.data.local.UsuarioEntity
import com.example.ventasapp.model.data.local.VentaEntity
import com.example.ventasapp.ui.components.BannerMensaje
import com.example.ventasapp.ui.components.BotonCarga
import com.example.ventasapp.ui.components.CampoFormulario
import com.example.ventasapp.ui.components.DialogoEliminarVenta
import com.example.ventasapp.ui.components.EstadoVacio
import com.example.ventasapp.ui.components.EtiquetaEstado
import com.example.ventasapp.ui.components.FilaDato
import com.example.ventasapp.ui.components.ImagenRespaldo
import com.example.ventasapp.ui.components.PantallaBase
import com.example.ventasapp.ui.components.TituloSeccion
import com.example.ventasapp.ui.theme.EstiloCodigo
import com.example.ventasapp.util.Formato
import com.example.ventasapp.viewmodel.DetalleVentaViewModel


/**
 * Detalle de una venta. Una sola pantalla sirve a los dos perfiles; lo que cambia es el panel
 * de acciones:
 *  - Vendedor: puede editar (si es borrador u observada) o eliminar el borrador.
 *  - Encargado: ve el formulario de revisión (validar, solicitar corrección, rechazar).
 */
@Composable
fun DetalleVentaScreen(
    ventaId: Long,
    usuario: UsuarioEntity?,
    viewModel: DetalleVentaViewModel,
    onVolver: () -> Unit,
    onEditar: (Long) -> Unit
) {
    val detalle by viewModel.detalle.collectAsState()
    val historial by viewModel.historial.collectAsState()
    val revision by viewModel.revision.collectAsState()
    val comisiones by viewModel.comisiones.collectAsState()
    val derivando by viewModel.derivando.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val haptica = LocalHapticFeedback.current
    val perfil = usuario?.let { Perfil.desde(it.perfil) }

    LaunchedEffect(ventaId) { viewModel.cargar(ventaId) }
    LaunchedEffect(Unit) {
        viewModel.eventos.collect { evento ->
            if (evento.esError) haptica.performHapticFeedback(HapticFeedbackType.LongPress)
            snackbar.showSnackbar(evento.texto)
        }
    }

    PantallaBase(titulo = "Detalle de venta", onVolver = onVolver, snackbarHostState = snackbar) { padding ->
        // "when" sobre una sealed interface: el compilador exige cubrir los tres casos.
        when (val estado = detalle) {
            DetalleUi.Cargando -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            DetalleUi.NoDisponible -> EstadoVacio(
                icono = Icons.Default.Warning,
                titulo = "Venta no disponible",
                mensaje = "La venta no existe o no tienes permiso para verla.",
                modifier = Modifier.padding(padding)
            )

            is DetalleUi.Listo -> ContenidoDetalle(
                venta = estado.venta,
                historial = historial,
                revision = revision,
                perfil = perfil,
                comisiones = comisiones,
                derivando = derivando,
                viewModel = viewModel,
                onVolver = onVolver,
                onEditar = onEditar,
                padding = padding
            )
        }
    }
}

@Composable
private fun ContenidoDetalle(
    venta: VentaEntity,
    historial: List<HistorialEntity>,
    revision: RevisionUiState,
    perfil: Perfil?,
    comisiones: Map<TipoConvenio, Double>,
    derivando: Boolean,
    viewModel: DetalleVentaViewModel,
    onVolver: () -> Unit,
    onEditar: (Long) -> Unit,
    padding: PaddingValues
) {
    val estado = EstadoVenta.desde(venta.estado)
    val tipo = TipoConvenio.desde(venta.tipoConvenio)
    var confirmarEliminar by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ---- Cabecera: código, estado y monto ----
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(venta.codigoAutorizacion.ifBlank { "Sin código" }, style = EstiloCodigo)
                    EtiquetaEstado(estado)
                }
                Text(
                    if (venta.monto > 0) Formato.pesos(venta.monto) else "Sin monto",
                    style = MaterialTheme.typography.headlineMedium
                )
                if (estado.estaValidada && tipo != null) {
                    Text(
                        "Comisión estimada (ficticia): ${Formato.pesos(tipo.comisionDe(venta.monto, comisiones[tipo] ?: tipo.porcentajeComision))}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // ---- Avisos según el estado ----
        if (estado == EstadoVenta.OBSERVADA && !venta.observacion.isNullOrBlank()) {
            BannerMensaje("Corrección solicitada: ${venta.observacion}", esError = true)
        }
        if (estado == EstadoVenta.RECHAZADA && !venta.motivoRechazo.isNullOrBlank()) {
            BannerMensaje("Motivo del rechazo: ${venta.motivoRechazo}", esError = true)
        }

        // ---- Datos de la operación ----
        TituloSeccion("Datos de la operación", Icons.Default.Info)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilaDato("Convenio", tipo?.etiqueta ?: "Sin definir")
                FilaDato("Prestador", venta.prestador.ifBlank { "Sin definir" })
                FilaDato("Operación o cita", Formato.fechaOperacion(venta.fechaOperacion))
                FilaDato("Vendedor", venta.vendedorNombre)
                HorizontalDivider()
                FilaDato("Cliente", venta.clienteNombre.ifBlank { "-" })
                FilaDato("RUT", venta.clienteRut.ifBlank { "-" })
                FilaDato("Correo", venta.clienteCorreo.ifBlank { "-" })
                FilaDato("Teléfono", venta.clienteTelefono.ifBlank { "-" })
                if (venta.mascotaNombre.isNotBlank()) FilaDato("Mascota", venta.mascotaNombre)
                HorizontalDivider()
                FilaDato("Enviada", Formato.fechaHora(venta.fechaEnvio))
                FilaDato("Revisada", Formato.fechaHora(venta.fechaRevision))
                FilaDato("Revisor", venta.revisorNombre ?: "-")
            }
        }

        // ---- Respaldo ----
        TituloSeccion("Respaldo fotográfico", Icons.Default.Image)
        ImagenRespaldo(
            ruta = venta.respaldoPath,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        )

        // ---- Acciones del vendedor ----
        if (perfil == Perfil.VENDEDOR) {
            if (estado.esEditablePorVendedor) {
                Button(onClick = { onEditar(venta.id) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(if (estado == EstadoVenta.OBSERVADA) "  Corregir y reenviar" else "  Editar borrador")
                }
            }
        }

        // ---- Eliminar venta (vendedor o encargado): la regla vive en EstadoVenta.puedeEliminar ----
        if (perfil != null && estado.puedeEliminar(perfil)) {
            OutlinedButton(
                onClick = { confirmarEliminar = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  Eliminar venta")
            }
        }

        // ---- Panel de revisión del encargado ----
        if (perfil == Perfil.ENCARGADO) {
            if (estado.esRevisablePorEncargado) {
                PanelRevision(revision, viewModel)
            }
            if (estado == EstadoVenta.VALIDADA) {
                Text(
                    "Se enviará un resumen de la venta (sin datos del cliente) a la API de pruebas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                BotonCarga(
                    // La acción muestra el progreso mientras se procesa la solicitud de derivación.
                    texto = "Derivar al prestador (API de pruebas)",
                    cargando = derivando,
                    onClick = viewModel::derivarAlPrestador,
                    icono = Icons.AutoMirrored.Filled.Send,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // ---- Historial de cambios (trazabilidad) ----
        TituloSeccion("Historial de cambios", Icons.Default.History)
        if (historial.isEmpty()) {
            Text("Sin movimientos registrados.", style = MaterialTheme.typography.bodyMedium)
        } else {
            historial.forEach { registro -> FilaHistorial(registro) }
        }
    }

    // Confirmación antes de eliminar: borra la venta, su historial y su foto de respaldo.
    if (confirmarEliminar) {
        DialogoEliminarVenta(
            venta = venta,
            onConfirmar = {
                confirmarEliminar = false
                viewModel.eliminarVenta(onVolver)
            },
            onCancelar = { confirmarEliminar = false }
        )
    }

    // Diálogo de confirmación de la acción de revisión elegida por el encargado.
    revision.accionPendiente?.let { accion ->
        AlertDialog(
            onDismissRequest = viewModel::cancelarAccion,
            title = { Text(accion.etiqueta) },
            text = { Text("¿Confirmas esta acción sobre la venta ${venta.codigoAutorizacion}? Quedará registrada en el historial.") },
            confirmButton = { TextButton(onClick = viewModel::confirmarAccion) { Text("Confirmar") } },
            dismissButton = { TextButton(onClick = viewModel::cancelarAccion) { Text("Cancelar") } }
        )
    }
}

/** Formulario de revisión: observación (con validación) y las tres acciones posibles. */
@Composable
private fun PanelRevision(revision: RevisionUiState, viewModel: DetalleVentaViewModel) {
    val sugerencias = listOf(
        "Respaldo ilegible: adjunta una foto más nítida del comprobante.",
        "Información incompleta: faltan datos obligatorios de la operación.",
        "El monto no coincide con el comprobante adjunto."
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Revisión de la venta", style = MaterialTheme.typography.titleMedium)
            Text(
                "Para solicitar corrección o rechazar es obligatorio escribir una observación.",
                style = MaterialTheme.typography.bodySmall
            )

            // Motivos frecuentes: un toque rellena la observación.
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Respaldo ilegible", "Información incompleta", "Monto no coincide").forEachIndexed { i, texto ->
                    AssistChip(onClick = { viewModel.usarSugerencia(sugerencias[i]) }, label = { Text(texto) })
                }
            }

            CampoFormulario(
                valor = revision.observacion,
                onValorChange = viewModel::onObservacionChange,
                etiqueta = "Observación",
                icono = Icons.Default.Edit,
                error = revision.errores.observacion,
                ayuda = "Mínimo 10 caracteres",
                lineas = 3
            )

            if (revision.procesando) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                Button(
                    onClick = { viewModel.solicitarAccion(AccionRevision.VALIDAR) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Validar venta")
                }
                FilledTonalButton(
                    onClick = { viewModel.solicitarAccion(AccionRevision.SOLICITAR_CORRECCION) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Solicitar corrección")
                }
                OutlinedButton(
                    onClick = { viewModel.solicitarAccion(AccionRevision.RECHAZAR) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Rechazar venta")
                }
            }
        }
    }
}

/** Una línea del historial: qué pasó, quién lo hizo y cuándo. */
@Composable
private fun FilaHistorial(registro: HistorialEntity) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(registro.accion, style = MaterialTheme.typography.titleSmall)
            Text(
                "${registro.usuarioNombre} - ${Formato.fechaHora(registro.fecha)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!registro.observacion.isNullOrBlank()) {
                Text(registro.observacion, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
