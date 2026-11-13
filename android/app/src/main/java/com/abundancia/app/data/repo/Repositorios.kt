package com.abundancia.app.data.repo

import android.content.Context
import com.abundancia.app.data.EstadoRequest
import com.abundancia.app.data.Fotos
import com.abundancia.app.data.LoginRequest
import com.abundancia.app.data.NuevaPublicacion
import com.abundancia.app.data.Publicacion
import com.abundancia.app.data.RegistroRequest
import com.abundancia.app.data.Session
import com.abundancia.app.data.Solicitud
import com.abundancia.app.data.SolicitudRequest
import com.abundancia.app.data.llamar
import com.abundancia.app.data.local.AbundanciaDatabase
import com.abundancia.app.data.local.BorradorPublicacionEntity
import com.abundancia.app.data.local.EstadoSync
import com.abundancia.app.data.local.PublicacionCacheEntity
import com.abundancia.app.data.local.SolicitudCacheEntity
import com.abundancia.app.data.local.SolicitudPendienteEntity
import com.abundancia.app.data.refId
import com.abundancia.app.data.refNombre
import com.abundancia.app.data.sync.Sincronizador
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import java.util.UUID

enum class ResultadoEnvio { OK, REINTENTAR }

/** Modelo que usan las pantallas (viene de la copia local o del servidor). */
data class PublicacionItem(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val categoria: String,
    val cantidad: Int,
    val ubicacion: String,
    val estado: String,
    val autorId: String,
    val autorNombre: String,
    val createdAt: String?,
    /** Ruta en el servidor ("/uploads/...") o null si no tiene foto */
    val imagen: String? = null,
)

data class SolicitudItem(
    val id: String,
    val publicacionId: String,
    val publicacionTitulo: String,
    val categoria: String,
    val ubicacion: String,
    val duenoId: String,
    val solicitanteId: String,
    val solicitanteNombre: String,
    val mensaje: String,
    val estado: String,
    val createdAt: String?,
)

/** Resultado de una consulta: los datos y, si falló el servidor, de dónde salieron. */
data class Consulta<T>(val datos: T?, val desdeCopiaLocal: Boolean, val error: String?)

private fun ahora() = System.currentTimeMillis()

/** ¿El fallo fue de red / servidor caído? Entonces se reintenta más tarde. */
private fun esFalloTemporal(e: Throwable?): Boolean =
    e is IOException || (e is HttpException && e.code() >= 500)

private fun Publicacion.aEntidad(t: Long) = PublicacionCacheEntity(
    id = id, titulo = titulo, descripcion = descripcion, categoria = categoria, cantidad = cantidad,
    ubicacion = ubicacion, estado = estado, autorId = autorId(), autorNombre = autorNombre(),
    createdAt = createdAt, guardadoEn = t, imagen = imagen,
)

private fun PublicacionCacheEntity.aItem() = PublicacionItem(
    id, titulo, descripcion, categoria, cantidad, ubicacion, estado, autorId, autorNombre, createdAt, imagen,
)

private fun Solicitud.aEntidad(t: Long) = SolicitudCacheEntity(
    id = id,
    publicacionId = publicacion?.id.orEmpty(),
    publicacionTitulo = publicacion?.titulo ?: "(publicación eliminada)",
    categoria = publicacion?.categoria.orEmpty(),
    ubicacion = publicacion?.ubicacion.orEmpty(),
    duenoId = publicacion?.usuario.refId(),
    solicitanteId = usuario.refId(),
    solicitanteNombre = usuario.refNombre(),
    mensaje = mensaje,
    estado = estado,
    createdAt = createdAt,
    guardadoEn = t,
)

private fun SolicitudCacheEntity.aItem() = SolicitudItem(
    id, publicacionId, publicacionTitulo, categoria, ubicacion, duenoId, solicitanteId,
    solicitanteNombre, mensaje, estado, createdAt,
)

// ─────────────────────────────────────────────────────────────────────────────
// Autenticación
// ─────────────────────────────────────────────────────────────────────────────
class AuthRepository(private val context: Context, private val db: AbundanciaDatabase) {

