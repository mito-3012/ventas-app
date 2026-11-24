package com.example.ventasapp.model.data.remote.convenios

import retrofit2.http.GET
import retrofit2.http.Path

interface ConveniosApiService {

    @GET("convenios")
    suspend fun obtenerConvenios(): List<ConvenioDto>

    @GET("convenios/{id}")
    suspend fun obtenerConvenio(
        @Path("id") id: Long
    ): ConvenioDto
}