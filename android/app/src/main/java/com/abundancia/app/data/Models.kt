package com.abundancia.app.data

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

/** Mismas categorías que valida el backend (models/Publicacion.js). */
val CATEGORIAS = listOf("Alimentos", "Ropa", "Útiles", "Otros")

const val FILTRO_TODAS = "Todas"
const val FILTRO_MIAS = "Mías"

data class Usuario(
    val id: String = "",
    val nombre: String = "",
    val email: String = "",
    val rol: String = "usuario",
) {
    val esAdmin: Boolean get() = rol == "admin"
}

data class LoginRequest(val email: String, val password: String)

data class LoginResponse(
    val token: String = "",
    val usuario: Usuario? = null,
)

data class RegistroRequest(val nombre: String, val email: String, val password: String)

/**
 * El backend devuelve "usuario" a veces como objeto ({_id, nombre}, cuando hace populate)
 * y a veces como un id en texto. Por eso se guarda como JsonElement.
 */
data class Publicacion(
    @SerializedName("_id") val id: String = "",
    val titulo: String = "",
    val descripcion: String = "",
    val categoria: String = "",
    val cantidad: Int = 0,
    val ubicacion: String = "",
    val estado: String = "pendiente",
    val createdAt: String? = null,
    val usuario: JsonElement? = null,
    /** Ruta de la foto en el servidor, p. ej. "/uploads/abc.jpg" (subida con Multer). */
    val imagen: String? = null,
) {
    fun autorId(): String = usuario.refId()
    fun autorNombre(): String = usuario.refNombre()
}

data class NuevaPublicacion(
    val titulo: String,
    val descripcion: String,
    val categoria: String,
    val cantidad: Int,
    val ubicacion: String,
    /** Id generado en el celular: si el envío se reintenta, el servidor no crea un duplicado. */
    val clienteId: String? = null,
)

data class PublicacionResumen(
    @SerializedName("_id") val id: String = "",
    val titulo: String = "",
    val categoria: String = "",
    val ubicacion: String = "",
    val usuario: JsonElement? = null,
)

data class Solicitud(
    @SerializedName("_id") val id: String = "",
    val publicacion: PublicacionResumen? = null,
    val usuario: JsonElement? = null,
    val mensaje: String = "",
    val estado: String = "pendiente",
    val createdAt: String? = null,
)

data class SolicitudRequest(val publicacion: String, val mensaje: String)

data class EstadoRequest(val estado: String)

fun JsonElement?.refId(): String {
    if (this == null || isJsonNull) return ""
    if (isJsonPrimitive) return asString
    if (isJsonObject) {
        val id = asJsonObject.get("_id") ?: return ""
        return if (id.isJsonNull) "" else id.asString
    }
    return ""
}

fun JsonElement?.refNombre(): String {
    if (this == null || !isJsonObject) return ""
    val nombre = asJsonObject.get("nombre") ?: return ""
    return if (nombre.isJsonNull) "" else nombre.asString
}
