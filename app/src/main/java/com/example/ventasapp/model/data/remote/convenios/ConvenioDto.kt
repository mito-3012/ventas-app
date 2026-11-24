package com.example.ventasapp.model.data.remote.convenios

data class ConvenioDto(
    val id: Long,
    val tipo: String,
    val nombre: String,
    val descripcion: String,
    val condiciones: String,
    val prestadores: List<String> = emptyList()
)