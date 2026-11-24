package com.example.ventasapp.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ventasapp.ui.components.BotonCarga
import com.example.ventasapp.ui.components.CampoClave
import com.example.ventasapp.ui.components.CampoFormulario
import com.example.ventasapp.ui.components.PantallaBase

import com.example.ventasapp.viewmodel.RegistroUsuarioViewModel


/**
 * Formulario "Crear cuenta de vendedor". Es la evolución de RegistroScreen de la guía 11:
 * mismos componentes (OutlinedTextField, PasswordVisualTransformation, Checkbox, Button) pero con
 * más validaciones, íconos y confirmación animada al terminar.
 */
@Composable
fun RegistroUsuarioScreen(
    viewModel: RegistroUsuarioViewModel,
    onVolver: () -> Unit
) {
    val estado by viewModel.estado.collectAsState()
    var cuentaCreada by remember { mutableStateOf(false) }

    // Al entrar a la pantalla se parte con el formulario limpio.
    LaunchedEffect(Unit) { viewModel.reiniciar() }

    PantallaBase(titulo = "Crear cuenta de vendedor", onVolver = onVolver) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Los datos de esta cuenta son de prueba. Usa nombres y correos ficticios.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            CampoFormulario(
                valor = estado.nombre,
                onValorChange = viewModel::onNombreChange,
                etiqueta = "Nombre completo",
                icono = Icons.Default.Person,
                error = estado.errores.nombre,
                capitalizacion = KeyboardCapitalization.Words
            )
            CampoFormulario(
                valor = estado.correo,
                onValorChange = viewModel::onCorreoChange,
                etiqueta = "Correo electrónico",
                icono = Icons.Default.Email,
                error = estado.errores.correo,
                ayuda = "Se usará para iniciar sesión",
                teclado = KeyboardType.Email
            )
            CampoFormulario(
                valor = estado.zona,
                onValorChange = viewModel::onZonaChange,
                etiqueta = "Zona o sucursal",
                icono = Icons.Default.LocationOn,
                error = estado.errores.zona,
                capitalizacion = KeyboardCapitalization.Words
            )
            CampoClave(
                valor = estado.clave,
                onValorChange = viewModel::onClaveChange,
                etiqueta = "Contraseña",
                mostrar = estado.mostrarClave,
                onAlternarMostrar = viewModel::alternarMostrarClave,
                error = estado.errores.clave,
                ayuda = "Mínimo 6 caracteres, con letras y números"
            )
            CampoClave(
                valor = estado.confirmarClave,
                onValorChange = viewModel::onConfirmarClaveChange,
                etiqueta = "Repetir contraseña",
                mostrar = estado.mostrarClave,
                onAlternarMostrar = viewModel::alternarMostrarClave,
                error = estado.errores.confirmarClave,
                imeAction = ImeAction.Done
            )

            // Checkbox de términos (en la guía 11 no era obligatorio; aquí sí lo es).
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = estado.aceptaTerminos, onCheckedChange = viewModel::onTerminosChange)
                Spacer(Modifier.width(8.dp))
                Text("Acepto los términos y condiciones de uso de prueba")
            }
            estado.errores.terminos?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            BotonCarga(
                texto = "Crear cuenta",
                cargando = estado.cargando,
                onClick = { viewModel.registrar { cuentaCreada = true } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )
        }
    }

    // Confirmación al terminar: dialogo con ícono de éxito.
    if (cuentaCreada) {
        AlertDialog(
            onDismissRequest = {},
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Cuenta creada") },
            text = { Text("Tu cuenta de vendedor quedó guardada en el dispositivo. Ya puedes iniciar sesión.") },
            confirmButton = {
                TextButton(onClick = {
                    cuentaCreada = false
                    onVolver()
                }) { Text("Ir a iniciar sesión") }
            }
        )
    }
}
