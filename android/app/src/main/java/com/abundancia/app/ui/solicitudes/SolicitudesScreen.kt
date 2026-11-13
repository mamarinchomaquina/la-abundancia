package com.abundancia.app.ui.solicitudes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abundancia.app.data.AppContainer
import com.abundancia.app.data.Session
import com.abundancia.app.data.Usuario
import com.abundancia.app.data.local.EstadoSync
import com.abundancia.app.data.local.SolicitudPendienteEntity
import com.abundancia.app.data.repo.SolicitudItem
import com.abundancia.app.ui.components.AvisoCopiaLocal
import com.abundancia.app.ui.components.BotonPrincipal
import com.abundancia.app.ui.components.BotonSecundario
import com.abundancia.app.ui.components.Cargando
import com.abundancia.app.ui.components.ChipCategoria
import com.abundancia.app.ui.components.EncabezadoMorado
import com.abundancia.app.ui.components.EstadoBadge
import com.abundancia.app.ui.components.EstadoSyncBadge
import com.abundancia.app.ui.components.EstadoVacio
import com.abundancia.app.ui.components.FiltroChip
import com.abundancia.app.ui.components.MensajeError
import com.abundancia.app.ui.components.Tarjeta
import com.abundancia.app.ui.components.formatearFecha
import com.abundancia.app.ui.theme.Borde
import com.abundancia.app.ui.theme.Fondo
import com.abundancia.app.ui.theme.Rojo
import com.abundancia.app.ui.theme.RojoSuave
import com.abundancia.app.ui.theme.Texto
import com.abundancia.app.ui.theme.TextoSuave
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SolicitudesViewModel : ViewModel() {
    private val repo = AppContainer.solicitudes
    private val yo = Session.usuario.value?.id.orEmpty()

    val solicitudes: StateFlow<List<SolicitudItem>> =
        repo.solicitudes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val pendientes: StateFlow<List<SolicitudPendienteEntity>> =
        repo.pendientes(yo).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val ultimaActualizacion: StateFlow<Long?> =
        repo.ultimaActualizacion.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    var recibidas by mutableStateOf(true)
        private set
    var cargando by mutableStateOf(false)
        private set
    var errorServidor by mutableStateOf<String?>(null)
        private set
    var errorAccion by mutableStateOf<String?>(null)
        private set
    var procesando by mutableStateOf<String?>(null)
        private set

    init { refrescar() }

    fun verRecibidas(v: Boolean) { recibidas = v }

    fun refrescar() {
        if (cargando) return
        viewModelScope.launch {
            cargando = true
            errorServidor = repo.refrescar().exceptionOrNull()?.message
            cargando = false
        }
    }

    /** Recibidas: solicitudes a MIS publicaciones. Enviadas: las que YO hice (confirmadas por el servidor). */
    fun visibles(lista: List<SolicitudItem>): List<SolicitudItem> =
        lista.filter { if (recibidas) it.duenoId == yo else it.solicitanteId == yo }

    fun resolver(id: String, estado: String) {
        if (procesando != null) return
        viewModelScope.launch {
            procesando = id
            errorAccion = null
            repo.resolver(id, estado).onFailure { errorAccion = it.message }
            procesando = null
        }
    }

    fun reintentar(localId: String) { viewModelScope.launch { repo.reintentar(localId) } }
    fun descartar(localId: String) { viewModelScope.launch { repo.descartar(localId) } }
}

