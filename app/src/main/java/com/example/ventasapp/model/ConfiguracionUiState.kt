package com.example.ventasapp.model

/**
 * Estado de la pantalla de configuración. Los porcentajes se editan como TEXTO (lo que escribe el
 * administrador, por ejemplo "7,5") y se convierten a número recién al guardar.
 */
data class ConfiguracionUiState(
    val valores: Map<TipoConvenio, String> = emptyMap(),
    val errores: Map<TipoConvenio, String?> = emptyMap()
)
