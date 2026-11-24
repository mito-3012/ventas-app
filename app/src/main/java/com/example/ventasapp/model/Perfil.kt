package com.example.ventasapp.model

/**
 * Perfiles de usuario. Cada perfil ve una parte distinta de la app (control de acceso por perfil).
 *
 *  - VENDEDOR:      registra sus ventas, las corrige y consulta su historial.
 *  - ENCARGADO:     revisa, valida, observa o rechaza las ventas recibidas.
 *  - ADMINISTRADOR: crea usuarios, los activa o desactiva, asigna perfiles y configura las
 *                   comisiones (ficticias). No revisa ventas.
 *
 * En la base de datos se guarda el nombre del enum como texto (por ejemplo "VENDEDOR").
 */
enum class Perfil(val etiqueta: String) {
    VENDEDOR("Vendedor"),
    ENCARGADO("Encargado de gestión"),
    ADMINISTRADOR("Administrador");

    companion object {
        /** Convierte el texto guardado en Room a enum. Si viene algo raro, asume el perfil de menor privilegio. */
        fun desde(nombre: String): Perfil = entries.firstOrNull { it.name == nombre } ?: VENDEDOR
    }
}
