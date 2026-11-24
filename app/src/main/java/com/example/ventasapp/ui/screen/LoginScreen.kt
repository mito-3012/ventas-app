package com.example.ventasapp.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ventasapp.model.Perfil
import com.example.ventasapp.ui.components.BannerMensaje
import com.example.ventasapp.ui.components.BotonCarga
import com.example.ventasapp.ui.components.CampoClave
import com.example.ventasapp.ui.components.CampoFormulario
import com.example.ventasapp.viewmodel.LoginViewModel


/**
 * Pantalla de inicio de sesión.
 *
 * La pantalla NO decide si las credenciales son válidas: solo pinta el estado que le entrega el
 * LoginViewModel (collectAsState) y le avisa los eventos (onCorreoChange, iniciarSesion...).
 * Es exactamente el patrón de RegistroScreen de la guía 11.
 */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginExitoso: (Perfil) -> Unit,
    onIrARegistro: () -> Unit
) {
    val estado by viewModel.login.collectAsState()
    val haptica = LocalHapticFeedback.current

    // Recurso nativo: vibración suave cuando aparece un error de credenciales.
    LaunchedEffect(estado.errores.general) {
        if (estado.errores.general != null) haptica.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ---- Encabezado de marca ----
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
            Text(
                "Registro y validación de ventas",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Text(
                "Inicia sesión con tu cuenta ficticia para continuar",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // ---- Formulario ----
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    CampoFormulario(
                        valor = estado.correo,
                        onValorChange = viewModel::onCorreoChange,
                        etiqueta = "Correo electrónico",
                        icono = Icons.Default.Email,
                        error = estado.errores.correo,
                        teclado = KeyboardType.Email
                    )
                    CampoClave(
                        valor = estado.clave,
                        onValorChange = viewModel::onClaveChange,
                        etiqueta = "Contraseña",
                        mostrar = estado.mostrarClave,
                        onAlternarMostrar = viewModel::alternarMostrarClave,
                        error = estado.errores.clave,
                        imeAction = ImeAction.Done,
                        onImeAction = { viewModel.iniciarSesion(onLoginExitoso) }
                    )

                    // Error general (credenciales incorrectas, cuenta desactivada): aparece con animación.
                    AnimatedVisibility(visible = estado.errores.general != null) {
                        BannerMensaje(texto = estado.errores.general.orEmpty(), esError = true)
                    }

                    BotonCarga(
                        texto = "Entrar",
                        cargando = estado.cargando,
                        onClick = { viewModel.iniciarSesion(onLoginExitoso) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        icono = Icons.AutoMirrored.Filled.Send
                    )
                }
            }

            TextButton(onClick = onIrARegistro) {
                Text("¿No tienes cuenta? Crear cuenta de vendedor")
            }

            // ---- Ayuda para la demostración ----
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cuentas de demostración (ficticias)", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Pulsa una opción para rellenar el formulario automáticamente.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    // Un chip por perfil. La fila se desplaza horizontalmente si no caben todos.
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = { viewModel.autocompletar("vendedor1@demo.cl", "Ventas123") },
                            label = { Text("Vendedor") }
                        )
                        AssistChip(
                            onClick = { viewModel.autocompletar("encargado@demo.cl", "Gestion123") },
                            label = { Text("Encargado") }
                        )
                        AssistChip(
                            onClick = { viewModel.autocompletar("admin@demo.cl", "Admin12345") },
                            label = { Text("Administrador") }
                        )
                    }
                }
            }
        }
    }
}
