package com.example.ventasapp.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ventasapp.ui.components.EstadoVacio
import com.example.ventasapp.ui.components.PantallaBase
import com.example.ventasapp.util.Formato

import com.example.ventasapp.viewmodel.AlertasViewModel


/**
 * Alertas internas de la aplicación (nueva venta recibida, venta observada, validada, etc.).
 * Las no leídas se destacan con otro color; al salir de la pantalla se marcan todas como leídas.
 */
@Composable
fun AlertasScreen(
    viewModel: AlertasViewModel,
    onVerVenta: (Long) -> Unit,
    onCerrarSesion: () -> Unit,
    barraInferior: @Composable () -> Unit
) {
    val alertas by viewModel.notificaciones.collectAsState()

    // Se marcan como leídas al SALIR de la pantalla, para que mientras se mira se vean destacadas.
    DisposableEffect(Unit) {
        onDispose { viewModel.marcarTodasLeidas() }
    }

    PantallaBase(
        titulo = "Alertas",
        onCerrarSesion = onCerrarSesion,
        barraInferior = barraInferior
    ) { padding ->
        if (alertas.isEmpty()) {
            EstadoVacio(
                icono = Icons.Default.Notifications,
                titulo = "Sin alertas",
                mensaje = "Aquí verás los avisos sobre tus ventas.",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = alertas, key = { it.id }) { alerta ->
                    Card(
                        onClick = { onVerVenta(alerta.ventaId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (alerta.leida) MaterialTheme.colorScheme.surfaceContainerLow
                            else MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(alerta.mensaje, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    Formato.fechaHora(alerta.fecha),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
