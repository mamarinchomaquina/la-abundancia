package com.abundancia.app.data

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.Part
import retrofit2.http.POST
import retrofit2.http.Path

/** Rutas del backend Express (backend/app.js). Son las mismas que consume el panel web. */
interface ApiService {

    @GET("health")
    suspend fun health(): ResponseBody

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("api/auth/registro")
    suspend fun registro(@Body body: RegistroRequest): ResponseBody

    @GET("api/publicaciones")
    suspend fun publicaciones(): List<Publicacion>

    @GET("api/publicaciones/{id}")
    suspend fun publicacion(@Path("id") id: String): Publicacion

    @POST("api/publicaciones")
    suspend fun crearPublicacion(@Body body: NuevaPublicacion): Publicacion

    /** Foto de la publicación: multipart/form-data con el campo "imagen" (Multer en el backend). */
    @Multipart
    @POST("api/publicaciones/{id}/imagen")
    suspend fun subirImagen(@Path("id") id: String, @Part imagen: MultipartBody.Part): Publicacion

    @DELETE("api/publicaciones/{id}")
    suspend fun eliminarPublicacion(@Path("id") id: String): Response<Unit>

    @GET("api/solicitudes")
    suspend fun solicitudes(): List<Solicitud>

    @POST("api/solicitudes")
    suspend fun crearSolicitud(@Body body: SolicitudRequest): ResponseBody

    @PATCH("api/solicitudes/{id}/estado")
    suspend fun cambiarEstadoSolicitud(
        @Path("id") id: String,
        @Body body: EstadoRequest,
    ): ResponseBody
}
