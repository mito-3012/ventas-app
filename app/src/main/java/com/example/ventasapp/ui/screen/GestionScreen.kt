package com.example.ventasapp.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import com.example.ventasapp.model.Perfil
import com.example.ventasapp.model.data.local.VentaEntity
import com.example.ventasapp.ui.components.BannerMensaje
import com.example.ventasapp.ui.components.DialogoEliminarVenta
import com.example.ventasapp.ui.components.EstadoVacio
import com.example.ventasapp.ui.components.PanelFiltros
import com.example.ventasapp.ui.components.PantallaBase
import com.example.ventasapp.ui.components.TarjetaVenta
import com.example.ventasapp.viewmodel.GestionViewModel


/**
 * Bandeja del encargado: todas las ventas recibidas, con búsqueda por código y filtros por
 * estado, vendedor y fecha.
 */
@Composable
fun GestionScreen(
    viewModel: GestionViewModel,
    onVerVenta: (Long) -> Unit,
    onCerrarSesion: () -> Unit,
    barraInferior: @Composable () -> Unit
) {
    val filtros by viewModel.filtros.collectAsState()
    val ventas by viewModel.filtradas.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var ventaAEliminar by remember { mutableStateOf<VentaEntity?>(null) }

    // Mensajes del ViewModel (venta eliminada, error de permisos...) en un Snackbar.
    LaunchedEffect(Unit) { viewModel.eventos.collect { snackbar.showSnackbar(it.texto) } }
    val vendedores by viewModel.vendedores.collectAsState()
    val indicadores by viewModel.indicadores.collectAsState()

    PantallaBase(
        titulo = "Gestión de ventas",
        onCerrarSesion = onCerrarSesion,
        snackbarHostState = snackbar,
        barraInferior = barraInferior
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Aviso de pendientes: aparece y desaparece con animación según haya ventas por revisar.
            AnimatedVisibility(visible = indicadores.pendientesDeRevision > 0) {
                BannerMensaje(
                    texto = "Hay ${indicadores.pendientesDeRevision} venta(s) pendientes de revisión.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            PanelFiltros(
                filtros = filtros,
                onFiltrosChange = viewModel::onFiltrosChange,
                vendedores = vendedores
            )

            if (ventas.isEmpty()) {
                EstadoVacio(
                    icono = Icons.Default.Inbox,
                    titulo = if (filtros.hayFiltrosActivos) "Sin resultados" else "No hay ventas recibidas",
                    mensaje = if (filtros.hayFiltrosActivos) "Prueba cambiando o limpiando los filtros."
                    else "Cuando un vendedor envíe una venta, aparecerá aquí."
                )
            } else {
                Text(
                    "${ventas.size} venta(s)",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = ventas, key = { it.id }) { venta ->
                        TarjetaVenta(
                            venta = venta,
                            onClick = { onVerVenta(venta.id) },
                            mostrarVendedor = true,
                            modifier = Modifier.animateItem(),
                            onEliminar = if (EstadoVenta.desde(venta.estado).puedeEliminar(Perfil.ENCARGADO)) {
                                { ventaAEliminar = venta }
                            } else null
                        )
                    }
                }
            }
        }
    }

    // Confirmación antes de eliminar (borra la venta, su historial y su foto).
    ventaAEliminar?.let { venta ->
        DialogoEliminarVenta(
            venta = venta,
            onConfirmar = {
                viewModel.eliminarVenta(venta.id)
                ventaAEliminar = null
            },
            onCancelar = { ventaAEliminar = null }
        )
    }
}
