package com.abundancia.app.ui.ajustes

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abundancia.app.BuildConfig
import com.abundancia.app.data.AppContainer
import com.abundancia.app.data.Session
import com.abundancia.app.data.normalizarServidor
import com.abundancia.app.data.sync.BuscadorServidor
import com.abundancia.app.data.sync.Sincronizador
import com.abundancia.app.ui.components.BotonPrincipal
import com.abundancia.app.ui.components.BotonSecundario
import com.abundancia.app.ui.components.CampoTexto
import com.abundancia.app.ui.components.EncabezadoMorado
import com.abundancia.app.ui.components.FiltroChip
import com.abundancia.app.ui.components.MensajeError
import com.abundancia.app.ui.components.MensajeExito
import com.abundancia.app.ui.components.Tarjeta
import com.abundancia.app.ui.publicaciones.PublicacionesViewModel
import com.abundancia.app.ui.theme.Amarillo
import com.abundancia.app.ui.theme.Blanco
import com.abundancia.app.ui.theme.Borde
import com.abundancia.app.ui.theme.Morado
import com.abundancia.app.ui.theme.Rojo
import com.abundancia.app.ui.theme.Texto
import com.abundancia.app.ui.theme.TextoSuave
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AjustesViewModel : ViewModel() {
    enum class Conexion { REVISANDO, CONECTADO, SIN_RESPUESTA }

    val servidor: StateFlow<String> = Session.servidor
    val conectarAutomatico: StateFlow<Boolean> = Session.conectarAutomatico
    val categoriaPreferida: StateFlow<String> = Session.categoriaPreferida
    val estadoBusqueda: StateFlow<BuscadorServidor.Estado> = BuscadorServidor.estado
    val mensajeBusqueda: StateFlow<String?> = BuscadorServidor.mensaje

    var ip by mutableStateOf(Session.servidor.value)
        private set
    var conexion by mutableStateOf(Conexion.REVISANDO)
        private set
    var probando by mutableStateOf(false)
        private set
    /** Resultado de "Probar" o "Guardar": (éxito, mensaje) */
    var resultado by mutableStateOf<Pair<Boolean, String>?>(null)
        private set

    init { revisarConexion() }

    fun cambiarIp(v: String) { ip = v.trim(); resultado = null }

    /** Revisa si el servidor guardado responde (indicador de arriba). */
    fun revisarConexion() {
        viewModelScope.launch {
            conexion = Conexion.REVISANDO
            val ok = withContext(Dispatchers.IO) { BuscadorServidor.responde(Session.servidor.value) }
            conexion = if (ok) Conexion.CONECTADO else Conexion.SIN_RESPUESTA
        }
    }

    fun probar() {
        if (probando) return
        viewModelScope.launch {
            probando = true
            resultado = null
            val destino = normalizarServidor(ip)
            val ok = withContext(Dispatchers.IO) { BuscadorServidor.responde(destino) }
            resultado = if (ok) true to "El servidor responde en $destino. Toca Guardar para usarlo."
            else false to "No hay respuesta en $destino. Revisa la IP, el Wi-Fi y que el backend esté encendido."
            probando = false
        }
    }

    fun guardar() {
        if (ip.isBlank()) { resultado = false to "Escribe la IP del PC."; return }
        Session.cambiarServidor(ip)
        ip = Session.servidor.value
        resultado = true to "Guardado. La app usará http://${Session.servidor.value}"
        Sincronizador.programar(AppContainer.appContext)
        revisarConexion()
    }

    fun buscarAutomaticamente() {
        if (estadoBusqueda.value == BuscadorServidor.Estado.BUSCANDO) return
        viewModelScope.launch {
            resultado = null
            val encontrado = BuscadorServidor.buscar(AppContainer.appContext)
            if (encontrado != null) {
                ip = Session.servidor.value
                Sincronizador.programar(AppContainer.appContext)
            }
            revisarConexion()
        }
    }

    fun cambiarAutomatico(activo: Boolean) = Session.cambiarConectarAutomatico(activo)
    fun elegirCategoria(c: String) = Session.cambiarCategoriaPreferida(c)
}

/**
 * Ajustes: conexión con el servidor (IP manual o búsqueda automática) y preferencias.
 * onAtras != null cuando se abre desde el login (pantalla completa con flecha de volver).
 */
