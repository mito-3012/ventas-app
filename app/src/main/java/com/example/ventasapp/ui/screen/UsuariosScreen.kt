package com.example.ventasapp.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ventasapp.model.NuevoUsuarioUiState
import com.example.ventasapp.model.Perfil
import com.example.ventasapp.model.data.local.UsuarioEntity
import com.example.ventasapp.ui.components.BotonCarga
import com.example.ventasapp.ui.components.CampoClave
import com.example.ventasapp.ui.components.CampoFormulario
import com.example.ventasapp.ui.components.CampoSeleccion
import com.example.ventasapp.ui.components.PantallaBase
import com.example.ventasapp.viewmodel.UsuariosViewModel


/**
 * Administración de usuarios: lista de cuentas, activar o desactivar, asignar perfil y crear
 * cuentas nuevas. La cuenta del propio administrador queda protegida (no puede desactivarse ni
 * cambiarse el perfil, para no dejar el sistema sin administrador).
 */
@Composable
fun UsuariosScreen(
    usuarioActualId: Long?,
    viewModel: UsuariosViewModel,
    onCerrarSesion: () -> Unit,
    barraInferior: @Composable () -> Unit
) {
    val usuarios by viewModel.usuarios.collectAsState()
    val formulario by viewModel.formulario.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val haptica = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        viewModel.eventos.collect { evento ->
            if (evento.esError) haptica.performHapticFeedback(HapticFeedbackType.LongPress)
            snackbar.showSnackbar(evento.texto)
        }
    }

    PantallaBase(
        titulo = "Usuarios",
        onCerrarSesion = onCerrarSesion,
        snackbarHostState = snackbar,
        barraInferior = barraInferior,
        botonFlotante = {
            ExtendedFloatingActionButton(
                onClick = viewModel::abrirFormulario,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nuevo usuario") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "${usuarios.size} usuario(s) registrados. Todos los datos son ficticios.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(items = usuarios, key = { it.id }) { usuario ->
                TarjetaUsuario(
                    usuario = usuario,
                    esElMismo = usuario.id == usuarioActualId,
                    onCambiarActivo = { viewModel.cambiarActivo(usuario.id, it) },
                    onCambiarPerfil = { viewModel.cambiarPerfil(usuario.id, it) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }

    if (formulario.visible) {
        DialogoNuevoUsuario(formulario, viewModel)
    }
}

@Composable
private fun TarjetaUsuario(
    usuario: UsuarioEntity,
    esElMismo: Boolean,
    onCambiarActivo: (Boolean) -> Unit,
    onCambiarPerfil: (Perfil) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuPerfil by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        usuario.nombre + if (esElMismo) "  (tú)" else "",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(usuario.correo, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(usuario.zona, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Switch(
                        checked = usuario.activo,
                        onCheckedChange = onCambiarActivo,
                        enabled = !esElMismo          // el administrador no puede desactivarse a sí mismo
                    )
                    Text(
                        if (usuario.activo) "Activo" else "Inactivo",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (usuario.activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }

            // Asignación de perfil mediante un menú desplegable.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                Box {
                    TextButton(onClick = { menuPerfil = true }, enabled = !esElMismo) {
                        Text("Perfil: ${Perfil.desde(usuario.perfil).etiqueta}")
                    }
                    DropdownMenu(expanded = menuPerfil, onDismissRequest = { menuPerfil = false }) {
                        Perfil.entries.forEach { perfil ->
                            DropdownMenuItem(
                                text = { Text(perfil.etiqueta) },
                                onClick = {
                                    menuPerfil = false
                                    onCambiarPerfil(perfil)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogoNuevoUsuario(formulario: NuevoUsuarioUiState, viewModel: UsuariosViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::cerrarFormulario,
        title = { Text("Nuevo usuario") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    "Usa datos ficticios. El usuario podrá iniciar sesión con este correo y esta contraseña.",
                    style = MaterialTheme.typography.bodySmall
                )
                CampoFormulario(
                    valor = formulario.nombre,
                    onValorChange = viewModel::onNombreChange,
                    etiqueta = "Nombre completo",
                    icono = Icons.Default.Person,
                    error = formulario.errores.nombre,
                    capitalizacion = KeyboardCapitalization.Words
                )
                CampoFormulario(
                    valor = formulario.correo,
                    onValorChange = viewModel::onCorreoChange,
                    etiqueta = "Correo electrónico",
                    icono = Icons.Default.Email,
                    error = formulario.errores.correo,
                    teclado = KeyboardType.Email
                )
                CampoFormulario(
                    valor = formulario.zona,
                    onValorChange = viewModel::onZonaChange,
                    etiqueta = "Zona o sucursal",
                    icono = Icons.Default.LocationOn,
                    error = formulario.errores.zona,
                    capitalizacion = KeyboardCapitalization.Words
                )
                CampoSeleccion(
                    valor = formulario.perfil.etiqueta,
                    etiqueta = "Perfil",
                    icono = Icons.Default.Person,
                    opciones = Perfil.entries.map { it.etiqueta },
                    onSeleccion = { viewModel.onPerfilChange(Perfil.entries[it]) }
                )
                CampoClave(
                    valor = formulario.clave,
                    onValorChange = viewModel::onClaveChange,
                    etiqueta = "Contraseña",
                    mostrar = formulario.mostrarClave,
                    onAlternarMostrar = viewModel::alternarMostrarClave,
                    error = formulario.errores.clave,
                    ayuda = "Mínimo 6 caracteres, con letras y números",
                    imeAction = ImeAction.Done
                )
            }
        },
        confirmButton = {
            BotonCarga(texto = "Crear usuario", cargando = formulario.cargando, onClick = viewModel::crearUsuario)
        },
        dismissButton = { TextButton(onClick = viewModel::cerrarFormulario) { Text("Cancelar") } }
    )
}
