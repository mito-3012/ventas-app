package com.example.ventasapp.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ventasapp.model.EstadoVenta
import com.example.ventasapp.model.TipoConvenio
import com.example.ventasapp.model.data.local.VentaEntity
import com.example.ventasapp.ui.theme.EstiloCodigo
import com.example.ventasapp.ui.theme.colores
import com.example.ventasapp.util.Formato
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Ícono asociado a cada estado (refuerza el color: no depende solo del color para entenderse). */
fun EstadoVenta.icono(): ImageVector = when (this) {
    EstadoVenta.BORRADOR -> Icons.Default.Edit
    EstadoVenta.PENDIENTE_REVISION -> Icons.Default.Schedule
    EstadoVenta.OBSERVADA -> Icons.Default.Warning
    EstadoVenta.CORREGIDA -> Icons.Default.Refresh
    EstadoVenta.VALIDADA -> Icons.Default.CheckCircle
    EstadoVenta.RECHAZADA -> Icons.Default.Cancel
    EstadoVenta.GESTIONADA -> Icons.AutoMirrored.Filled.Send
}

/**
 * Píldora que muestra el estado de una venta.
 * animateColorAsState hace que el color cambie con una transición suave cuando la venta cambia
 * de estado (por ejemplo de Pendiente a Validada) en vez de saltar de golpe.
 */
@Composable
fun EtiquetaEstado(estado: EstadoVenta, modifier: Modifier = Modifier) {
    val colores = estado.colores()
    val colorTexto by animateColorAsState(colores.texto, tween(400), label = "colorTextoEstado")
    val colorFondo by animateColorAsState(colores.fondo, tween(400), label = "colorFondoEstado")

    Surface(modifier = modifier, shape = RoundedCornerShape(50), color = colorFondo) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(estado.icono(), contentDescription = null, tint = colorTexto, modifier = Modifier.size(14.dp))
            Text(estado.etiqueta, color = colorTexto, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

/** Tarjeta de una venta para las listas (vendedor y encargado). */
@Composable
fun TarjetaVenta(
    venta: VentaEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    mostrarVendedor: Boolean = false,
    /** Si no es null, la tarjeta muestra el botón "Eliminar" (solo cuando el perfil puede eliminar esa venta). */
    onEliminar: (() -> Unit)? = null
) {
    val estado = EstadoVenta.desde(venta.estado)
    val tipo = TipoConvenio.desde(venta.tipoConvenio)

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),   // si la tarjeta cambia de tamaño, lo hace con animación
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = venta.codigoAutorizacion.ifBlank { "Sin código" },
                    style = EstiloCodigo,
                    color = MaterialTheme.colorScheme.primary
                )
                EtiquetaEstado(estado)
            }
            Text(tipo?.etiqueta ?: "Convenio sin definir", style = MaterialTheme.typography.titleMedium)
            if (venta.prestador.isNotBlank()) {
                Text(
                    venta.prestador,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (venta.monto > 0) Formato.pesos(venta.monto) else "Sin monto",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = Formato.fechaOperacion(venta.fechaOperacion),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (mostrarVendedor) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(venta.vendedorNombre, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (onEliminar != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = onEliminar,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Eliminar")
                    }
                }
            }
        }
    }
}

/**
 * Diálogo de confirmación para eliminar una venta. Lo comparten las listas (vendedor y encargado)
 * y el detalle, así el mensaje es siempre el mismo y deja claro que se borra también la foto.
 */
@Composable
fun DialogoEliminarVenta(venta: VentaEntity, onConfirmar: () -> Unit, onCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancelar,
        icon = { Icon(Icons.Default.Delete, contentDescription = null) },
        title = { Text("Eliminar venta") },
        text = {
            Text(
                "Se eliminará la venta ${venta.codigoAutorizacion.ifBlank { "sin código" }} junto con su " +
                    "historial y su foto de respaldo. Esta acción no se puede deshacer."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirmar) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

/**
 * Muestra el respaldo fotográfico desde el archivo privado de la app.
 * produceState decodifica la imagen en un hilo de IO y entrega el resultado cuando está lista,
 * así la pantalla no se congela al abrir fotos.
 */
@Composable
fun ImagenRespaldo(ruta: String?, modifier: Modifier = Modifier) {
    val imagen by produceState<ImageBitmap?>(initialValue = null, key1 = ruta) {
        value = if (ruta == null) null else withContext(Dispatchers.IO) {
            BitmapFactory.decodeFile(ruta)?.asImageBitmap()
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = imagen
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Respaldo fotográfico del comprobante ficticio",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                Text(
                    text = if (ruta == null) "Sin respaldo adjunto" else "Cargando imagen...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Pantalla vacía reutilizable: ícono + título + explicación. */
@Composable
fun EstadoVacio(icono: ImageVector, titulo: String, mensaje: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icono, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.outline)
        Text(titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            mensaje,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** Mensaje destacado dentro de la pantalla (error, advertencia o información). */
@Composable
fun BannerMensaje(texto: String, modifier: Modifier = Modifier, esError: Boolean = false) {
    val fondo = if (esError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
    val contenido = if (esError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = fondo, contentColor = contenido) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(if (esError) Icons.Default.Warning else Icons.Default.Info, contentDescription = null)
            Text(texto, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Título de sección dentro de un formulario o detalle. */
@Composable
fun TituloSeccion(texto: String, icono: ImageVector, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(texto, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}

/** Fila "etiqueta: valor" para las pantallas de detalle. */
@Composable
fun FilaDato(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp)
        )
        Text(valor, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

/**
 * Tarjeta con un número grande (indicadores). El número se anima: cuando el valor cambia,
 * "cuenta" desde el valor anterior hasta el nuevo (animateIntAsState).
 */
@Composable
fun TarjetaNumero(
    titulo: String,
    valor: Int,
    icono: ImageVector,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    formatoPesos: Boolean = false
) {
    val valorAnimado by animateIntAsState(targetValue = valor, animationSpec = tween(700), label = "numeroAnimado")
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icono, contentDescription = null, tint = color)
            Text(
                text = if (formatoPesos) Formato.pesos(valorAnimado) else valorAnimado.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = color
            )
            Text(titulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Botón con estado de carga: mientras se ejecuta la acción, el texto se reemplaza (con animación)
 * por un indicador de progreso y el botón queda deshabilitado, para evitar doble envío.
 */
@Composable
fun BotonCarga(
    texto: String,
    cargando: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    habilitado: Boolean = true
) {
    Button(onClick = onClick, enabled = habilitado && !cargando, modifier = modifier) {
        AnimatedContent(targetState = cargando, label = "contenidoBoton") { enCurso ->
            if (enCurso) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icono != null) {
                        Icon(icono, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(texto)
                }
            }
        }
    }
}