    suspend fun login(email: String, password: String): Result<Unit> {
        val r = llamar { this.login(LoginRequest(email.trim(), password)) }
        return r.mapCatching { resp ->
            val u = resp.usuario
            if (resp.token.isBlank() || u == null) throw IllegalStateException("Respuesta inválida del servidor.")
            Session.iniciar(resp.token, u)
            Sincronizador.programar(context)   // por si quedó algo pendiente de una sesión anterior
        }
    }

    suspend fun registro(nombre: String, email: String, password: String): Result<Unit> {
        val r = llamar { this.registro(RegistroRequest(nombre.trim(), email.trim(), password)) }
        return if (r.isSuccess) login(email, password) else Result.failure(r.exceptionOrNull()!!)
    }

    /** Cierra sesión y borra la copia local (los pendientes se conservan para cuando vuelva a entrar). */
    suspend fun cerrarSesion() {
        Session.cerrar()
        db.publicacionCacheDao().borrarTodas()
        db.solicitudCacheDao().borrarTodas()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Publicaciones: copia local (Room) + servidor + borradores y cola de envío
// ─────────────────────────────────────────────────────────────────────────────
class PublicacionesRepository(private val context: Context, db: AbundanciaDatabase) {
    private val cache = db.publicacionCacheDao()
    private val borradores = db.borradorDao()

    /** Siempre se lee de Room; el servidor solo actualiza esta copia. */
    val publicaciones: Flow<List<PublicacionItem>> = cache.observarTodas().map { lista -> lista.map { it.aItem() } }

    val ultimaActualizacion: Flow<Long?> = cache.observarUltimaActualizacion()

    /** Trae la lista del servidor y REEMPLAZA completa la copia local. */
    suspend fun refrescar(): Result<Unit> =
        llamar { this.publicaciones() }.mapCatching { lista ->
            val t = ahora()
            cache.reemplazarTodas(lista.map { it.aEntidad(t) })
        }

    /** Detalle: intenta el servidor; si no hay conexión usa la copia local. */
    suspend fun obtener(id: String): Consulta<PublicacionItem> {
        val r = llamar { this.publicacion(id) }
        val p = r.getOrNull()
        if (p != null) {
            val entidad = p.aEntidad(ahora())
            cache.guardarUna(entidad)
            return Consulta(entidad.aItem(), desdeCopiaLocal = false, error = null)
        }
        val local = cache.porId(id)?.aItem()
        return Consulta(local, desdeCopiaLocal = local != null, error = r.exceptionOrNull()?.message)
    }

    /** Eliminar necesita conexión (el servidor decide si se permite). */
    suspend fun eliminar(id: String): Result<Unit> {
        val r = llamar {
            val resp = this.eliminarPublicacion(id)
            if (!resp.isSuccessful) throw HttpException(resp)
        }
        if (r.isSuccess) cache.borrar(id)
        return r
    }

    // ── Borrador del formulario ──────────────────────────────────────────────
    suspend fun borradorActual(usuarioId: String): BorradorPublicacionEntity =
        borradores.borradorActual(usuarioId) ?: BorradorPublicacionEntity(
            localId = UUID.randomUUID().toString(),
            usuarioId = usuarioId,
            creadoEn = ahora(),
            actualizadoEn = ahora(),
        )

    suspend fun guardarBorrador(borrador: BorradorPublicacionEntity) {
        borradores.guardar(borrador.copy(estadoSync = EstadoSync.BORRADOR, actualizadoEn = ahora()))
    }

    // ── Cola de envío ────────────────────────────────────────────────────────
    fun envios(usuarioId: String): Flow<List<BorradorPublicacionEntity>> = borradores.observarEnvios(usuarioId)

    fun porEnviar(usuarioId: String): Flow<Int> = borradores.observarPorEnviar(usuarioId)

    /** Pasa el borrador a la cola. Se envía ya si hay red, o cuando vuelva la conexión. */
    suspend fun encolar(borrador: BorradorPublicacionEntity) {
        borradores.guardar(borrador.copy(estadoSync = EstadoSync.PENDIENTE, error = null, actualizadoEn = ahora()))
        Sincronizador.programar(context)
    }

    suspend fun reintentar(localId: String) {
        borradores.actualizarEstado(localId, EstadoSync.PENDIENTE, null, null, ahora())
        Sincronizador.programar(context)
    }

    suspend fun descartar(localId: String) {
        Fotos.borrar(borradores.porId(localId)?.imagenLocal)
        borradores.borrar(localId)
    }

    suspend fun limpiarEnviados(usuarioId: String) = borradores.limpiarEnviados(usuarioId)

    /**
     * Lo llama el Worker. Dos pasos por publicación:
     *  1) crearla (el clienteId = localId evita duplicados si el envío se repite)
     *  2) subir su foto, si tiene (multipart a /api/publicaciones/:id/imagen)
     * Si el paso 2 falla por red, queda PENDIENTE con el servidorId ya guardado y
     * en el siguiente intento solo se sube la foto.
     */
    suspend fun enviarPendientes(usuarioId: String): ResultadoEnvio {
        borradores.reiniciarEnviando()
        for (b in borradores.pendientes(usuarioId)) {
            borradores.actualizarEstado(b.localId, EstadoSync.ENVIANDO, null, null, ahora())

            // Paso 1: crear (solo si aún no existe en el servidor)
            var servidorId = b.servidorId
            if (servidorId == null) {
                val r = llamar {
                    this.crearPublicacion(
                        NuevaPublicacion(
                            titulo = b.titulo.trim(),
                            descripcion = b.descripcion.trim(),
                            categoria = b.categoria,
                            cantidad = b.cantidad.toIntOrNull() ?: 0,
                            ubicacion = b.ubicacion.trim(),
                            clienteId = b.localId,
                        )
                    )
                }
                val creada = r.getOrNull()
                if (creada == null) {
                    val causa = r.exceptionOrNull()?.cause
                    when {
                        esFalloTemporal(causa) -> {
                            borradores.actualizarEstado(b.localId, EstadoSync.PENDIENTE, "Esperando al servidor…", null, ahora())
                            return ResultadoEnvio.REINTENTAR
                        }
                        causa is HttpException && causa.code() == 401 -> {
                            borradores.actualizarEstado(b.localId, EstadoSync.PENDIENTE, "Inicia sesión para enviar.", null, ahora())
                            return ResultadoEnvio.OK
                        }
                        else -> {
                            borradores.actualizarEstado(b.localId, EstadoSync.ERROR, r.exceptionOrNull()?.message, null, ahora())
                            continue
                        }
                    }
                }
                servidorId = creada.id
                cache.guardarUna(creada.aEntidad(ahora()))
                borradores.actualizarEstado(b.localId, EstadoSync.ENVIANDO, null, servidorId, ahora())
            }

            // Paso 2: subir la foto
            val rutaFoto = b.imagenLocal
            val archivo = rutaFoto?.let { File(it) }
            if (archivo != null && archivo.exists()) {
                val parte = MultipartBody.Part.createFormData(
                    "imagen", archivo.name, archivo.asRequestBody("image/jpeg".toMediaType()),
                )
                val id = servidorId ?: continue
                val r = llamar { this.subirImagen(id, parte) }
                val conFoto = r.getOrNull()
                if (conFoto == null) {
                    val causa = r.exceptionOrNull()?.cause
                    when {
                        esFalloTemporal(causa) -> {
                            borradores.actualizarEstado(b.localId, EstadoSync.PENDIENTE, "Publicada. Falta subir la foto…", id, ahora())
                            return ResultadoEnvio.REINTENTAR
                        }
                        causa is HttpException && causa.code() == 401 -> {
                            borradores.actualizarEstado(b.localId, EstadoSync.PENDIENTE, "Inicia sesión para subir la foto.", id, ahora())
                            return ResultadoEnvio.OK
                        }
                        else -> {
                            // La publicación ya existe; la foto fue rechazada (formato, tamaño o ya aprobada)
                            borradores.actualizarEstado(
                                b.localId, EstadoSync.ENVIADO,
                                "Publicada, pero la foto no se pudo subir: ${r.exceptionOrNull()?.message}", id, ahora(),
                            )
                            continue
                        }
                    }
                }
                cache.guardarUna(conFoto.aEntidad(ahora()))
            }
            if (rutaFoto != null) {
                Fotos.borrar(rutaFoto)
                borradores.quitarImagenLocal(b.localId)
            }
            borradores.actualizarEstado(b.localId, EstadoSync.ENVIADO, null, servidorId, ahora())
        }
        return ResultadoEnvio.OK
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Solicitudes: copia local + cola de solicitudes por enviar
// ─────────────────────────────────────────────────────────────────────────────
class SolicitudesRepository(private val context: Context, db: AbundanciaDatabase) {
    private val cache = db.solicitudCacheDao()
    private val pendientesDao = db.solicitudPendienteDao()

    val solicitudes: Flow<List<SolicitudItem>> = cache.observarTodas().map { lista -> lista.map { it.aItem() } }

    val ultimaActualizacion: Flow<Long?> = cache.observarUltimaActualizacion()

    suspend fun refrescar(): Result<Unit> =
        llamar { this.solicitudes() }.mapCatching { lista ->
            val t = ahora()
            cache.reemplazarTodas(lista.map { it.aEntidad(t) })
        }

    fun pendientes(usuarioId: String): Flow<List<SolicitudPendienteEntity>> = pendientesDao.observar(usuarioId)

    fun pendientePara(usuarioId: String, publicacionId: String): Flow<SolicitudPendienteEntity?> =
        pendientesDao.observarPara(usuarioId, publicacionId)

    fun porEnviar(usuarioId: String): Flow<Int> = pendientesDao.observarPorEnviar(usuarioId)

    /** Guarda la solicitud en el celular y la manda en cuanto haya conexión. No duplica si ya hay una activa. */
    suspend fun solicitar(usuarioId: String, publicacionId: String, titulo: String, mensaje: String): Boolean {
        if (pendientesDao.activaPara(usuarioId, publicacionId) != null) return false
        pendientesDao.guardar(
            SolicitudPendienteEntity(
                localId = UUID.randomUUID().toString(),
                usuarioId = usuarioId,
                publicacionId = publicacionId,
                publicacionTitulo = titulo,
                mensaje = mensaje.trim(),
                creadoEn = ahora(),
            )
        )
        Sincronizador.programar(context)
        return true
    }

    /** Aceptar / rechazar necesita conexión: el servidor valida quién puede hacerlo. */
    suspend fun resolver(id: String, estado: String): Result<Unit> {
        val r = llamar { this.cambiarEstadoSolicitud(id, EstadoRequest(estado)) }.map { }
        if (r.isSuccess) refrescar()
        return r
    }

    suspend fun reintentar(localId: String) {
        pendientesDao.actualizarEstado(localId, EstadoSync.PENDIENTE, null)
        Sincronizador.programar(context)
    }

    suspend fun descartar(localId: String) = pendientesDao.borrar(localId)

    suspend fun enviarPendientes(usuarioId: String): ResultadoEnvio {
        pendientesDao.reiniciarEnviando()
        for (s in pendientesDao.pendientes(usuarioId)) {
            pendientesDao.actualizarEstado(s.localId, EstadoSync.ENVIANDO, null)
            val r = llamar { this.crearSolicitud(SolicitudRequest(s.publicacionId, s.mensaje)) }
            if (r.isSuccess) {
                pendientesDao.actualizarEstado(s.localId, EstadoSync.ENVIADO, null)
                continue
            }
            val causa = r.exceptionOrNull()?.cause
            when {
                esFalloTemporal(causa) -> {
                    pendientesDao.actualizarEstado(s.localId, EstadoSync.PENDIENTE, "Esperando al servidor…")
                    return ResultadoEnvio.REINTENTAR
                }
                // 409 = el servidor ya tiene esa solicitud (un envío anterior sí llegó): no se duplica
                causa is HttpException && causa.code() == 409 ->
                    pendientesDao.actualizarEstado(s.localId, EstadoSync.ENVIADO, null)
                causa is HttpException && causa.code() == 401 -> {
                    pendientesDao.actualizarEstado(s.localId, EstadoSync.PENDIENTE, "Inicia sesión para enviar.")
                    return ResultadoEnvio.OK
                }
                else -> pendientesDao.actualizarEstado(s.localId, EstadoSync.ERROR, r.exceptionOrNull()?.message)
            }
        }
        if (Session.token != null) refrescar()
        return ResultadoEnvio.OK
    }
}

/** Cantidad total de cambios hechos sin conexión que aún no llegan al servidor. */
fun totalPorEnviar(
    publicaciones: PublicacionesRepository,
    solicitudes: SolicitudesRepository,
    usuarioId: String,
): Flow<Int> = combine(publicaciones.porEnviar(usuarioId), solicitudes.porEnviar(usuarioId)) { a, b -> a + b }