@Composable
fun SolicitudesScreen(usuario: Usuario, vm: SolicitudesViewModel = viewModel()) {
    val lista by vm.solicitudes.collectAsState()
    val pendientes by vm.pendientes.collectAsState()
    val ultima by vm.ultimaActualizacion.collectAsState()
    val enLinea by AppContainer.conectividad.enLinea.collectAsState()
    val visibles = remember(lista, vm.recibidas, usuario.id) { vm.visibles(lista) }
    // En "Enviadas" se muestran también las que aún no llegan al servidor
    val sinConfirmar = if (vm.recibidas) emptyList() else pendientes.filter { it.estadoSync != EstadoSync.ENVIADO }

    val servidor by Session.servidor.collectAsState()
    // Al recuperar la red o cuando la app encuentra el servidor en otra IP, se actualiza sola
    LaunchedEffect(enLinea, servidor) { if (enLinea) vm.refrescar() }

    Column(Modifier.fillMaxSize()) {
        EncabezadoMorado("Solicitudes", "Quién pide y qué pediste") {
            if (vm.cargando) {
                CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
            } else {
                IconButton(onClick = vm::refrescar) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Actualizar", tint = Color.White)
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color.White)
                        .border(1.dp, Borde, RoundedCornerShape(9.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    FiltroChip("Recibidas", activo = vm.recibidas, onClick = { vm.verRecibidas(true) })
                    FiltroChip("Enviadas", activo = !vm.recibidas, onClick = { vm.verRecibidas(false) })
                }
            }

            val errorServidor = vm.errorServidor
            if (errorServidor != null && !vm.cargando) item { AvisoCopiaLocal(errorServidor, ultima) }

            val ea = vm.errorAccion
            if (ea != null) item { MensajeError(ea) }

            if (sinConfirmar.isNotEmpty()) {
                item { Text("Guardadas en el teléfono", fontWeight = FontWeight.SemiBold, color = Texto) }
                items(sinConfirmar, key = { "local-" + it.localId }) { s ->
                    TarjetaPendiente(s, onReintentar = { vm.reintentar(s.localId) }, onDescartar = { vm.descartar(s.localId) })
                }
            }

            when {
                vm.cargando && lista.isEmpty() -> item { Cargando() }
                visibles.isEmpty() && sinConfirmar.isEmpty() -> item {
                    EstadoVacio(
                        if (vm.recibidas) "Aún nadie ha solicitado tus publicaciones."
                        else "Aún no has solicitado ninguna donación."
                    )
                }
                else -> items(visibles, key = { it.id }) { s ->
                    TarjetaSolicitud(
                        s = s,
                        recibida = vm.recibidas,
                        enLinea = enLinea,
                        procesando = vm.procesando == s.id,
                        onAceptar = { vm.resolver(s.id, "aceptada") },
                        onRechazar = { vm.resolver(s.id, "rechazada") },
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaPendiente(s: SolicitudPendienteEntity, onReintentar: () -> Unit, onDescartar: () -> Unit) {
    Tarjeta {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                s.publicacionTitulo,
                modifier = Modifier.weight(1f),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Texto,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(8.dp))
            EstadoSyncBadge(s.estadoSync)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (s.estadoSync == EstadoSync.ERROR) s.error ?: "El servidor la rechazó."
            else s.error ?: "Se enviará automáticamente cuando haya conexión.",
            fontSize = 12.sp,
            color = if (s.estadoSync == EstadoSync.ERROR) Rojo else TextoSuave,
        )
        if (s.estadoSync == EstadoSync.ERROR || s.estadoSync == EstadoSync.PENDIENTE) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BotonSecundario("Reintentar", onReintentar, modifier = Modifier.weight(1f))
                BotonSecundario("Descartar", onDescartar, modifier = Modifier.weight(1f), color = Rojo, fondo = RojoSuave)
            }
        }
    }
}

@Composable
private fun TarjetaSolicitud(
    s: SolicitudItem,
    recibida: Boolean,
    enLinea: Boolean,
    procesando: Boolean,
    onAceptar: () -> Unit,
    onRechazar: () -> Unit,
) {
    Tarjeta {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                s.publicacionTitulo,
                modifier = Modifier.weight(1f),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Texto,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(8.dp))
            EstadoBadge(s.estado)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (s.categoria.isNotEmpty()) ChipCategoria(s.categoria)
            Text(
                if (recibida) "Solicitada por ${s.solicitanteNombre.ifBlank { "un usuario" }}" else s.ubicacion,
                fontSize = 12.sp,
                color = TextoSuave,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (s.mensaje.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                "“${s.mensaje}”",
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic,
                color = Texto,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(9.dp))
                    .background(Fondo)
                    .padding(10.dp),
            )
        }
        val fecha = formatearFecha(s.createdAt)
        if (fecha.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(fecha, fontSize = 11.5.sp, color = TextoSuave)
        }
        if (recibida && s.estado == "pendiente") {
            Spacer(Modifier.height(12.dp))
            if (!enLinea) {
                Text("Para aceptar o rechazar necesitas conexión.", fontSize = 12.sp, color = TextoSuave)
                Spacer(Modifier.height(8.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BotonPrincipal(
                    texto = "Aceptar",
                    onClick = onAceptar,
                    cargando = procesando,
                    enabled = enLinea,
                    modifier = Modifier.weight(1f),
                )
                BotonSecundario(
                    texto = "Rechazar",
                    onClick = onRechazar,
                    enabled = enLinea && !procesando,
                    color = Rojo,
                    fondo = RojoSuave,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
