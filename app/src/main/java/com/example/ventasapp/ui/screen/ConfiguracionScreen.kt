package com.example.ventasapp.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ventasapp.model.TipoConvenio
import com.example.ventasapp.ui.components.CampoFormulario
import com.example.ventasapp.ui.components.PantallaBase
import com.example.ventasapp.ui.components.TituloSeccion
import com.example.ventasapp.viewmodel.ConfiguracionViewModel



/** Configuración del administrador: porcentajes de comisión ficticios y limpieza de datos de prueba. */
@Composable
fun ConfiguracionScreen(
    viewModel: ConfiguracionViewModel,
    onCerrarSesion: () -> Unit,
    barraInferior: @Composable () -> Unit
) {
    val estado by viewModel.estado.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val haptica = LocalHapticFeedback.current
    var confirmarLimpieza by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.eventos.collect { evento ->
            if (evento.esError) haptica.performHapticFeedback(HapticFeedbackType.LongPress)
            snackbar.showSnackbar(evento.texto)
        }
    }

    PantallaBase(
        titulo = "Configuración",
        onCerrarSesion = onCerrarSesion,
        snackbarHostState = snackbar,
        barraInferior = barraInferior
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TituloSeccion("Comisiones (ficticias)", Icons.Default.AttachMoney)
            Text(
                "Porcentaje que se usa para calcular la comisión estimada de cada convenio. " +
                    "Es una simulación: no genera pagos ni efectos reales. Rango permitido: 0 a 30.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TipoConvenio.entries.forEach { tipo ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        CampoFormulario(
                            valor = estado.valores[tipo].orEmpty(),
                            onValorChange = { viewModel.onValorChange(tipo, it) },
                            etiqueta = "${tipo.etiqueta} (%)",
                            icono = Icons.Default.Settings,
                            error = estado.errores[tipo],
                            ayuda = "Ejemplo: 10 o 7,5",
                            teclado = KeyboardType.Decimal,
                            imeAction = ImeAction.Done,
                            onImeAction = { viewModel.guardar(tipo) }
                        )
                        Button(onClick = { viewModel.guardar(tipo) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Guardar porcentaje")
                        }
                    }
                }
            }

            TituloSeccion("Datos de prueba", Icons.Default.Delete)
            Text(
                "Elimina del teléfono todas las fotos de comprobantes ficticios adjuntadas durante las pruebas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = { confirmarLimpieza = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Text("  Eliminar respaldos de prueba")
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
