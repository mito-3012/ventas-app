package com.example.ventasapp

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ventasapp.navigation.AppNavigation
import com.example.ventasapp.ui.theme.VentasAppTheme
import java.util.Locale

/**
 * Única Activity de la app. Solo prepara el tema y entrega el control a AppNavigation.
 * No lleva Scaffold aquí porque cada pantalla define el suyo (con su propia barra superior).
 */
class MainActivity : ComponentActivity() {

    /**
     * Fuerza el idioma de la app a español (castellano). Así los textos que vienen de las librerías
     * (por ejemplo el calendario: "Select date" pasa a "Seleccionar fecha") salen en español aunque
     * el teléfono esté en inglés. Las pantallas del SISTEMA (app de cámara, ventana de permisos)
     * no dependen de la app: siguen el idioma del teléfono.
     */
    override fun attachBaseContext(newBase: Context) {
        val configuracion = Configuration(newBase.resources.configuration)
        configuracion.setLocale(Locale.forLanguageTag("es-ES"))
        super.attachBaseContext(newBase.createConfigurationContext(configuracion))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()   // la app se dibuja bajo la barra de estado y la de navegación
        setContent {
            VentasAppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation()
                }
            }
        }
    }
}
