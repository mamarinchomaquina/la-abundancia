package com.abundancia.app.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import com.abundancia.app.BuildConfig
import com.abundancia.app.data.Session
import com.abundancia.app.data.normalizarServidor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * Encuentra el backend en la red Wi-Fi sin escribir la IP a mano:
 *  1) escucha el anuncio UDP que envía el backend cada 2 s (puerto 41234)
 *  2) si no llega, recorre las IP de la red probando GET /health en el puerto 3000
 * Cuando lo encuentra, guarda la nueva IP en DataStore.
 */
object BuscadorServidor {
    enum class Estado { INACTIVO, BUSCANDO, ENCONTRADO, NO_ENCONTRADO }

    private const val PUERTO_ANUNCIO = 41234
    private const val MAX_IPS = 4096

    private val _estado = MutableStateFlow(Estado.INACTIVO)
    val estado: StateFlow<Estado> = _estado

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    @Volatile
    private var ultimoIntentoAutomatico = 0L

    private val clienteRapido: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(450, TimeUnit.MILLISECONDS)
            .readTimeout(800, TimeUnit.MILLISECONDS)
            .build()
    }

    /** ¿Hay un backend de La Abundancia en esa dirección ("ip:puerto")? */
    fun responde(servidor: String): Boolean = runCatching {
        val url = "http://${normalizarServidor(servidor)}/health"
        clienteRapido.newCall(Request.Builder().url(url).build()).execute().use { r ->
            r.body?.string()?.contains("\"database\"") == true
        }
    }.getOrDefault(false)

    /** Búsqueda completa (la usa la pantalla de Ajustes). Devuelve "ip:puerto" o null. */
    suspend fun buscar(context: Context): String? = mutex.withLock {
        withContext(Dispatchers.IO) {
            _estado.value = Estado.BUSCANDO
            _mensaje.value = "Buscando el servidor en la red Wi-Fi…"
            val encontrado = escucharAnuncio(context) ?: escanearRed(context)
            if (encontrado != null) {
                Session.cambiarServidor(encontrado)
                _estado.value = Estado.ENCONTRADO
                _mensaje.value = "Servidor encontrado en $encontrado"
            } else {
                _estado.value = Estado.NO_ENCONTRADO
                _mensaje.value = "No se encontró el servidor. Revisa que el backend esté encendido, " +
                    "que el celular esté en el mismo Wi-Fi que el PC y que el firewall permita el puerto 3000."
            }
            encontrado
        }
    }

    /**
     * Se llama sola cuando una petición no logra conectar. Máximo una vez por minuto,
     * y solo si la opción "Conectar automáticamente" está activa.
     */
    fun buscarEnSegundoPlano(context: Context) {
        if (!Session.conectarAutomatico.value || mutex.isLocked) return
        val ahora = System.currentTimeMillis()
        if (ahora - ultimoIntentoAutomatico < 60_000) return
        ultimoIntentoAutomatico = ahora
        scope.launch {
            if (responde(Session.servidor.value)) return@launch   // fue un fallo momentáneo
            if (buscar(context) != null) Sincronizador.programar(context)
        }
    }

    // ── 1) Anuncio UDP del backend ───────────────────────────────────────────
    private fun escucharAnuncio(context: Context, tiempoMs: Int = 4500): String? {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val candado = wifi.createMulticastLock("abundancia-busqueda").apply {
            setReferenceCounted(false)
            acquire()
        }
        return try {
            DatagramSocket(null).use { socket ->
                socket.reuseAddress = true
                socket.broadcast = true
                socket.bind(InetSocketAddress(PUERTO_ANUNCIO))
                val fin = System.currentTimeMillis() + tiempoMs
                val buffer = ByteArray(512)
                var resultado: String? = null
                while (resultado == null) {
                    val restante = fin - System.currentTimeMillis()
                    if (restante <= 0) break
                    socket.soTimeout = restante.toInt()
                    val paquete = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(paquete)
                    } catch (e: SocketTimeoutException) {
                        break
                    }
                    val texto = String(paquete.data, 0, paquete.length, Charsets.UTF_8)
                    if (texto.contains("la-abundancia")) {
                        val puerto = Regex("\"puerto\"\\s*:\\s*(\\d+)").find(texto)?.groupValues?.get(1)
                            ?: BuildConfig.API_PORT
                        val ip = paquete.address?.hostAddress
                        if (ip != null) resultado = "$ip:$puerto"
                    }
                }
                resultado
            }
        } catch (e: Exception) {
            null
        } finally {
            candado.release()
        }
    }

    // ── 2) Recorrer las IP de la red ─────────────────────────────────────────
    private suspend fun escanearRed(context: Context): String? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val red = cm.activeNetwork ?: return null
        val enlace = cm.getLinkProperties(red)?.linkAddresses
            ?.firstOrNull { it.address is Inet4Address } ?: return null

        val propia = aEntero(enlace.address.address)
        val prefijo = enlace.prefixLength.coerceIn(20, 30)   // como máximo 4096 direcciones
        val mascara = -1 shl (32 - prefijo)
        val base = propia and mascara
        val total = minOf(1 shl (32 - prefijo), MAX_IPS)

        // Primero las IP más cercanas a la del celular
        val candidatas = (1 until total - 1)
            .map { base + it }
            .filter { it != propia }
            .sortedBy { kotlin.math.abs(it - propia) }

        val puerto = BuildConfig.API_PORT
        return coroutineScope {
            val semaforo = Semaphore(64)
            val hallado = CompletableDeferred<String?>()
            val trabajos = candidatas.map { ip ->
                launch(Dispatchers.IO) {
                    semaforo.withPermit {
                        if (!hallado.isCompleted) {
                            val servidor = "${aTexto(ip)}:$puerto"
                            if (responde(servidor)) hallado.complete(servidor)
                        }
                    }
                }
            }
            launch {
                trabajos.joinAll()
                hallado.complete(null)
            }
            val resultado = hallado.await()
            trabajos.forEach { it.cancel() }
            resultado
        }
    }

    private fun aEntero(bytes: ByteArray): Int =
        ((bytes[0].toInt() and 0xFF) shl 24) or ((bytes[1].toInt() and 0xFF) shl 16) or
            ((bytes[2].toInt() and 0xFF) shl 8) or (bytes[3].toInt() and 0xFF)

    private fun aTexto(ip: Int): String =
        "${(ip ushr 24) and 0xFF}.${(ip ushr 16) and 0xFF}.${(ip ushr 8) and 0xFF}.${ip and 0xFF}"
}
