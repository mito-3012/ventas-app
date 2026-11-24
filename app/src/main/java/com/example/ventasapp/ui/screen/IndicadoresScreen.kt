package com.example.ventasapp.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ventasapp.model.EstadoVenta
import com.example.ventasapp.ui.components.PantallaBase
import com.example.ventasapp.ui.components.TarjetaNumero
import com.example.ventasapp.ui.components.TituloSeccion
import com.example.ventasapp.ui.theme.colores
import com.example.ventasapp.util.Formato
import com.example.ventasapp.viewmodel.GestionViewModel


/** Indicadores generales del proceso y datos para el cálculo (ficticio) de comisiones. */
@Composable
fun IndicadoresScreen(
    viewModel: GestionViewModel,
    onCerrarSesion: () -> Unit,
    barraInferior: @Composable () -> Unit,
    /** Solo el encargado puede borrar las fotos de prueba. */
    puedeLimpiarRespaldos: Boolean = false
) {
    val indicadores by viewModel.indicadores.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var confirmarLimpieza by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.eventos.collect { snackbar.showSnackbar(it.texto) }
    }

    PantallaBase(
        titulo = "Indicadores",
        onCerrarSesion = onCerrarSesion,
        barraInferior = barraInferior,
        snackbarHostState = snackbar
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TarjetaNumero("Pendientes de revisión", indicadores.pendientesDeRevision, Icons.Default.Schedule,
                    Modifier.weight(1f), EstadoVenta.PENDIENTE_REVISION.colores().texto)
                TarjetaNumero("Por derivar al prestador", indicadores.pendientesConPrestador, Icons.AutoMirrored.Filled.Send,
                    Modifier.weight(1f), EstadoVenta.GESTIONADA.colores().texto)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TarjetaNumero("Monto validado (ficticio)", indicadores.montoValidado, Icons.Default.AttachMoney,
                    Modifier.weight(1f), EstadoVenta.VALIDADA.colores().texto, formatoPesos = true)
                TarjetaNumero("Comisión estimada (ficticia)", indicadores.comisionEstimada, Icons.Default.CheckCircle,
                    Modifier.weight(1f), EstadoVenta.VALIDADA.colores().texto, formatoPesos = true)
            }

            // ---- Barras por estado (el progreso se anima al cambiar los datos) ----
            TituloSeccion("Ventas por estado", Icons.Default.BarChart)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EstadoVenta.entries.filter { it != EstadoVenta.BORRADOR }.forEach { estado ->
                        val cantidad = indicadores.porEstado[estado] ?: 0
                        val fraccion = if (indicadores.totalVentas == 0) 0f else cantidad.toFloat() / indicadores.totalVentas
                        val progreso by animateFloatAsState(fraccion, tween(700), label = "progresoEstado")
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(estado.etiqueta, style = MaterialTheme.typography.bodyMedium)
                                Text(cantidad.toString(), style = MaterialTheme.typography.titleSmall)
                            }
                            LinearProgressIndicator(
                                progress = { progreso },
                                modifier = Modifier.fillMaxWidth(),
                                color = estado.colores().texto
                            )
                        }
                    }
                }
            }

            // ---- Detalle por vendedor (base del cálculo simulado de comisiones) ----
            TituloSeccion("Comisiones simuladas por vendedor", Icons.Default.AttachMoney)
            if (indicadores.porVendedor.isEmpty()) {
                Text(
                    "Aún no hay ventas validadas para calcular comisiones.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                indicadores.porVendedor.forEach { resumen ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(resumen.vendedorNombre, style = MaterialTheme.typography.titleSmall)
                            Text("Ventas validadas: ${resumen.ventasValidadas}", style = MaterialTheme.typography.bodyMedium)
                            Text("Monto: ${Formato.pesos(resumen.montoValidado)}", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Comisión estimada (ficticia): ${Formato.pesos(resumen.comisionEstimada)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // ---- Limpieza de datos de prueba (exigencia del caso) ----
            if (puedeLimpiarRespaldos) {
                TituloSeccion("Datos de prueba", Icons.Default.Delete)
                Text(
                    "Elimina del teléfono todas las fotos de comprobantes ficticias adjuntadas durante las pruebas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = { confirmarLimpieza = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Text("  Eliminar respaldos de prueba")
                }
            }
        }
    }

    if (confirmarLimpieza) {
        AlertDialog(
            onDismissRequest = { confirmarLimpieza = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Eliminar respaldos de prueba") },
            text = { Text("Se borrarán todas las imágenes adjuntas de las ventas. Las ventas y su historial se conservan.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarLimpieza = false
                    viewModel.eliminarRespaldosDePrueba()
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { confirmarLimpieza = false }) { Text("Cancelar") } }
        )
    }
}
