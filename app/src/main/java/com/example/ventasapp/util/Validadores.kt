package com.example.ventasapp.util

/**
 * Reglas de validación de TODOS los formularios de la aplicación.
 *
 * Decisión de diseño (rúbrica IE 2.2.1): la validación vive aquí, en Kotlin puro, y no dentro de
 * los Composables. Las pantallas nunca deciden si un dato es válido; solo muestran el mensaje que
 * devuelve el ViewModel. Además, al no depender de Android, estas funciones se prueban con
 * pruebas unitarias simples (ver ValidadoresTest).
 *
 * Convención: cada función devuelve un String? con el mensaje de error, o null si el dato es válido.
 * Es el mismo criterio de UsuarioErrores de la guía 11.
 */
object Validadores {

    private const val MSG_OBLIGATORIO = "Campo obligatorio"
    private const val MAX_MONTO = 10_000_000
    private const val DIAS_MAXIMOS_ANTIGUEDAD = 60

    /** Cuántos días hacia el futuro se puede agendar una cita (1 año). */
    const val DIAS_MAXIMOS_FUTURO = 365
    private const val UN_DIA_MS = 24L * 60 * 60 * 1000

    private val REGEX_NOMBRE = Regex("^[\\p{L} .'-]+$")
    private val REGEX_CORREO = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val REGEX_TELEFONO = Regex("^(\\+?56)?9\\d{8}$")
    private val REGEX_CODIGO = Regex("^[A-Z0-9]{6,12}$")

    fun obligatorio(valor: String, mensaje: String = MSG_OBLIGATORIO): String? =
        if (valor.isBlank()) mensaje else null

    fun nombrePersona(valor: String): String? {
        val limpio = valor.trim()
        return when {
            limpio.isEmpty() -> MSG_OBLIGATORIO
            limpio.length < 3 -> "Debe tener al menos 3 caracteres"
            !REGEX_NOMBRE.matches(limpio) -> "Solo se permiten letras y espacios"
            else -> null
        }
    }

    fun correo(valor: String): String? {
        val limpio = valor.trim()
        return when {
            limpio.isEmpty() -> MSG_OBLIGATORIO
            !REGEX_CORREO.matches(limpio) -> "Introduce un correo electrónico válido (por ejemplo: nombre@dominio.cl)"
            else -> null
        }
    }

    fun clave(valor: String): String? = when {
        valor.isEmpty() -> MSG_OBLIGATORIO
        valor.length < 6 -> "Debe tener al menos 6 caracteres"
        valor.none { it.isLetter() } || valor.none { it.isDigit() } -> "Debe incluir letras y números"
        else -> null
    }

    fun confirmacionClave(clave: String, confirmacion: String): String? = when {
        confirmacion.isEmpty() -> MSG_OBLIGATORIO
        clave != confirmacion -> "Las contraseñas no coinciden"
        else -> null
    }

    fun terminosAceptados(aceptados: Boolean): String? =
        if (aceptados) null else "Debes aceptar los términos para continuar"

    // ---------------------------------------------------------------- RUT chileno (ficticio)

    /** Deja solo dígitos y la letra K, en mayúscula. "11.111.111-1" -> "111111111". */
    fun normalizarRut(valor: String): String =
        valor.filter { it.isDigit() || it == 'k' || it == 'K' }.uppercase()

    /** Calcula el dígito verificador con el algoritmo módulo 11. */
    fun digitoVerificador(cuerpo: String): Char {
        var suma = 0
        var multiplicador = 2
        for (digito in cuerpo.reversed()) {
            suma += (digito - '0') * multiplicador
            multiplicador = if (multiplicador == 7) 2 else multiplicador + 1
        }
        return when (val resto = 11 - (suma % 11)) {
            11 -> '0'
            10 -> 'K'
            else -> resto.toString()[0]
        }
    }

    fun rut(valor: String): String? {
        if (valor.isBlank()) return MSG_OBLIGATORIO
        val limpio = normalizarRut(valor)
        if (limpio.length < 8 || limpio.length > 9) return "Largo inválido (ej: 11.111.111-1)"
        val cuerpo = limpio.dropLast(1)
        val dv = limpio.last()
        if (cuerpo.any { !it.isDigit() }) return "El RUT solo admite números antes del guion"
        return if (digitoVerificador(cuerpo) == dv) null else "RUT inválido: el dígito verificador no calza"
    }

