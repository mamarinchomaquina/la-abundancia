package com.abundancia.app.data

import com.abundancia.app.data.sync.BuscadorServidor
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

object ApiClient {
    private var urlActual: String? = null
    private var api: ApiService? = null

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val token = Session.token
                val request = if (token != null) {
                    original.newBuilder().header("Authorization", "Bearer $token").build()
                } else {
                    original
                }
                val response = chain.proceed(request)
                // Token vencido o inválido (expira a las 8 h): se cierra la sesión
                if (response.code == 401 && token != null &&
                    !original.url.encodedPath.contains("auth/login")
                ) {
                    Session.cerrar()
                }
                response
            }
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()
    }

    @Synchronized
    fun api(baseUrl: String = Session.baseUrl()): ApiService {
        val actual = api
        if (actual != null && urlActual == baseUrl) return actual
        val nuevo = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(http)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
        api = nuevo
        urlActual = baseUrl
        return nuevo
    }
}

/** Ejecuta una llamada al backend y convierte cualquier error en un mensaje entendible. */
suspend fun <T> llamar(bloque: suspend ApiService.() -> T): Result<T> = withContext(Dispatchers.IO) {
    try {
        Result.success(ApiClient.api().bloque())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Si no hubo forma de llegar al servidor, se busca solo en la red (si está activado)
        if (e is ConnectException || e is SocketTimeoutException || e is NoRouteToHostException) {
            BuscadorServidor.buscarEnSegundoPlano(AppContainer.appContext)
        }
        Result.failure(Exception(mensajeError(e), e))
    }
}

/** Prueba si el backend responde en esa IP (usa GET /health). */
suspend fun probarServidor(servidor: String): Result<Unit> = withContext(Dispatchers.IO) {
    try {
        ApiClient.api(baseUrlDe(servidor)).health()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        // Respondió algo: el servidor sí es alcanzable
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Exception(mensajeError(e, normalizarServidor(servidor)), e))
    }
}

fun mensajeError(e: Throwable, servidor: String = Session.servidor.value): String = when (e) {
    is HttpException -> {
        val cuerpo = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
        val mensaje = cuerpo?.let {
            runCatching { JsonParser.parseString(it).asJsonObject.get("error")?.asString }.getOrNull()
        }
        when {
            !mensaje.isNullOrBlank() -> mensaje
            e.code() == 401 -> "Tu sesión expiró. Ingresa de nuevo."
            e.code() == 403 -> "No tienes permiso para esta acción."
            e.code() == 404 -> "No se encontró el recurso solicitado."
            else -> "Error del servidor (${e.code()})."
        }
    }
    is ConnectException, is SocketTimeoutException, is NoRouteToHostException, is UnknownHostException ->
        "No se pudo conectar con $servidor. Revisa que el backend esté encendido, " +
            "que el celular esté en el mismo Wi-Fi que el PC y que la IP sea la correcta."
    is IOException -> "Problema de red: ${e.message ?: "sin conexión"}"
    is IllegalArgumentException -> "La dirección del servidor no es válida."
    else -> e.message ?: "Ocurrió un error inesperado."
}
