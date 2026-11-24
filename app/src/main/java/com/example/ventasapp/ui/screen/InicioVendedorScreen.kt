package com.example.ventasapp.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ventasapp.model.EstadoVenta
import com.example.ventasapp.model.data.local.UsuarioEntity
import com.example.ventasapp.ui.components.PantallaBase
import com.example.ventasapp.ui.components.TarjetaNumero
import com.example.ventasapp.ui.components.TarjetaVenta
import com.example.ventasapp.ui.theme.colores
import com.example.ventasapp.viewmodel.MisVentasViewModel




/**
 * Pantalla de inicio del vendedor: saludo, resumen ficticio de sus operaciones, aviso de ventas
 * observadas y acceso rápido para registrar una venta nueva.
 */
@Composable
fun InicioVendedorScreen(
    usuario: UsuarioEntity?,
    viewModel: MisVentasViewModel,
    onNuevaVenta: () -> Unit,
    onVerObservadas: () -> Unit,
    onVerVenta: (Long) -> Unit,
    onCerrarSesion: () -> Unit,
    barraInferior: @Composable () -> Unit
) {
    val resumen by viewModel.resumen.collectAsState()
    val todas by viewModel.todas.collectAsState()

    fun cantidad(estado: EstadoVenta) = resumen.porEstado[estado] ?: 0
    val pendientes = cantidad(EstadoVenta.PENDIENTE_REVISION) + cantidad(EstadoVenta.CORREGIDA)
    val validadas = cantidad(EstadoVenta.VALIDADA) + cantidad(EstadoVenta.GESTIONADA)
    val observadas = cantidad(EstadoVenta.OBSERVADA)

    PantallaBase(
        titulo = "Inicio",
        onCerrarSesion = onCerrarSesion,
        barraInferior = barraInferior,
        botonFlotante = {
            ExtendedFloatingActionButton(
                onClick = onNuevaVenta,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nueva venta") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Hola,", style = MaterialTheme.typography.bodyMedium)
                        Text(usuario?.nombre ?: "", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "${usuario?.zona.orEmpty()} - Perfil vendedor",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Aviso destacado solo si hay ventas que el encargado pidió corregir.
            if (observadas > 0) {
                item {
                    Card(
                        onClick = onVerObservadas,
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null)
                            Text(
                                if (observadas == 1) "Tienes 1 venta observada que necesita corrección. Pulsa para verla."
                                else "Tienes $observadas ventas observadas que necesitan corrección. Pulsa para verlas.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            item {
                Text("Resumen de mis operaciones", style = MaterialTheme.typography.titleMedium)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TarjetaNumero("Borradores", cantidad(EstadoVenta.BORRADOR), Icons.Default.Edit, Modifier.weight(1f),
                        EstadoVenta.BORRADOR.colores().texto)
                    TarjetaNumero("En revisión", pendientes, Icons.Default.Schedule, Modifier.weight(1f),
                        EstadoVenta.PENDIENTE_REVISION.colores().texto)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TarjetaNumero("Observadas", observadas, Icons.Default.Warning, Modifier.weight(1f),
                        EstadoVenta.OBSERVADA.colores().texto)
                    TarjetaNumero("Validadas", validadas, Icons.Default.CheckCircle, Modifier.weight(1f),
                        EstadoVenta.VALIDADA.colores().texto)
                }
            }
            item {
                TarjetaNumero(
                    titulo = "Comisión estimada (ficticia, solo ventas validadas)",
                    valor = resumen.comisionEstimada,
                    icono = Icons.Default.CheckCircle,
                    modifier = Modifier.fillMaxWidth(),
                    formatoPesos = true
                )
            }

            item { Text("Últimas ventas", style = MaterialTheme.typography.titleMedium) }
            if (todas.isEmpty()) {
                item {
                    Text(
                        "Aún no registras ventas. Pulsa \"Nueva venta\" para crear la primera.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(items = todas.take(3), key = { it.id }) { venta ->
                    TarjetaVenta(venta = venta, onClick = { onVerVenta(venta.id) })
                }
            }
            // Espacio final para que el botón flotante no tape la última tarjeta.
            item { Spacer(Modifier.height(64.dp)) }
        }
    }
}
