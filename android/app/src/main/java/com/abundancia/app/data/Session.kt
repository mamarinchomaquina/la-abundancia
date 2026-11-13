package com.abundancia.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.abundancia.app.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors

/** DataStore de preferencias (migra lo que había guardado la versión anterior en SharedPreferences). */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "abundancia_prefs",
    produceMigrations = { context -> listOf(SharedPreferencesMigration(context, "abundancia")) },
)

/**
 * Preferencias guardadas con DataStore:
 *  - sesión (token JWT y usuario), igual que el localStorage del panel web
 *  - IP del servidor
 *  - onboarding completado
 *  - categoría preferida (filtro con el que abre la lista de publicaciones)
 */
object Session {
    private lateinit var store: DataStore<Preferences>
    private val gson = Gson()

    // Un solo hilo para que las escrituras se guarden en el mismo orden en que se pidieron
    private val escrituras = CoroutineScope(
        SupervisorJob() + Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    )

    private val KEY_TOKEN = stringPreferencesKey("token")
    private val KEY_USUARIO = stringPreferencesKey("usuario")
    private val KEY_SERVIDOR = stringPreferencesKey("servidor")
    private val KEY_ONBOARDING = booleanPreferencesKey("onboarding_completado")
    private val KEY_CATEGORIA = stringPreferencesKey("categoria_preferida")
    private val KEY_AUTO = booleanPreferencesKey("conectar_automatico")

    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario

    private val _servidor = MutableStateFlow(normalizarServidor(BuildConfig.API_HOST))
    val servidor: StateFlow<String> = _servidor

    private val _onboarding = MutableStateFlow(false)
    val onboardingCompletado: StateFlow<Boolean> = _onboarding

    private val _categoria = MutableStateFlow(FILTRO_TODAS)
    val categoriaPreferida: StateFlow<String> = _categoria

    /** Buscar el servidor solo en la red si la IP guardada deja de responder. */
    private val _conectarAutomatico = MutableStateFlow(true)
    val conectarAutomatico: StateFlow<Boolean> = _conectarAutomatico

    @Volatile
    var token: String? = null
        private set

    /** Lectura inicial (una sola vez al abrir la app). */
    fun init(context: Context) {
        store = context.applicationContext.dataStore
        val prefs = runBlocking { store.data.first() }
        token = prefs[KEY_TOKEN]
        val json = prefs[KEY_USUARIO]
        _usuario.value = if (token != null && json != null) {
            runCatching { gson.fromJson(json, Usuario::class.java) }.getOrNull()
        } else {
            null
        }
        _servidor.value = normalizarServidor(prefs[KEY_SERVIDOR] ?: BuildConfig.API_HOST)
        _onboarding.value = prefs[KEY_ONBOARDING] ?: false
        _categoria.value = prefs[KEY_CATEGORIA] ?: FILTRO_TODAS
        _conectarAutomatico.value = prefs[KEY_AUTO] ?: true
    }

    private fun guardar(cambio: (MutablePreferences) -> Unit) {
        escrituras.launch { store.edit { cambio(it) } }
    }

    fun iniciar(nuevoToken: String, nuevoUsuario: Usuario) {
        token = nuevoToken
        _usuario.value = nuevoUsuario
        val json = gson.toJson(nuevoUsuario)
        guardar {
            it[KEY_TOKEN] = nuevoToken
            it[KEY_USUARIO] = json
        }
    }

    fun cerrar() {
        token = null
        _usuario.value = null
        guardar {
            it.remove(KEY_TOKEN)
            it.remove(KEY_USUARIO)
        }
    }

    fun cambiarServidor(valor: String) {
        val v = normalizarServidor(valor)
        _servidor.value = v
        guardar { it[KEY_SERVIDOR] = v }
    }

    fun completarOnboarding() {
        _onboarding.value = true
        guardar { it[KEY_ONBOARDING] = true }
    }

    fun cambiarCategoriaPreferida(categoria: String) {
        _categoria.value = categoria
        guardar { it[KEY_CATEGORIA] = categoria }
    }

    fun cambiarConectarAutomatico(activo: Boolean) {
        _conectarAutomatico.value = activo
        guardar { it[KEY_AUTO] = activo }
    }

    fun baseUrl(): String = baseUrlDe(_servidor.value)
}

/** Acepta "192.168.1.5", "192.168.1.5:3000" o "http://192.168.1.5:3000/". Devuelve "ip:puerto". */
fun normalizarServidor(valor: String): String {
    var v = valor.trim()
        .removePrefix("http://")
        .removePrefix("https://")
        .trimEnd('/')
    if (v.isEmpty()) v = BuildConfig.API_HOST
    if (!v.contains(':')) v = "$v:${BuildConfig.API_PORT}"
    return v
}

fun baseUrlDe(servidor: String): String = "http://${normalizarServidor(servidor)}/"

/** URL completa de una foto guardada en el backend ("/uploads/abc.jpg"). */
fun urlImagen(ruta: String?): String? =
    if (ruta.isNullOrBlank()) null else Session.baseUrl() + ruta.removePrefix("/")
