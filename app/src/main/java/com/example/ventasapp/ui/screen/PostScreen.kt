package com.example.ventasapp.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ventasapp.model.ModoFormularioPost
import com.example.ventasapp.model.Post
import com.example.ventasapp.model.PostFormUiState
import com.example.ventasapp.ui.components.PantallaBase
import com.example.ventasapp.viewmodel.PostViewModel


// Pantalla de administración que permite consultar y ejecutar operaciones CRUD sobre la API.
@Composable
fun PostScreen(
    viewModel: PostViewModel,
    onCerrarSesion: () -> Unit,
    barraInferior: @Composable () -> Unit
) {
    val estado by viewModel.estado.collectAsState()
    val formulario by viewModel.formulario.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var postAEliminar by remember { mutableIntStateOf(-1) }

    // Inicia la carga cuando se abre la pantalla y escucha los mensajes emitidos por el ViewModel.
    LaunchedEffect(Unit) {
        viewModel.cargarSiCorresponde()
        viewModel.eventos.collect { evento -> snackbar.showSnackbar(evento.texto) }
    }

    PantallaBase(
        titulo = "API REST · Publicaciones",
        onCerrarSesion = onCerrarSesion,
        snackbarHostState = snackbar,
        barraInferior = barraInferior,
        botonFlotante = {
            FloatingActionButton(onClick = viewModel::abrirFormularioCrear) {
                Icon(Icons.Default.Add, contentDescription = "Crear publicación")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("JSONPlaceholder", style = MaterialTheme.typography.titleMedium)
                    Text("GET · POST · PUT · PATCH · DELETE", style = MaterialTheme.typography.bodySmall)
                    Text("API de demostración: las escrituras no persisten en el servidor.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = viewModel::cargarPosts, enabled = !estado.cargando) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar lista")
                }
            }

            if (estado.cargando && estado.posts.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (estado.error != null && estado.posts.isEmpty()) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(estado.error ?: "No se pudieron cargar las publicaciones.", color = MaterialTheme.colorScheme.error)
                    Button(onClick = viewModel::cargarPosts, modifier = Modifier.padding(top = 8.dp)) { Text("Reintentar") }
                }
            } else {
                // Presenta las publicaciones en una lista desplazable para facilitar su revisión.
            LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(estado.posts, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            procesando = estado.procesandoId == post.id,
                            onReemplazar = { viewModel.abrirFormularioReemplazar(post) },
                            onActualizarTitulo = { viewModel.abrirFormularioActualizarTitulo(post) },
                            onEliminar = { postAEliminar = post.id }
                        )
                    }
                }
            }
        }
    }

    // El diálogo se muestra únicamente cuando el estado indica que el formulario está abierto.
    if (formulario.visible) {
        FormularioPostDialog(
            formulario = formulario,
            onTituloChange = viewModel::onTituloChange,
            onCuerpoChange = viewModel::onCuerpoChange,
            onGuardar = viewModel::guardarFormulario,
            onCerrar = viewModel::cerrarFormulario
        )
    }

    // Solicita confirmación antes de ejecutar una eliminación.
    if (postAEliminar >= 0) {
        AlertDialog(
            onDismissRequest = { postAEliminar = -1 },
            title = { Text("Eliminar publicación") },
            text = { Text("¿Seguro que deseas eliminar la publicación N.° $postAEliminar?") },
            confirmButton = {
                TextButton(onClick = { val id = postAEliminar; postAEliminar = -1; viewModel.eliminarPost(id) }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { postAEliminar = -1 }) { Text("Cancelar") } }
        )
    }
}

// Tarjeta que presenta los datos de una publicación y sus acciones disponibles.
@Composable
private fun PostCard(
    post: Post,
    procesando: Boolean,
    onReemplazar: () -> Unit,
    onActualizarTitulo: () -> Unit,
    onEliminar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Publicación #${post.id} · Usuario ${post.userId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(post.title, style = MaterialTheme.typography.titleMedium)
            Text(post.body, style = MaterialTheme.typography.bodyMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onActualizarTitulo, enabled = !procesando) {
                    Icon(Icons.Default.Edit, contentDescription = "Actualizar solo título")
                }
                IconButton(onClick = onReemplazar, enabled = !procesando) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reemplazar publicación completa")
                }
                IconButton(onClick = onEliminar, enabled = !procesando) {
                    if (procesando) CircularProgressIndicator() else Icon(Icons.Default.Delete, contentDescription = "Eliminar publicación", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// Formulario reutilizable para crear, reemplazar o actualizar parcialmente una publicación.
@Composable
private fun FormularioPostDialog(
    formulario: PostFormUiState,
    onTituloChange: (String) -> Unit,
    onCuerpoChange: (String) -> Unit,
    onGuardar: () -> Unit,
    onCerrar: () -> Unit
) {
    // En una operación PATCH se solicita únicamente el título; las demás operaciones requieren el contenido.
    val soloTitulo = formulario.modo == ModoFormularioPost.ACTUALIZAR_TITULO
    val tituloDialogo = when (formulario.modo) {
        ModoFormularioPost.CREAR -> "Nueva publicación"
        ModoFormularioPost.REEMPLAZAR -> "Reemplazar publicación"
        ModoFormularioPost.ACTUALIZAR_TITULO -> "Actualizar título (PATCH)"
    }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(tituloDialogo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = formulario.titulo,
                    onValueChange = onTituloChange,
                    label = { Text("Título") },
                    isError = formulario.errores.titulo != null,
                    supportingText = { Text(formulario.errores.titulo ?: "Obligatorio, máximo 120 caracteres") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !formulario.guardando
                )
                if (!soloTitulo) {
                    OutlinedTextField(
                        value = formulario.cuerpo,
                        onValueChange = onCuerpoChange,
                        label = { Text("Contenido") },
                        isError = formulario.errores.cuerpo != null,
                        supportingText = { Text(formulario.errores.cuerpo ?: "Obligatorio, máximo 2000 caracteres") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        enabled = !formulario.guardando
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onGuardar, enabled = !formulario.guardando) {
                if (formulario.guardando) CircularProgressIndicator() else Text("Guardar")
            }
        },
        dismissButton = { TextButton(onClick = onCerrar, enabled = !formulario.guardando) { Text("Cancelar") } }
    )
}
