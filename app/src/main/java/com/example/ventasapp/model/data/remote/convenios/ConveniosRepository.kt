package com.example.ventasapp.model.data.remote.convenios

class ConveniosRepository(
    private val api: ConveniosApiService = ConveniosApi.service
) {

    suspend fun listar(): List<ConvenioDto> {
        return api.obtenerConvenios()
    }

    suspend fun detalle(id: Long): ConvenioDto {
        return api.obtenerConvenio(id)
    }
}