@Composable
fun AjustesScreen(onAtras: (() -> Unit)? = null, vm: AjustesViewModel = viewModel()) {
    val servidor by vm.servidor.collectAsState()
    val automatico by vm.conectarAutomatico.collectAsState()
    val categoria by vm.categoriaPreferida.collectAsState()
    val estadoBusqueda by vm.estadoBusqueda.collectAsState()
    val mensajeBusqueda by vm.mensajeBusqueda.collectAsState()
    val buscando = estadoBusqueda == BuscadorServidor.Estado.BUSCANDO

    // Si la búsqueda automática cambió la IP mientras la pantalla estaba abierta
    LaunchedEffect(servidor) { vm.revisarConexion() }

    Column(Modifier.fillMaxSize()) {
        EncabezadoMorado("Ajustes", "Conexión y preferencias", onAtras = onAtras)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Estado de la conexión ───────────────────────────────────────
            Tarjeta {
                Text("Servidor", fontWeight = FontWeight.SemiBold, color = Texto)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (color, texto) = when (vm.conexion) {
                        AjustesViewModel.Conexion.REVISANDO -> TextoSuave to "Revisando…"
                        AjustesViewModel.Conexion.CONECTADO -> Morado to "Conectado"
                        AjustesViewModel.Conexion.SIN_RESPUESTA -> Rojo to "Sin respuesta"
                    }
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(texto, color = color, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("· http://$servidor", color = TextoSuave, fontSize = 13.sp, modifier = Modifier.weight(1f))
                }
            }

            // ── Búsqueda automática ─────────────────────────────────────────
            Tarjeta {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Conectar automáticamente", fontWeight = FontWeight.SemiBold, color = Texto)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Si el servidor deja de responder (por ejemplo, porque cambió la IP del PC), " +
                                "la app lo busca sola en la red Wi-Fi.",
                            fontSize = 13.sp,
                            color = TextoSuave,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = automatico,
                        onCheckedChange = vm::cambiarAutomatico,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Blanco,
                            checkedTrackColor = Morado,
                            uncheckedThumbColor = Blanco,
                            uncheckedTrackColor = Borde,
                            uncheckedBorderColor = Borde,
                        ),
                    )
                }
                Spacer(Modifier.height(14.dp))
                BotonPrincipal(
                    texto = "Buscar servidor ahora",
                    onClick = vm::buscarAutomaticamente,
                    cargando = buscando,
                )
                val msg = mensajeBusqueda
                if (msg != null) {
                    Spacer(Modifier.height(10.dp))
                    when (estadoBusqueda) {
                        BuscadorServidor.Estado.ENCONTRADO -> MensajeExito(msg)
                        BuscadorServidor.Estado.NO_ENCONTRADO -> MensajeError(msg)
                        BuscadorServidor.Estado.BUSCANDO -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(16.dp), color = Morado, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(msg, fontSize = 13.sp, color = TextoSuave)
                        }
                        else -> {}
                    }
                }
            }

            // ── IP manual ───────────────────────────────────────────────────
            Tarjeta {
                Text("IP del servidor (manual)", fontWeight = FontWeight.SemiBold, color = Texto)
                Spacer(Modifier.height(4.dp))
                Text(
                    "En el PC abre una terminal, escribe ipconfig y copia la \"Dirección IPv4\" del Wi-Fi.",
                    fontSize = 13.sp,
                    color = TextoSuave,
                )
                Spacer(Modifier.height(12.dp))
                CampoTexto(
                    etiqueta = null,
                    valor = vm.ip,
                    onCambio = vm::cambiarIp,
                    placeholder = "192.168.1.100 o 192.168.1.100:3000",
                    teclado = KeyboardType.Uri,
                )
                val r = vm.resultado
                if (r != null) {
                    Spacer(Modifier.height(10.dp))
                    if (r.first) MensajeExito(r.second) else MensajeError(r.second)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BotonSecundario("Probar", vm::probar, modifier = Modifier.weight(1f), cargando = vm.probando)
                    BotonPrincipal("Guardar", vm::guardar, modifier = Modifier.weight(1f))
                }
            }

            // ── Preferencias ────────────────────────────────────────────────
            Tarjeta {
                Text("Categoría preferida", fontWeight = FontWeight.SemiBold, color = Texto)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Filtro con el que se abre la lista de publicaciones. Se guarda en el teléfono.",
                    fontSize = 13.sp,
                    color = TextoSuave,
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PublicacionesViewModel.FILTROS.forEach { f ->
                        FiltroChip(texto = f, activo = categoria == f, onClick = { vm.elegirCategoria(f) })
                    }
                }
            }

            Text(
                "La Abundancia · versión ${BuildConfig.VERSION_NAME}",
                fontSize = 12.sp,
                color = TextoSuave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
            if (!automatico) {
                Text(
                    "Con la conexión automática apagada, la app solo usa la IP que guardes aquí.",
                    fontSize = 12.sp,
                    color = Amarillo,
                )
            }
        }
    }
}
