package com.example.ventasapp.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import com.example.ventasapp.model.Perfil
import com.example.ventasapp.ui.components.ItemNavegacion

/**
 * Rutas centralizadas (como sugiere la presentación 2.2.3: AppRoutes.kt).
 * Tener todas las rutas en un solo lugar evita errores de tipeo ("registro" vs "regitro") y
 * permite cambiar una ruta sin tener que buscarla por todo el proyecto.
 */
object Rutas {
    const val LOGIN = "login"
    const val REGISTRO_USUARIO = "registro_usuario"

    // Destinos principales del vendedor (barra inferior)
    const val INICIO = "inicio"
    const val MIS_VENTAS = "mis_ventas"

    // Destinos principales del encargado
    const val GESTION = "gestion"
    const val INDICADORES = "indicadores"

    // Destinos principales del administrador
    const val USUARIOS = "usuarios"
    const val CONFIGURACION = "configuracion"
    // Ruta interna para acceder a la pantalla de operaciones con la API REST.
    const val API = "api_pruebas"

    // Compartido por vendedor y encargado
    const val ALERTAS = "alertas"

    // Rutas con argumento: el valor entre llaves se reemplaza por el id de la venta.
    const val VENTA_FORM = "venta_form/{ventaId}"
    const val DETALLE = "detalle/{ventaId}"
    const val ARG_VENTA_ID = "ventaId"

    fun ventaForm(ventaId: Long = 0L) = "venta_form/$ventaId"
    fun detalle(ventaId: Long) = "detalle/$ventaId"

    /** Destinos que comparten barra inferior: entre ellos la transición es un simple desvanecido. */
    val destinosPrincipales = setOf(
        INICIO, MIS_VENTAS, GESTION, INDICADORES, USUARIOS, CONFIGURACION, ALERTAS, API
    )

    /** Primera pantalla que ve cada perfil al iniciar sesión. */
    fun inicioPara(perfil: Perfil): String = when (perfil) {
        Perfil.VENDEDOR -> INICIO
        Perfil.ENCARGADO -> GESTION
        Perfil.ADMINISTRADOR -> USUARIOS
    }

    /** Pestañas de la barra inferior según el perfil. */
    fun itemsPara(perfil: Perfil): List<ItemNavegacion> = when (perfil) {
        Perfil.VENDEDOR -> listOf(
            ItemNavegacion(INICIO, "Inicio", Icons.Default.Home),
            ItemNavegacion(MIS_VENTAS, "Mis ventas", Icons.Default.ShoppingCart),
            ItemNavegacion(ALERTAS, "Alertas", Icons.Default.Notifications, conInsignia = true)
        )
        Perfil.ENCARGADO -> listOf(
            ItemNavegacion(GESTION, "Gestión", Icons.Default.Inbox),
            ItemNavegacion(INDICADORES, "Indicadores", Icons.Default.BarChart),
            ItemNavegacion(ALERTAS, "Alertas", Icons.Default.Notifications, conInsignia = true)
        )
        Perfil.ADMINISTRADOR -> listOf(
            ItemNavegacion(USUARIOS, "Usuarios", Icons.Default.Group),
            ItemNavegacion(CONFIGURACION, "Configuración", Icons.Default.Settings),
            // Incorpora el acceso a la pantalla de publicaciones en la navegación del administrador.
            ItemNavegacion(API, "API", Icons.Default.Cloud)
        )
    }
}
