package com.example.ventasapp.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth



import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.ventasapp.util.Formato

/**
 * CAMPO DE FORMULARIO REUTILIZABLE
 *
 * Todos los formularios de la app usan este componente, así la retroalimentación visual es igual
 * en todas partes (rúbrica IE 2.1.2):
 *  - Ícono inicial que explica el campo.
 *  - Borde y etiqueta rojos cuando hay error (isError).
 *  - Ícono de advertencia al final cuando hay error.
 *  - Mensaje de error animado debajo (AnimatedContent).
 *  - Texto de ayuda cuando no hay error, para que el usuario sepa qué formato se espera.
 *
 * Es un componente "sin lógica": no valida nada. Solo recibe el error que decidió el ViewModel.
 */
@Composable
fun CampoFormulario(
    valor: String,
    onValorChange: (String) -> Unit,
    etiqueta: String,
    icono: ImageVector,
    modifier: Modifier = Modifier,
    error: String? = null,
    ayuda: String? = null,
    teclado: KeyboardType = KeyboardType.Text,
    capitalizacion: KeyboardCapitalization = KeyboardCapitalization.None,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    iconoFinal: (@Composable () -> Unit)? = null,
    lineas: Int = 1,
    soloLectura: Boolean = false,
    habilitado: Boolean = true
) {
    val foco = LocalFocusManager.current

    // Ícono de advertencia que aparece al final del campo cuando hay un error.
    val iconoError: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Campo con error",
            tint = MaterialTheme.colorScheme.error
        )
    }

    OutlinedTextField(
        value = valor,
        onValueChange = onValorChange,
        modifier = modifier.fillMaxWidth(),
        enabled = habilitado,
        readOnly = soloLectura,
        label = { Text(etiqueta) },
        leadingIcon = { Icon(imageVector = icono, contentDescription = null) },
        trailingIcon = iconoFinal ?: if (error != null) iconoError else null,
        isError = error != null,
        supportingText = {
            // AnimatedContent hace una transición suave entre "ayuda" y "mensaje de error".
            AnimatedContent(targetState = error, label = "mensajeDelCampo") { mensaje ->
                Text(
                    text = mensaje ?: ayuda ?: " ",
                    color = if (mensaje != null) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        visualTransformation = visualTransformation,
        singleLine = lineas == 1,
        minLines = lineas,
        maxLines = if (lineas == 1) 1 else lineas + 2,
        keyboardOptions = KeyboardOptions(
            keyboardType = teclado,
            capitalization = capitalizacion,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = { foco.moveFocus(FocusDirection.Next) },
            onDone = {
                foco.clearFocus()
                onImeAction?.invoke()
            }
        )
    )
}

/** Campo de contraseña con botón para mostrar u ocultar el texto. */
@Composable
fun CampoClave(
    valor: String,
    onValorChange: (String) -> Unit,
    etiqueta: String,
    mostrar: Boolean,
    onAlternarMostrar: () -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    ayuda: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null
) {
    CampoFormulario(
        valor = valor,
        onValorChange = onValorChange,
        etiqueta = etiqueta,
        icono = Icons.Default.Lock,
        modifier = modifier,
        error = error,
        ayuda = ayuda,
        teclado = KeyboardType.Password,
        imeAction = imeAction,
        onImeAction = onImeAction,
        // PasswordVisualTransformation reemplaza cada letra por un punto.
        visualTransformation = if (mostrar) VisualTransformation.None else PasswordVisualTransformation(),
        iconoFinal = {
            IconButton(onClick = onAlternarMostrar) {
                Icon(
                    imageVector = if (mostrar) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (mostrar) "Ocultar contraseña" else "Mostrar contraseña"
                )
            }
        }
    )
}

/**
 * Campo de selección (lista desplegable).
 *
 * Truco: un OutlinedTextField de solo lectura no deja pasar el clic, así que se pone encima una
 * capa transparente (Box con clickable) que abre el menú. El DropdownMenu queda anclado al Box.
 */
@Composable
fun CampoSeleccion(
    valor: String,
    etiqueta: String,
    icono: ImageVector,
    opciones: List<String>,
    onSeleccion: (Int) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    ayuda: String? = null,
    habilitado: Boolean = true
) {
    var expandido by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        CampoFormulario(
            valor = valor,
            onValorChange = {},
            etiqueta = etiqueta,
            icono = icono,
            error = error,
            ayuda = ayuda,
            soloLectura = true,
            habilitado = habilitado,
            iconoFinal = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Abrir lista") }
        )
        // Capa transparente que captura el toque.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(enabled = habilitado) { expandido = true }
        )
        DropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            opciones.forEachIndexed { indice, opcion ->
                DropdownMenuItem(
                    text = { Text(opcion) },
                    onClick = {
                        expandido = false
                        onSeleccion(indice)
                    }
                )
            }
        }
    }
}

/**
 * Campo de fecha con calendario (DatePicker de Material 3).
 * Las fechas futuras quedan deshabilitadas en el calendario (SelectableDates), y además el
 * ViewModel vuelve a validar la regla: la validación real nunca depende solo de la interfaz.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoFecha(
    fechaMillis: Long?,
    etiqueta: String,
    onFechaSeleccionada: (Long) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    ayuda: String? = null,
    limiteMaximo: Long? = Formato.hoyComoFechaUtc()
) {
    var abierto by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        CampoFormulario(
            valor = if (fechaMillis == null) "" else Formato.fechaOperacion(fechaMillis),
            onValorChange = {},
            etiqueta = etiqueta,
            icono = Icons.Default.DateRange,
            error = error,
            ayuda = ayuda,
            soloLectura = true,
            iconoFinal = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Abrir calendario") }
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { abierto = true }
        )
    }

    if (abierto) {
        val estadoCalendario = rememberDatePickerState(
            initialSelectedDateMillis = fechaMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    limiteMaximo == null || utcTimeMillis <= limiteMaximo
            }
        )
        DatePickerDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoCalendario.selectedDateMillis?.let(onFechaSeleccionada)
                    abierto = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { abierto = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = estadoCalendario)
        }
    }
}
