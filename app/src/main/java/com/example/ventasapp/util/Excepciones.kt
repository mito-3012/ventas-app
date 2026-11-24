package com.example.ventasapp.util

/**
 * Excepción para reglas de negocio incumplidas (por ejemplo "no puedes validar tu propia venta").
 *
 * Se distingue de un error técnico (IOException, etc.) porque su mensaje SÍ está pensado para
 * mostrárselo al usuario tal cual. El ViewModel la captura y la convierte en un mensaje visual.
 */
open class ReglaNegocioException(mensaje: String) : Exception(mensaje)

/** Caso especial: el código de autorización ya existe. Permite pintar el error en el campo correcto. */
class CodigoDuplicadoException :
    ReglaNegocioException("Este código de autorización ya está registrado en otra venta.")

/** Caso especial: el correo ya está registrado al crear una cuenta. */
class CorreoDuplicadoException :
    ReglaNegocioException("Ya existe una cuenta con este correo.")