    /** "111111111" -> "11.111.111-1". Se usa para guardar y mostrar el RUT en formato estándar. */
    fun formatearRut(valor: String): String {
        val limpio = normalizarRut(valor)
        if (limpio.length < 2) return limpio
        val cuerpo = limpio.dropLast(1).reversed().chunked(3).joinToString(".").reversed()
        return "$cuerpo-${limpio.last()}"
    }

    // ---------------------------------------------------------------- Otros campos

    fun telefono(valor: String): String? {
        val limpio = valor.replace(" ", "")
        return when {
            limpio.isEmpty() -> MSG_OBLIGATORIO
            !REGEX_TELEFONO.matches(limpio) -> "Formato válido: 9XXXXXXXX o +569XXXXXXXX"
            else -> null
        }
    }

    fun monto(valor: String): String? {
        if (valor.isBlank()) return MSG_OBLIGATORIO
        val numero = valor.toIntOrNull() ?: return "Solo números enteros, sin puntos ni signos"
        return when {
            numero <= 0 -> "El monto debe ser mayor a 0"
            numero > MAX_MONTO -> "El monto supera el máximo permitido (\$10.000.000)"
            else -> null
        }
    }

    fun codigoAutorizacion(valor: String): String? = when {
        valor.isBlank() -> MSG_OBLIGATORIO
        !REGEX_CODIGO.matches(valor) -> "Entre 6 y 12 caracteres: solo letras mayúsculas y números"
        else -> null
    }

    /**
     * @param fechaMillis fecha elegida (medianoche UTC) o null si no eligió.
     * @param hoyMillis "hoy" también como medianoche UTC. Se recibe por parámetro para poder probar la función.
     */
    fun fechaOperacion(fechaMillis: Long?, hoyMillis: Long): String? = when {
        fechaMillis == null -> "Selecciona la fecha de la operación o de la cita"
        fechaMillis > fechaLimiteFutura(hoyMillis) ->
            "La cita no puede estar a más de $DIAS_MAXIMOS_FUTURO días (1 año) desde hoy"
        fechaMillis < hoyMillis - DIAS_MAXIMOS_ANTIGUEDAD * UN_DIA_MS ->
            "La operación no puede tener más de $DIAS_MAXIMOS_ANTIGUEDAD días de antigüedad"
        else -> null
    }

    /** Última fecha que se puede elegir: hoy + 1 año. Se usa también para limitar el calendario. */
    fun fechaLimiteFutura(hoyMillis: Long): Long = hoyMillis + DIAS_MAXIMOS_FUTURO * UN_DIA_MS

    /** Porcentaje de comisión escrito por el administrador: número entre 0 y 30 (acepta coma o punto). */
    fun porcentajeComision(valor: String): String? {
        if (valor.isBlank()) return MSG_OBLIGATORIO
        val numero = valor.trim().replace(',', '.').toDoubleOrNull()
            ?: return "Escribe un número (ejemplo: 10 o 7,5)"
        return if (numero < 0.0 || numero > 30.0) "Debe estar entre 0 y 30" else null
    }

    fun seleccion(valor: Any?, mensaje: String): String? = if (valor == null) mensaje else null

    /** Validaciones para publicaciones de la API REST (JSONPlaceholder). */
    // Comprueba que el título tenga contenido y respete la extensión permitida por el formulario.
    fun tituloPublicacion(valor: String): String? = when {
        valor.isBlank() -> "El título es obligatorio"
        valor.trim().length < 3 -> "El título debe tener al menos 3 caracteres"
        valor.trim().length > 120 -> "El título no puede superar 120 caracteres"
        else -> null
    }

    // Comprueba que el contenido no esté vacío y se mantenga dentro del límite establecido.
    fun cuerpoPublicacion(valor: String): String? = when {
        valor.isBlank() -> "El contenido es obligatorio"
        valor.trim().length < 5 -> "El contenido debe tener al menos 5 caracteres"
        valor.trim().length > 2000 -> "El contenido no puede superar 2000 caracteres"
        else -> null
    }

    fun observacion(valor: String): String? {
        val limpio = valor.trim()
        return when {
            limpio.isEmpty() -> "Debes escribir una observación para esta acción"
            limpio.length < 10 -> "Detalla un poco más (mínimo 10 caracteres)"
            else -> null
        }
    }
}
