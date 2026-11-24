package com.example.ventasapp.util

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Manejo de contraseñas. El caso exige que las contraseñas de prueba NO queden como texto visible.
 *
 * Se guarda un hash PBKDF2 con "sal" aleatoria por usuario. La sal evita que dos usuarios con la
 * misma contraseña tengan el mismo hash. Nunca se almacena la clave original.
 *
 * Nota técnica: para un producto real se recomienda Argon2 o bcrypt y más iteraciones; PBKDF2 se
 * eligió porque viene incluido en Android desde la API 24 sin dependencias externas.
 */
object Seguridad {

    private const val ITERACIONES = 10_000
    private const val LARGO_CLAVE_BITS = 256

    fun generarSal(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.aHex()
    }

    fun hashClave(clave: String, sal: String): String {
        val spec = PBEKeySpec(clave.toCharArray(), sal.hexABytes(), ITERACIONES, LARGO_CLAVE_BITS)
        val fabrica = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        return fabrica.generateSecret(spec).encoded.aHex()
    }

    /** Compara en tiempo constante (isEqual) para no filtrar información por diferencias de tiempo. */
    fun verificar(clave: String, sal: String, hashGuardado: String): Boolean =
        MessageDigest.isEqual(hashClave(clave, sal).toByteArray(), hashGuardado.toByteArray())

    private fun ByteArray.aHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexABytes(): ByteArray =
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
