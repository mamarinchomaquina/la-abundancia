package com.abundancia.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Copia la foto elegida a la memoria de la app, reducida a máximo 1280 px y en JPG.
 * Así se puede subir más tarde aunque no haya internet, y queda por debajo del
 * límite de 2 MB que valida Multer en el backend.
 */
object Fotos {
    private const val LADO_MAXIMO = 1280
    const val TAMANO_MAXIMO = 2L * 1024 * 1024

    fun guardarComprimida(context: Context, uri: Uri): File? {
        val cr = context.contentResolver

        // 1) Medir sin cargar la imagen completa
        val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val entradaMedidas = cr.openInputStream(uri) ?: return null
        entradaMedidas.use { BitmapFactory.decodeStream(it, null, medidas) }
        if (medidas.outWidth <= 0 || medidas.outHeight <= 0) return null

        var muestra = 1
        while (medidas.outWidth / (muestra * 2) >= LADO_MAXIMO || medidas.outHeight / (muestra * 2) >= LADO_MAXIMO) {
            muestra *= 2
        }

        // 2) Cargar reducida
        val opciones = BitmapFactory.Options().apply { inSampleSize = muestra }
        val entrada = cr.openInputStream(uri) ?: return null
        val original = entrada.use { BitmapFactory.decodeStream(it, null, opciones) } ?: return null

        // 3) Corregir la orientación de fotos tomadas con la cámara
        val grados = runCatching {
            cr.openInputStream(uri)?.use { s ->
                when (ExifInterface(s).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        }.getOrDefault(0f)

        val escala = minOf(1f, LADO_MAXIMO.toFloat() / maxOf(original.width, original.height))
        val matriz = Matrix().apply {
            if (escala < 1f) postScale(escala, escala)
            if (grados != 0f) postRotate(grados)
        }
        val resultado = if (escala < 1f || grados != 0f) {
            Bitmap.createBitmap(original, 0, 0, original.width, original.height, matriz, true)
        } else {
            original
        }

        // 4) Guardar como JPG
        val carpeta = File(context.filesDir, "fotos").apply { mkdirs() }
        val archivo = File(carpeta, "${UUID.randomUUID()}.jpg")
        FileOutputStream(archivo).use { resultado.compress(Bitmap.CompressFormat.JPEG, 80, it) }
        return archivo
    }

    fun borrar(ruta: String?) {
        if (ruta != null) runCatching { File(ruta).delete() }
    }
}
