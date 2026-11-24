package com.example.ventasapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ventasapp.model.EstadoVenta
import com.example.ventasapp.model.FiltrosVentas
import com.example.ventasapp.model.data.local.UsuarioEntity

/**
 * Estructura base de TODAS las pantallas internas: barra superior con título, botón volver o
 * cerrar sesión, barra inferior opcional, botón flotante opcional y zona de mensajes (Snackbar).
 *
 * Usar siempre este componente garantiza coherencia visual entre vistas (rúbrica IE 2.1.1).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaBase(
    titulo: String,
    modifier: Modifier = Modifier,
    onVolver: (() -> Unit)? = null,
    onCerrarSesion: (() -> Unit)? = null,
    snackbarHostState: SnackbarHostState? = null,
    barraInferior: (@Composable () -> Unit)? = null,
    botonFlotante: (@Composable () -> Unit)? = null,
    contenido: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(titulo, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (onVolver != null) {
                        IconButton(onClick = onVolver) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    }
                },
                actions = {
                    if (onCerrarSesion != null) {
                        IconButton(onClick = onCerrarSesion) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar sesión")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = { barraInferior?.invoke() },
        floatingActionButton = { botonFlotante?.invoke() },
        snackbarHost = { if (snackbarHostState != null) SnackbarHost(snackbarHostState) },
        content = contenido
    )
}

/** Un destino de la barra inferior. [conInsignia] muestra el contador de alertas sin leer. */
data class ItemNavegacion(
    val ruta: String,
    val etiqueta: String,
    val icono: ImageVector,
    val conInsignia: Boolean = false
)

/** Barra de navegación inferior (Material 3 NavigationBar) para los destinos principales. */
@Composable
fun BarraInferior(
    items: List<ItemNavegacion>,
    rutaActual: String?,
    noLeidas: Int,
    onSeleccion: (String) -> Unit
) {
    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = rutaActual == item.ruta,
                onClick = { onSeleccion(item.ruta) },
                icon = {
                    if (item.conInsignia && noLeidas > 0) {
                        BadgedBox(badge = { Badge { Text(noLeidas.toString()) } }) {
                            Icon(item.icono, contentDescription = item.etiqueta)
                        }
                    } else {
                        Icon(item.icono, contentDescription = item.etiqueta)
                    }
                },
                label = { Text(item.etiqueta) }
            )
        }
    }
}

/**
 * Panel de búsqueda y filtros de ventas.
 *  - Buscador por código (siempre visible).
 *  - Chips por estado (siempre visibles).
 *  - Si se entrega la lista de [vendedores] (perfil encargado), aparece "Más filtros" con
 *    selector de vendedor y fecha.
 *
 * Es un componente "sin estado propio": recibe los filtros y avisa cada cambio con
 * [onFiltrosChange]; el ViewModel es quien los guarda.
 */
@Composable
fun PanelFiltros(
    filtros: FiltrosVentas,
    onFiltrosChange: (FiltrosVentas) -> Unit,
    modifier: Modifier = Modifier,
    vendedores: List<UsuarioEntity>? = null
) {
    var masFiltros by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OutlinedTextField(
            value = filtros.texto,
            onValueChange = { nuevo ->
                val limpio = nuevo.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }.take(12)
                onFiltrosChange(filtros.copy(texto = limpio))
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Buscar por código de autorización") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (filtros.texto.isNotEmpty()) {
                    IconButton(onClick = { onFiltrosChange(filtros.copy(texto = "")) }) {
                        Icon(Icons.Default.Clear, contentDescription = "Borrar búsqueda")
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = filtros.estado == null,
                    onClick = { onFiltrosChange(filtros.copy(estado = null)) },
                    label = { Text("Todas") }
                )
            }
            items(EstadoVenta.entries.toList()) { estado ->
                FilterChip(
                    selected = filtros.estado == estado,
                    onClick = {
                        onFiltrosChange(filtros.copy(estado = if (filtros.estado == estado) null else estado))
                    },
                    label = { Text(estado.etiqueta) }
                )
            }
        }

        if (vendedores != null) {
            TextButton(onClick = { masFiltros = !masFiltros }) {
                Icon(Icons.Default.FilterList, contentDescription = null)
                Text(if (masFiltros) "  Ocultar filtros" else "  Más filtros (vendedor y fecha)")
            }
            AnimatedVisibility(visible = masFiltros) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val todos = "Todos los vendedores"
                    CampoSeleccion(
                        valor = vendedores.firstOrNull { it.id == filtros.vendedorId }?.nombre ?: todos,
                        etiqueta = "Vendedor",
                        icono = Icons.Default.Person,
                        opciones = listOf(todos) + vendedores.map { it.nombre },
                        onSeleccion = { indice ->
                            val id = if (indice == 0) null else vendedores[indice - 1].id
                            onFiltrosChange(filtros.copy(vendedorId = id))
                        }
                    )
                    CampoFecha(
                        fechaMillis = filtros.fechaOperacion,
                        etiqueta = "Fecha de la operación",
                        onFechaSeleccionada = { onFiltrosChange(filtros.copy(fechaOperacion = it)) }
                    )
                    if (filtros.fechaOperacion != null) {
                        TextButton(onClick = { onFiltrosChange(filtros.copy(fechaOperacion = null)) }) {
                            Text("Quitar filtro de fecha")
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = filtros.hayFiltrosActivos) {
            TextButton(onClick = { onFiltrosChange(FiltrosVentas()) }, modifier = Modifier.fillMaxWidth()) {
                Text("Limpiar todos los filtros")
            }
        }
    }
}
