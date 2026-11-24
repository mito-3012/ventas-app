package com.example.ventasapp.util

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Convierte fallos técnicos de Retrofit/red en mensajes comprensibles. */
object ErroresRed {
    // Clasifica la respuesta HTTP para explicar al usuario por qué falló la solicitud.
    fun mensajeParaCodigoHttp(codigo: Int): String = when (codigo) {
        404 -> "El recurso solicitado no existe en el servidor (código 404)."
        in 400..499 -> "El servidor no pudo procesar la solicitud (código $codigo)."
        in 500..599 -> "El servidor presenta un inconveniente (código $codigo). Intenta nuevamente más tarde."
        else -> "El servidor respondió con un código inesperado ($codigo)."
    }

    // Distingue problemas de conexión, tiempo de espera y otros errores inesperados.
    fun mensajeDeError(error: Throwable): String = when (error) {
        is UnknownHostException -> "No fue posible conectar con el servidor. Verifica tu conexión a internet."
        is SocketTimeoutException -> "El servidor tardó demasiado en responder. Intenta nuevamente."
        is IOException -> "Se produjo un error de red. Verifica tu conexión a internet."
        else -> "Ocurrió un error inesperado al comunicarse con el servidor."
    }
}
