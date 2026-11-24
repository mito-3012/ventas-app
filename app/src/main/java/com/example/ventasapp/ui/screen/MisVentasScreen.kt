package com.example.ventasapp.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
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
import com.example.ventasapp.ui.components.DialogoEliminarVenta
import com.example.ventasapp.ui.components.EstadoVacio
import com.example.ventasapp.ui.components.PanelFiltros
import com.example.ventasapp.ui.components.PantallaBase
import com.example.ventasapp.ui.components.TarjetaVenta
import com.example.ventasapp.viewmodel.MisVentasViewModel


/** Historial de ventas del vendedor, con búsqueda por código y filtro por estado. */
@Composable
fun MisVentasScreen(
    viewModel: MisVentasViewModel,
    onNuevaVenta: () -> Unit,
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

    PantallaBase(
        titulo = "Mis ventas",
        onCerrarSesion = onCerrarSesion,
        snackbarHostState = snackbar,
        barraInferior = barraInferior,
        botonFlotante = {
            ExtendedFloatingActionButton(
                onClick = onNuevaVenta,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nueva venta") }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            PanelFiltros(filtros = filtros, onFiltrosChange = viewModel::onFiltrosChange)

            if (ventas.isEmpty()) {
                EstadoVacio(
                    icono = Icons.Default.Search,
                    titulo = if (filtros.hayFiltrosActivos) "Sin resultados" else "Aún no hay ventas",
                    mensaje = if (filtros.hayFiltrosActivos) "Prueba cambiando o limpiando los filtros."
                    else "Registra tu primera venta con el botón \"Nueva venta\"."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = ventas, key = { it.id }) { venta ->
                        // animateItem(): las tarjetas se reordenan o desaparecen con animación al filtrar.
                        TarjetaVenta(
                            venta = venta,
                            onClick = { onVerVenta(venta.id) },
                            modifier = Modifier.animateItem(),
                            // El botón solo aparece si la regla del dominio permite eliminar esta venta.
                            onEliminar = if (EstadoVenta.desde(venta.estado).puedeEliminar(Perfil.VENDEDOR)) {
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
