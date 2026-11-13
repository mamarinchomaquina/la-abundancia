package com.abundancia.app.ui.publicaciones

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abundancia.app.data.AppContainer
import com.abundancia.app.data.CATEGORIAS
import com.abundancia.app.data.FILTRO_MIAS
import com.abundancia.app.data.FILTRO_TODAS
import com.abundancia.app.data.Fotos
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.abundancia.app.data.Session
import com.abundancia.app.data.local.BorradorPublicacionEntity
import com.abundancia.app.data.local.SolicitudPendienteEntity
import com.abundancia.app.data.repo.PublicacionItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

private fun usuarioId(): String = Session.usuario.value?.id.orEmpty()

// ─────────────────────────────────────────────────────────────────────────────
// Lista
// ─────────────────────────────────────────────────────────────────────────────
class PublicacionesViewModel : ViewModel() {
    private val repo = AppContainer.publicaciones

    /** La lista sale de Room: se ve aunque no haya internet. */
    val publicaciones: StateFlow<List<PublicacionItem>> =
        repo.publicaciones.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val ultimaActualizacion: StateFlow<Long?> =
        repo.ultimaActualizacion.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Filtro inicial = preferencia guardada en DataStore
    var filtro by mutableStateOf(Session.categoriaPreferida.value)
        private set
    var busqueda by mutableStateOf("")
        private set
    var cargando by mutableStateOf(false)
        private set
    /** Mensaje cuando el servidor no respondió y se muestra la copia local. */
    var errorServidor by mutableStateOf<String?>(null)
        private set

    init { refrescar() }

    fun cambiarFiltro(f: String) { filtro = f }
    fun cambiarBusqueda(b: String) { busqueda = b }

    fun refrescar() {
        if (cargando) return
        viewModelScope.launch {
            cargando = true
            errorServidor = repo.refrescar().exceptionOrNull()?.message
            cargando = false
        }
    }

    fun filtrar(lista: List<PublicacionItem>): List<PublicacionItem> {
        val yo = usuarioId()
        return lista.filter { p ->
            val pasaFiltro = when (filtro) {
                FILTRO_TODAS -> true
                FILTRO_MIAS -> p.autorId == yo
                else -> p.categoria == filtro
            }
            val pasaBusqueda = busqueda.isBlank() ||
                p.titulo.contains(busqueda, ignoreCase = true) ||
                p.descripcion.contains(busqueda, ignoreCase = true) ||
                p.ubicacion.contains(busqueda, ignoreCase = true)
            pasaFiltro && pasaBusqueda
        }
    }

    companion object {
        val FILTROS = listOf(FILTRO_TODAS, FILTRO_MIAS) + CATEGORIAS
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Detalle
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalCoroutinesApi::class)
class DetalleViewModel : ViewModel() {
    private val repo = AppContainer.publicaciones
    private val solicitudes = AppContainer.solicitudes
    private val idActual = MutableStateFlow<String?>(null)

    var publicacion by mutableStateOf<PublicacionItem?>(null)
        private set
    var cargando by mutableStateOf(true)
        private set
    var desdeCopiaLocal by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    var mensaje by mutableStateOf("")
        private set
    var enviando by mutableStateOf(false)
        private set
    var avisoSolicitud by mutableStateOf<String?>(null)
        private set
    var eliminando by mutableStateOf(false)
        private set

