package com.example.ventasapp.model.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Instancia única de Retrofit para comunicarse con JSONPlaceholder. */
object RetrofitInstance {
    // Dirección base del servicio REST utilizado para las pruebas de integración.
    private const val BASE_URL = "https://jsonplaceholder.typicode.com/"

    // Define límites de espera para evitar que una solicitud quede pendiente indefinidamente.
    private val cliente = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    // Construye el cliente una sola vez y lo reutiliza durante la ejecución de la aplicación.
    // Gson convierte automáticamente las respuestas JSON en los modelos de Kotlin.
    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
