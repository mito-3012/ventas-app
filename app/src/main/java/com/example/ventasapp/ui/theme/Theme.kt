package com.example.ventasapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EsquemaClaro = lightColorScheme(
    primary = PetroleoPrimario,
    onPrimary = Color.White,
    primaryContainer = PetroleoContenedor,
    onPrimaryContainer = PetroleoSobreContenedor,
    secondary = VerdeSecundario,
    onSecondary = Color.White,
    secondaryContainer = VerdeContenedor,
    onSecondaryContainer = Color(0xFF00201A),
    tertiary = AmbarTerciario,
    onTertiary = Color.White,
    tertiaryContainer = AmbarContenedor,
    onTertiaryContainer = Color(0xFF2E1500),
    background = FondoClaro,
    onBackground = TextoClaro,
    surface = FondoClaro,
    onSurface = TextoClaro,
    surfaceVariant = VariantClaro,
    onSurfaceVariant = TextoVariantClaro,
    outline = BordeClaro,
    error = ErrorClaro,
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    // Los "surfaceContainer" se definen a mano; si no, Material usa tonos violeta por defecto.
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFEFF5F7),
    surfaceContainer = Color(0xFFE9F0F3),
    surfaceContainerHigh = Color(0xFFE3EBEE),
    surfaceContainerHighest = Color(0xFFDDE6EA)
)

private val EsquemaOscuro = darkColorScheme(
    primary = PetroleoPrimarioOscuro,
    onPrimary = Color(0xFF003544),
    primaryContainer = PetroleoContenedorOscuro,
    onPrimaryContainer = PetroleoContenedor,
    secondary = VerdeSecundarioOscuro,
    onSecondary = Color(0xFF00382E),
    secondaryContainer = Color(0xFF254E43),
    onSecondaryContainer = VerdeContenedor,
    tertiary = AmbarTerciarioOscuro,
    onTertiary = Color(0xFF4C2600),
    tertiaryContainer = Color(0xFF6B3A00),
    onTertiaryContainer = AmbarContenedor,
    background = FondoOscuro,
    onBackground = TextoOscuro,
    surface = FondoOscuro,
    onSurface = TextoOscuro,
    surfaceVariant = VariantOscuro,
    onSurfaceVariant = TextoVariantOscuro,
    outline = BordeOscuro,
    error = ErrorOscuro,
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    surfaceContainerLowest = Color(0xFF08141A),
    surfaceContainerLow = Color(0xFF121F26),
    surfaceContainer = Color(0xFF16252C),
    surfaceContainerHigh = Color(0xFF1F2F37),
    surfaceContainerHighest = Color(0xFF2A3A42)
)

/**
 * Tema general. A diferencia del proyecto base, aquí NO se usa el color dinámico de Android 12+
 * (dynamicColor), porque ese modo ignora la paleta propia y toma los colores del fondo de pantalla.
 */
@Composable
fun VentasAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) EsquemaOscuro else EsquemaClaro,
        typography = Typography,
        content = content
    )
}