    /** Estado (en Room) de la solicitud que hice para esta publicación, si existe. */
    val miSolicitud: StateFlow<SolicitudPendienteEntity?> = idActual
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else solicitudes.pendientePara(usuarioId(), id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun cargar(id: String) {
        if (idActual.value == id && publicacion != null) return
        idActual.value = id
        recargar()
    }

    fun recargar() {
        val id = idActual.value ?: return
        viewModelScope.launch {
            cargando = true
            val r = repo.obtener(id)
            publicacion = r.datos
            desdeCopiaLocal = r.desdeCopiaLocal
            error = r.error
            cargando = false
        }
    }

    fun cambiarMensaje(v: String) { if (v.length <= 1000) mensaje = v }

    fun solicitar() {
        val p = publicacion ?: return
        if (enviando) return
        viewModelScope.launch {
            enviando = true
            val nueva = solicitudes.solicitar(usuarioId(), p.id, p.titulo, mensaje)
            avisoSolicitud = if (nueva) null else "Ya tienes una solicitud para esta publicación."
            enviando = false
        }
    }

    fun eliminar(onListo: () -> Unit) {
        val p = publicacion ?: return
        if (eliminando) return
        viewModelScope.launch {
            eliminando = true
            repo.eliminar(p.id)
                .onSuccess { onListo() }
                .onFailure { error = it.message }
            eliminando = false
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Nueva publicación: borrador guardado en Room + cola de envío
// ─────────────────────────────────────────────────────────────────────────────
class NuevaPublicacionViewModel : ViewModel() {
    private val repo = AppContainer.publicaciones
    private val yo = usuarioId()

    private var borrador: BorradorPublicacionEntity? = null
    private var guardado: Job? = null

    var titulo by mutableStateOf("")
        private set
    var descripcion by mutableStateOf("")
        private set
    var categoria by mutableStateOf("")
        private set
    var cantidad by mutableStateOf("1")
        private set
    var ubicacion by mutableStateOf("")
        private set
    /** Ruta del archivo de la foto guardada en el celular (se sube junto con la publicación). */
    var imagenLocal by mutableStateOf<String?>(null)
        private set
    var procesandoFoto by mutableStateOf(false)
        private set
    var errorFoto by mutableStateOf<String?>(null)
        private set

    var errorTitulo by mutableStateOf<String?>(null)
        private set
    var errorDescripcion by mutableStateOf<String?>(null)
        private set
    var errorCategoria by mutableStateOf<String?>(null)
        private set
    var errorCantidad by mutableStateOf<String?>(null)
        private set
    var errorUbicacion by mutableStateOf<String?>(null)
        private set

    var listo by mutableStateOf(false)   // ya se cargó el borrador
        private set
    var enviando by mutableStateOf(false)
        private set
    var aviso by mutableStateOf<String?>(null)
        private set

    /** Lo que se publicó desde el celular con su estado de envío. */
    val envios: StateFlow<List<BorradorPublicacionEntity>> =
        repo.envios(yo).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            val b = repo.borradorActual(yo)
            borrador = b
            titulo = b.titulo
            descripcion = b.descripcion
            categoria = b.categoria
            cantidad = b.cantidad
            ubicacion = b.ubicacion
            imagenLocal = b.imagenLocal?.takeIf { java.io.File(it).exists() }
            listo = true
        }
    }

    fun cambiarTitulo(v: String) { if (v.length <= 150) { titulo = v; errorTitulo = null; autoguardar() } }
    fun cambiarDescripcion(v: String) { if (v.length <= 3000) { descripcion = v; errorDescripcion = null; autoguardar() } }
    fun cambiarCategoria(v: String) { categoria = v; errorCategoria = null; autoguardar() }
    fun cambiarCantidad(v: String) {
        if (v.all { it.isDigit() } && v.length <= 5) { cantidad = v; errorCantidad = null; autoguardar() }
    }
    fun cambiarUbicacion(v: String) { if (v.length <= 200) { ubicacion = v; errorUbicacion = null; autoguardar() } }

    private fun actual(): BorradorPublicacionEntity? = borrador?.copy(
        titulo = titulo, descripcion = descripcion, categoria = categoria,
        cantidad = cantidad, ubicacion = ubicacion, imagenLocal = imagenLocal,
    )

    /** Copia la foto elegida al celular (reducida a JPG) para poder subirla aunque no haya internet. */
    fun elegirFoto(uri: Uri) {
        if (procesandoFoto) return
        viewModelScope.launch {
            procesandoFoto = true
            errorFoto = null
            val archivo = withContext(Dispatchers.IO) {
                runCatching { Fotos.guardarComprimida(AppContainer.appContext, uri) }.getOrNull()
            }
            if (archivo == null) {
                errorFoto = "No se pudo leer la imagen. Prueba con otra foto."
            } else if (archivo.length() > Fotos.TAMANO_MAXIMO) {
                Fotos.borrar(archivo.absolutePath)
                errorFoto = "La imagen supera el tamaño máximo de 2 MB."
            } else {
                Fotos.borrar(imagenLocal)   // reemplaza la anterior
                imagenLocal = archivo.absolutePath
                autoguardar()
            }
            procesandoFoto = false
        }
    }

    fun quitarFoto() {
        Fotos.borrar(imagenLocal)
        imagenLocal = null
        errorFoto = null
        autoguardar()
    }

    /** Guarda el borrador en Room medio segundo después de dejar de escribir. */
    private fun autoguardar() {
        aviso = null
        val b = actual() ?: return
        guardado?.cancel()
        guardado = viewModelScope.launch {
            delay(500)
            repo.guardarBorrador(b)
        }
    }

    fun publicar() {
        if (enviando || !listo) return
        val n = cantidad.toIntOrNull() ?: 0
        errorTitulo = if (titulo.isBlank()) "Escribe un título." else null
        errorDescripcion = if (descripcion.isBlank()) "Escribe una descripción." else null
        errorCategoria = if (categoria.isBlank()) "Elige una categoría." else null
        errorCantidad = if (n < 1) "La cantidad debe ser al menos 1." else null
        errorUbicacion = if (ubicacion.isBlank()) "Escribe la ubicación." else null
        if (listOf(errorTitulo, errorDescripcion, errorCategoria, errorCantidad, errorUbicacion).any { it != null }) return

        val b = actual() ?: return
        viewModelScope.launch {
            enviando = true
            guardado?.cancelAndJoin()
            repo.encolar(b)
            // Formulario nuevo y vacío para la siguiente publicación
            val nuevo = repo.borradorActual(yo)
            borrador = nuevo
            titulo = ""; descripcion = ""; categoria = ""; cantidad = "1"; ubicacion = ""; imagenLocal = null
            aviso = if (AppContainer.conectividad.enLinea.value) {
                "Enviando tu publicación… Quedará pendiente hasta que el administrador la apruebe."
            } else {
                "Sin conexión: tu publicación quedó guardada en el teléfono y se enviará sola al reconectar."
            }
            enviando = false
        }
    }

    fun reintentar(localId: String) { viewModelScope.launch { repo.reintentar(localId) } }
    fun descartar(localId: String) { viewModelScope.launch { repo.descartar(localId) } }
    fun limpiarEnviados() { viewModelScope.launch { repo.limpiarEnviados(yo) } }
}
