package com.example.ventasapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.ventasapp.model.data.local.VentasDatabase
import com.example.ventasapp.model.data.repository.SesionManager
import com.example.ventasapp.model.data.repository.VentasRepository
import java.util.Locale

/**
 * Clase Application: se crea una sola vez cuando parte el proceso de la app (por eso se declara
 * en el AndroidManifest con android:name=".VentasApplication").
 *
 * Aquí se construye el repositorio único que comparten todos los ViewModels. Es una forma simple
 * de "inyección de dependencias" sin librerías externas.
 */
class VentasApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Formatos de fecha y números del proceso en español, igual que los textos de la app.
        Locale.setDefault(Locale.forLanguageTag("es-ES"))
    }

    /** "by lazy": la base de datos recién se abre la primera vez que alguien usa el repositorio. */
    val repositorio: VentasRepository by lazy {
        VentasRepository(
            db = VentasDatabase.getDatabase(this),
            sesion = SesionManager(),
            contexto = applicationContext
        )
    }
}

/** Atajo para que cualquier AndroidViewModel obtenga el repositorio con: `repositorio`. */
val AndroidViewModel.repositorio: VentasRepository
    get() = getApplication<VentasApplication>().repositorio
