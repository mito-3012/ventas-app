package com.example.ventasapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.ventasapp.model.EstadoVenta

/*
 * PALETA DE LA APLICACIÓN
 * Se reemplazó el violeta que viene por defecto por una paleta azul petróleo (marca) con un acento
 * ámbar quemado. Cada color se declara una vez acá y el tema los reparte por toda la app, así que
 * si el equipo quiere otra paleta basta con cambiar estos valores.
 */

// ---- Tema claro ----
val PetroleoPrimario = Color(0xFF0B5D73)
val PetroleoContenedor = Color(0xFFC6E9F3)
val PetroleoSobreContenedor = Color(0xFF001F29)
val VerdeSecundario = Color(0xFF3E6B5E)
val VerdeContenedor = Color(0xFFC0EBDC)
val AmbarTerciario = Color(0xFFB4590A)
val AmbarContenedor = Color(0xFFFFDCC2)
val FondoClaro = Color(0xFFF5F9FA)
val TextoClaro = Color(0xFF0F2530)
val VariantClaro = Color(0xFFDCE5E9)
val TextoVariantClaro = Color(0xFF3F5560)
val BordeClaro = Color(0xFF70858F)
val ErrorClaro = Color(0xFFB3261E)

// ---- Tema oscuro ----
val PetroleoPrimarioOscuro = Color(0xFF7FD0EA)
val PetroleoContenedorOscuro = Color(0xFF004D63)
val VerdeSecundarioOscuro = Color(0xFFA5CFC0)
val AmbarTerciarioOscuro = Color(0xFFFFB782)
val FondoOscuro = Color(0xFF0D1B21)
val TextoOscuro = Color(0xFFDCE8ED)
val VariantOscuro = Color(0xFF3F5560)
val TextoVariantOscuro = Color(0xFFBFD0D7)
val BordeOscuro = Color(0xFF8A9EA7)
val ErrorOscuro = Color(0xFFFFB4AB)

/** Colores de una "píldora" de estado: texto/ícono y fondo suave. */
data class ColoresEstado(val texto: Color, val fondo: Color)

/**
 * Cada estado de la venta tiene su propio color para que se distinga de un vistazo.
 * Hay una variante clara y otra oscura para mantener buen contraste en ambos temas.
 */
@Composable
fun EstadoVenta.colores(): ColoresEstado {
    val oscuro = isSystemInDarkTheme()
    val texto = when (this) {
        EstadoVenta.BORRADOR -> if (oscuro) Color(0xFFB0BEC5) else Color(0xFF546E7A)
        EstadoVenta.PENDIENTE_REVISION -> if (oscuro) Color(0xFFFFCA6B) else Color(0xFF8A5A00)
        EstadoVenta.OBSERVADA -> if (oscuro) Color(0xFFFFB782) else Color(0xFFB4590A)
        EstadoVenta.CORREGIDA -> if (oscuro) Color(0xFF90CAF9) else Color(0xFF1565C0)
        EstadoVenta.VALIDADA -> if (oscuro) Color(0xFFA5D6A7) else Color(0xFF2E7D32)
        EstadoVenta.RECHAZADA -> if (oscuro) Color(0xFFFFB4AB) else Color(0xFFB3261E)
        EstadoVenta.GESTIONADA -> if (oscuro) Color(0xFF80CBC4) else Color(0xFF00695C)
    }
    return ColoresEstado(texto = texto, fondo = texto.copy(alpha = 0.14f))
}
