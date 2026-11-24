package com.example.ventasapp.util

/**
 * Evento de una sola vez que el ViewModel le envía a la pantalla (por ejemplo, mostrar un Snackbar).
 *
 * Se usa un Channel en vez de un campo dentro del estado porque un mensaje debe mostrarse UNA vez;
 * si viviera en el estado, reaparecería al rotar la pantalla.
 */
data class EventoUi(val texto: String, val esError: Boolean = false)
