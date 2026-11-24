package com.example.ventasapp.ui.components

import android.content.ContentUris
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * GALERÍA PROPIA (pantalla completa) para elegir la foto del respaldo.
 *
 * Por qué existe: el selector de fotos que trae Android es una pantalla del SISTEMA y muestra sus
 * textos en el idioma del teléfono (por eso salía en inglés). Esta galería es parte de la app, así
 * que todos sus textos están en español, sin importar el idioma del dispositivo.
 *
 * Necesita el permiso de lectura de imágenes (se pide en VentaFormScreen antes de abrirla).
 */
@Composable
fun SelectorGaleria(onElegida: (Uri) -> Unit, onCerrar: () -> Unit) {
    val contexto = LocalContext.current

    // null = todavía cargando; lista vacía = no hay imágenes en el dispositivo.
    val imagenes by produceState<List<Uri>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { consultarImagenes(contexto) }
    }

    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false)   // ocupa toda la pantalla
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ---- Barra superior propia ----
                Surface(color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onCerrar) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar galería")
                        }
                        Text("Elegir imagen de la galería", style = MaterialTheme.typography.titleLarge)
                    }
                }

                val lista = imagenes
                when {
                    lista == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator()
                            Text("Cargando imágenes...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    lista.isEmpty() -> EstadoVacio(
                        icono = Icons.Default.Image,
                        titulo = "No hay imágenes",
                        mensaje = "No se encontraron fotos en este dispositivo. Toma una con la cámara o guarda " +
                            "una imagen en el teléfono y vuelve a intentarlo.",
                        modifier = Modifier.padding(top = 48.dp)
                    )

                    else -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 110.dp),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(items = lista, key = { it.toString() }) { uri ->
                            Miniatura(uri = uri, onClick = { onElegida(uri) })
                        }
                    }
                }
            }
        }
    }
}

/** Una miniatura cuadrada. Se decodifica reducida (inSampleSize) para no gastar memoria. */
@Composable
private fun Miniatura(uri: Uri, onClick: () -> Unit) {
    val contexto = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = uri) {
        value = withContext(Dispatchers.IO) {
            try {
                val opciones = BitmapFactory.Options().apply { inSampleSize = 8 }
                contexto.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, opciones)?.asImageBitmap()
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val imagen = bitmap
        if (imagen != null) {
            Image(
                bitmap = imagen,
                contentDescription = "Imagen de la galería",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

/** Lee de MediaStore las imágenes del dispositivo, las más recientes primero (máximo 300). */
private fun consultarImagenes(contexto: Context, limite: Int = 300): List<Uri> {
    val coleccion = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val lista = mutableListOf<Uri>()
    try {
        contexto.contentResolver.query(
            coleccion,
            arrayOf(MediaStore.Images.Media._ID),
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val columnaId = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext() && lista.size < limite) {
                lista.add(ContentUris.withAppendedId(coleccion, cursor.getLong(columnaId)))
            }
        }
    } catch (e: Exception) {
        // Sin permiso o sin almacenamiento: se devuelve la lista vacía y la pantalla lo informa.
    }
    return lista
}
