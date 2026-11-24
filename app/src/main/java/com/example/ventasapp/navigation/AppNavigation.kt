package com.example.ventasapp.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ventasapp.model.EstadoVenta
import com.example.ventasapp.model.Perfil
import com.example.ventasapp.model.data.local.UsuarioEntity
import com.example.ventasapp.ui.components.BarraInferior
import com.example.ventasapp.ui.components.EstadoVacio
import com.example.ventasapp.ui.screen.AlertasScreen
import com.example.ventasapp.ui.screen.ConfiguracionScreen
import com.example.ventasapp.ui.screen.DetalleVentaScreen
import com.example.ventasapp.ui.screen.GestionScreen
import com.example.ventasapp.ui.screen.IndicadoresScreen
import com.example.ventasapp.ui.screen.InicioVendedorScreen
import com.example.ventasapp.ui.screen.LoginScreen
import com.example.ventasapp.ui.screen.MisVentasScreen
import com.example.ventasapp.ui.screen.PostScreen
import com.example.ventasapp.ui.screen.RegistroUsuarioScreen
import com.example.ventasapp.ui.screen.UsuariosScreen
import com.example.ventasapp.ui.screen.VentaFormScreen
import com.example.ventasapp.viewmodel.AlertasViewModel
import com.example.ventasapp.viewmodel.ConfiguracionViewModel
import com.example.ventasapp.viewmodel.DetalleVentaViewModel
import com.example.ventasapp.viewmodel.GestionViewModel
import com.example.ventasapp.viewmodel.LoginViewModel
import com.example.ventasapp.viewmodel.MisVentasViewModel
import com.example.ventasapp.viewmodel.PostViewModel
import com.example.ventasapp.viewmodel.RegistroUsuarioViewModel
import com.example.ventasapp.viewmodel.UsuariosViewModel
import com.example.ventasapp.viewmodel.VentaFormViewModel


private const val DURACION_MS = 300

