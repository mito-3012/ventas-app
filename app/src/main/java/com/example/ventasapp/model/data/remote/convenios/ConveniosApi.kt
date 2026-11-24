package com.example.ventasapp.model.data.remote.convenios

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ConveniosApi {

    // Dirección base de la API REST.
    // 10.0.2.2 permite acceder al localhost del computador
    // desde el emulador Android.
    private const val BASE_URL = "http://10.0.2.2:3000/"

    // Registro de solicitudes y respuestas HTTP.
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Cliente HTTP utilizado por Retrofit.
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    // Instancia de Retrofit.
    val service: ConveniosApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ConveniosApiService::class.java)
    }
}