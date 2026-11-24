package com.example.ventasapp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import kotlin.math.max

/**
 * Manejo de los respaldos fotográficos (comprobantes FICTICIOS).
 *
 * Responsabilidades:
 *  1. Crear la URI segura donde la cámara deja la foto (FileProvider).
 *  2. Restringir tipo de archivo y tamaño (exigencia del caso).
 *  3. Reducir y comprimir la imagen, y copiarla al almacenamiento PRIVADO de la app.
 *
 * Guardar una copia propia es importante: las URIs que entrega la galería pueden dejar de ser
 * válidas, en cambio el archivo en filesDir existe mientras la app esté instalada.
 *
 * Todas las funciones que leen o escriben archivos son bloqueantes: hay que llamarlas desde
 * Dispatchers.IO (el ViewModel se encarga de eso).
 */
object ImagenUtils {

    private const val MAX_BYTES_ORIGEN = 15L * 1024 * 1024   // archivo de entrada: máximo 15 MB
    private const val MAX_BYTES_FINAL = 2 * 1024 * 1024      // archivo guardado: máximo 2 MB
    private const val LADO_MAXIMO_PX = 1280                  // lado más largo de la imagen guardada
    private val TIPOS_PERMITIDOS = setOf("image/jpeg", "image/png", "image/webp")

    // ------------------------------------------------------------------ Cámara

    /** Crea un archivo vacío en cache/camara donde la app de cámara escribirá la foto. */
    fun crearArchivoCamara(context: Context): File {
        val carpeta = File(context.cacheDir, "camara").apply { mkdirs() }
        // Se crea vacío de antemano: algunas apps de cámara fallan si el archivo destino no existe.
        return File(carpeta, "captura_${System.currentTimeMillis()}.jpg").also { it.createNewFile() }
    }

    /** Convierte el archivo en una URI content:// que sí se puede entregar a otra app. */
    fun uriSegura(context: Context, archivo: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)

    // ------------------------------------------------------------------ Procesamiento

    /** Procesa una imagen elegida desde la galería. Devuelve la ruta del archivo privado final. */
    fun procesarDesdeUri(context: Context, uri: Uri): String {
        val resolver = context.contentResolver
        val tipo = resolver.getType(uri)
        if (tipo == null || tipo !in TIPOS_PERMITIDOS) {
            throw ReglaNegocioException("Formato no permitido. Usa una imagen JPG, PNG o WEBP.")
        }
        val tamano = resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        if (tamano > MAX_BYTES_ORIGEN) {
            throw ReglaNegocioException("El archivo pesa más de 15 MB. Elige una imagen más ligera.")
        }
        return procesar(context) { resolver.openInputStream(uri) }
    }

    /** Procesa la foto recién tomada con la cámara y borra el archivo temporal. */
    fun procesarDesdeArchivo(context: Context, archivo: File): String {
        try {
            if (!archivo.exists() || archivo.length() == 0L) {
                throw ReglaNegocioException("No se pudo obtener la foto. Inténtalo de nuevo.")
            }
            return procesar(context) { archivo.inputStream() }
        } finally {
            archivo.delete()
        }
    }

    private fun procesar(context: Context, abrir: () -> InputStream?): String {
        // Paso 1: leer solo las dimensiones, sin cargar la imagen completa en memoria.
        val soloBordes = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // OJO: con inJustDecodeBounds = true, decodeStream SIEMPRE devuelve null (solo rellena
        // outWidth y outHeight). Por eso no se debe usar su resultado para detectar un error.
        val flujoBordes = abrir() ?: throw ReglaNegocioException("No se pudo leer el archivo seleccionado.")
        flujoBordes.use { BitmapFactory.decodeStream(it, null, soloBordes) }
        if (soloBordes.outWidth <= 0 || soloBordes.outHeight <= 0) {
            throw ReglaNegocioException("El archivo no es una imagen válida.")
        }

        // Paso 2: submuestreo (potencias de 2) para no gastar memoria con fotos de 12 megapíxeles.
        var muestreo = 1
        while (max(soloBordes.outWidth, soloBordes.outHeight) / muestreo > LADO_MAXIMO_PX * 2) {
            muestreo *= 2
        }
        val opciones = BitmapFactory.Options().apply { inSampleSize = muestreo }
        var bitmap = abrir()?.use { BitmapFactory.decodeStream(it, null, opciones) }
            ?: throw ReglaNegocioException("No se pudo decodificar la imagen.")

        // Paso 3: corregir la rotación (las fotos de cámara suelen venir "de lado" con un dato EXIF).
        val grados = abrir()?.use { leerRotacionExif(it) } ?: 0
        if (grados != 0) {
            val matriz = Matrix().apply { postRotate(grados.toFloat()) }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)
        }

        // Paso 4: escalar para que el lado más largo no pase de LADO_MAXIMO_PX.
        val escala = LADO_MAXIMO_PX.toFloat() / max(bitmap.width, bitmap.height)
        if (escala < 1f) {
            bitmap = Bitmap.createScaledBitmap(
                bitmap, (bitmap.width * escala).toInt(), (bitmap.height * escala).toInt(), true
            )
        }

        // Paso 5: comprimir a JPEG bajando la calidad hasta cumplir el tamaño máximo.
        var calidad = 85
        var bytes: ByteArray
        do {
            val salida = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, calidad, salida)
            bytes = salida.toByteArray()
            calidad -= 10
        } while (bytes.size > MAX_BYTES_FINAL && calidad > 20)
        if (bytes.size > MAX_BYTES_FINAL) {
            throw ReglaNegocioException("La imagen supera el tamaño máximo permitido (2 MB).")
        }

        // Paso 6: guardar en el almacenamiento privado de la app.
        val destino = File(carpetaRespaldos(context), "respaldo_${System.currentTimeMillis()}.jpg")
        destino.writeBytes(bytes)
        return destino.absolutePath
    }

    private fun leerRotacionExif(flujo: InputStream): Int = try {
        when (ExifInterface(flujo).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } catch (e: Exception) {
        0
    }

    // ------------------------------------------------------------------ Limpieza

    private fun carpetaRespaldos(context: Context): File =
        File(context.filesDir, "respaldos").apply { mkdirs() }

    /**
     * Borra UN respaldo del almacenamiento privado. Por seguridad solo borra archivos que estén
     * dentro de la carpeta de respaldos de la app (nunca una ruta arbitraria).
     */
    fun eliminarRespaldo(context: Context, ruta: String?): Boolean {
        if (ruta.isNullOrBlank()) return false
        return try {
            val archivo = File(ruta).canonicalFile
            archivo.parentFile == carpetaRespaldos(context).canonicalFile && archivo.delete()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Borra las fotos que ya no pertenecen a ninguna venta (por ejemplo, fotos que se tomaron en un
     * formulario y se descartaron). [enUso] son las rutas que SÍ están guardadas en la base.
     */
    fun eliminarHuerfanos(context: Context, enUso: Set<String>): Int {
        val archivos = carpetaRespaldos(context).listFiles() ?: return 0
        return archivos.count { it.absolutePath !in enUso && it.delete() }
    }

    /** Borra todos los respaldos de prueba. Devuelve cuántos archivos se eliminaron. */
    fun eliminarTodosLosRespaldos(context: Context): Int {
        val archivos = carpetaRespaldos(context).listFiles() ?: return 0
        return archivos.count { it.delete() }
    }
}