/**
 * Navegación de toda la aplicación (NavHost + NavController).
 *
 * Igual que en la guía 11, los ViewModels se crean UNA sola vez aquí arriba y se reparten a las
 * pantallas. Por eso un dato escrito en una pantalla está disponible en otra sin pasar argumentos
 * por la ruta (solo se pasa el id de la venta, que es lo que identifica el registro).
 *
 * Hay tres perfiles (vendedor, encargado y administrador). Cada uno tiene su propia
 * pantalla de inicio y su propia barra inferior, y las pantallas se protegen con [SoloPara].
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // ViewModels compartidos (una instancia por Activity).
    val loginVM: LoginViewModel = viewModel()
    val registroUsuarioVM: RegistroUsuarioViewModel = viewModel()
    val ventaFormVM: VentaFormViewModel = viewModel()
    val misVentasVM: MisVentasViewModel = viewModel()
    val gestionVM: GestionViewModel = viewModel()
    val detalleVM: DetalleVentaViewModel = viewModel()
    val alertasVM: AlertasViewModel = viewModel()
    val usuariosVM: UsuariosViewModel = viewModel()
    val configuracionVM: ConfiguracionViewModel = viewModel()
    // Mantiene el ViewModel de publicaciones asociado al ciclo de vida de la navegación.
    val postVM: PostViewModel = viewModel()

    val usuario by loginVM.usuario.collectAsState()
    val entradaActual by navController.currentBackStackEntryAsState()
    val rutaActual = entradaActual?.destination?.route

    /**
     * Guardia de sesión: si no hay usuario conectado y la persona está en una pantalla interna
     * (por ejemplo, después de cerrar sesión o si Android restauró la pila de navegación tras cerrar
     * el proceso), se la devuelve al login y se limpia el historial de pantallas.
     */
    LaunchedEffect(usuario, rutaActual) {
        val enPantallaPublica = rutaActual == Rutas.LOGIN || rutaActual == Rutas.REGISTRO_USUARIO
        if (usuario == null && rutaActual != null && !enPantallaPublica) {
            navController.navigate(Rutas.LOGIN) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val cerrarSesion: () -> Unit = { loginVM.cerrarSesion() }
    val volver: () -> Unit = { navController.popBackStack() }
    val abrirDetalle: (Long) -> Unit = { id -> navController.navigate(Rutas.detalle(id)) }

    /** Barra inferior de los destinos principales; las pestañas dependen del perfil. */
    val barraInferior: @Composable () -> Unit = {
        val noLeidas by alertasVM.noLeidas.collectAsState()
        val perfil = usuario?.let { Perfil.desde(it.perfil) } ?: Perfil.VENDEDOR
        BarraInferior(items = Rutas.itemsPara(perfil), rutaActual = rutaActual, noLeidas = noLeidas) { destino ->
            navegarADestinoPrincipal(navController, destino, Rutas.inicioPara(perfil))
        }
    }

    NavHost(
        navController = navController,
        startDestination = Rutas.LOGIN,
        // Entre destinos principales: desvanecido. Al avanzar a otra pantalla: desliza desde la derecha.
        enterTransition = {
            if (esPrincipal(initialState) && esPrincipal(targetState)) fadeIn(tween(200))
            else slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(DURACION_MS)) +
                fadeIn(tween(DURACION_MS))
        },
        exitTransition = {
            if (esPrincipal(initialState) && esPrincipal(targetState)) fadeOut(tween(200))
            else slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(DURACION_MS)) +
                fadeOut(tween(DURACION_MS))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(DURACION_MS)) +
                fadeIn(tween(DURACION_MS))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(DURACION_MS)) +
                fadeOut(tween(DURACION_MS))
        }
    ) {
        // ------------------------------------------------------------ Acceso
        composable(Rutas.LOGIN) {
            LoginScreen(
                viewModel = loginVM,
                onLoginExitoso = { perfil ->
                    navController.navigate(Rutas.inicioPara(perfil)) {
                        // Se saca el login de la pila: con "atrás" no se vuelve a la pantalla de acceso.
                        popUpTo(Rutas.LOGIN) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onIrARegistro = { navController.navigate(Rutas.REGISTRO_USUARIO) }
            )
        }
        composable(Rutas.REGISTRO_USUARIO) {
            RegistroUsuarioScreen(viewModel = registroUsuarioVM, onVolver = volver)
        }

        // ------------------------------------------------------------ Perfil vendedor
        composable(Rutas.INICIO) {
            SoloPara(usuario, Perfil.VENDEDOR) {
                InicioVendedorScreen(
                    usuario = usuario,
                    viewModel = misVentasVM,
                    onNuevaVenta = { navController.navigate(Rutas.ventaForm(0L)) },
                    onVerObservadas = {
                        misVentasVM.onFiltrosChange(misVentasVM.filtros.value.copy(estado = EstadoVenta.OBSERVADA))
                        navegarADestinoPrincipal(navController, Rutas.MIS_VENTAS, Rutas.INICIO)
                    },
                    onVerVenta = abrirDetalle,
                    onCerrarSesion = cerrarSesion,
                    barraInferior = barraInferior
                )
            }
        }
        composable(Rutas.MIS_VENTAS) {
            SoloPara(usuario, Perfil.VENDEDOR) {
                MisVentasScreen(
                    viewModel = misVentasVM,
                    onNuevaVenta = { navController.navigate(Rutas.ventaForm(0L)) },
                    onVerVenta = abrirDetalle,
                    onCerrarSesion = cerrarSesion,
                    barraInferior = barraInferior
                )
            }
        }
        composable(
            route = Rutas.VENTA_FORM,
            arguments = listOf(navArgument(Rutas.ARG_VENTA_ID) { type = NavType.LongType; defaultValue = 0L })
        ) { entrada ->
            val ventaId = entrada.arguments?.getLong(Rutas.ARG_VENTA_ID) ?: 0L
            SoloPara(usuario, Perfil.VENDEDOR) {
                VentaFormScreen(ventaId = ventaId, viewModel = ventaFormVM, onVolver = volver)
            }
        }

        // ------------------------------------------------------------ Perfil encargado
        composable(Rutas.GESTION) {
            SoloPara(usuario, Perfil.ENCARGADO) {
                GestionScreen(
                    viewModel = gestionVM,
                    onVerVenta = abrirDetalle,
                    onCerrarSesion = cerrarSesion,
                    barraInferior = barraInferior
                )
            }
        }
        // Los indicadores los ve el encargado, que también limpia las fotos de prueba.
        composable(Rutas.INDICADORES) {
            SoloPara(usuario, Perfil.ENCARGADO) {
                IndicadoresScreen(
                    viewModel = gestionVM,
                    onCerrarSesion = cerrarSesion,
                    barraInferior = barraInferior,
                    puedeLimpiarRespaldos = usuario?.let { Perfil.desde(it.perfil) } == Perfil.ENCARGADO
                )
            }
        }

        // ------------------------------------------------------------ Perfil administrador
        composable(Rutas.USUARIOS) {
            SoloPara(usuario, Perfil.ADMINISTRADOR) {
                UsuariosScreen(
                    usuarioActualId = usuario?.id,
                    viewModel = usuariosVM,
                    onCerrarSesion = cerrarSesion,
                    barraInferior = barraInferior
                )
            }
        }
        composable(Rutas.CONFIGURACION) {
            SoloPara(usuario, Perfil.ADMINISTRADOR) {
                ConfiguracionScreen(
                    viewModel = configuracionVM,
                    onCerrarSesion = cerrarSesion,
                    barraInferior = barraInferior
                )
            }
        }

        // La pantalla de pruebas de API queda restringida al perfil administrador.
        // ------------------------------------------------------------ API REST (administrador)
        composable(Rutas.API) {
            SoloPara(usuario, Perfil.ADMINISTRADOR) {
                PostScreen(
                    viewModel = postVM,
                    onCerrarSesion = cerrarSesion,
                    barraInferior = barraInferior
                )
            }
        }

        // ------------------------------------------------------------ Compartidas
        composable(Rutas.ALERTAS) {
            SoloPara(usuario, Perfil.VENDEDOR, Perfil.ENCARGADO) {
                AlertasScreen(
                    viewModel = alertasVM,
                    onVerVenta = abrirDetalle,
                    onCerrarSesion = cerrarSesion,
                    barraInferior = barraInferior
                )
            }
        }
        composable(
            route = Rutas.DETALLE,
            arguments = listOf(navArgument(Rutas.ARG_VENTA_ID) { type = NavType.LongType; defaultValue = 0L })
        ) { entrada ->
            val ventaId = entrada.arguments?.getLong(Rutas.ARG_VENTA_ID) ?: 0L
            // El administrador no ve ventas.
            SoloPara(usuario, Perfil.VENDEDOR, Perfil.ENCARGADO) {
                DetalleVentaScreen(
                    ventaId = ventaId,
                    usuario = usuario,
                    viewModel = detalleVM,
                    onVolver = volver,
                    onEditar = { id -> navController.navigate(Rutas.ventaForm(id)) }
                )
            }
        }
    }
}

/** true si la pantalla de la entrada pertenece a la barra inferior. */
private fun esPrincipal(entrada: NavBackStackEntry): Boolean {
    val ruta = entrada.destination.route
    return ruta != null && ruta in Rutas.destinosPrincipales
}

/**
 * Cambio de pestaña estándar de Material:
 *  - popUpTo(inicio) + saveState: evita apilar pestañas y recuerda su estado.
 *  - launchSingleTop: no crea otra copia si ya estás ahí.
 *  - restoreState: al volver a una pestaña, recupera lo que tenía.
 */
private fun navegarADestinoPrincipal(navController: NavController, destino: String, inicio: String) {
    navController.navigate(destino) {
        popUpTo(inicio) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Control de acceso por perfil a nivel de navegación. Además del control del repositorio, una
 * pantalla se muestra solo a los perfiles indicados, aunque se llegara a navegar a ella.
 */
@Composable
private fun SoloPara(usuario: UsuarioEntity?, vararg perfiles: Perfil, contenido: @Composable () -> Unit) {
    when {
        usuario == null -> Box(Modifier.fillMaxSize())   // transición de cierre de sesión: pantalla vacía
        Perfil.desde(usuario.perfil) in perfiles -> contenido()
        else -> EstadoVacio(
            icono = Icons.Default.Warning,
            titulo = "Acceso no autorizado",
            mensaje = "Tu perfil no tiene permiso para ver esta pantalla."
        )
    }
}
