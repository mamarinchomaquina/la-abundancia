package com.abundancia.app.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.abundancia.app.data.AppContainer
import com.abundancia.app.data.Session
import com.abundancia.app.data.repo.ResultadoEnvio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.TimeUnit

/** Observa si el celular tiene red (Wi-Fi o datos). Al reconectar dispara el envío de pendientes. */
class Conectividad(private val context: Context) {
    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _enLinea = MutableStateFlow(hayRed())
    val enLinea: StateFlow<Boolean> = _enLinea

    init {
        cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _enLinea.value = true
                Sincronizador.programar(context)
            }

            override fun onLost(network: Network) {
                _enLinea.value = false
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                _enLinea.value = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
        })
    }

    private fun hayRed(): Boolean {
        val red = cm.activeNetwork ?: return false
        val capacidades = cm.getNetworkCapabilities(red) ?: return false
        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

/** Programa el envío de todo lo pendiente. WorkManager espera a que haya red, aunque la app esté cerrada. */
object Sincronizador {
    private const val NOMBRE = "enviar_pendientes"

    fun programar(context: Context) {
        val trabajo = OneTimeWorkRequestBuilder<EnviarPendientesWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(NOMBRE, ExistingWorkPolicy.APPEND_OR_REPLACE, trabajo)
    }
}

class EnviarPendientesWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val usuario = Session.usuario.value ?: return Result.success()
        if (Session.token == null) return Result.success()

        val publicaciones = AppContainer.publicaciones.enviarPendientes(usuario.id)
        val solicitudes = AppContainer.solicitudes.enviarPendientes(usuario.id)

        return if (publicaciones == ResultadoEnvio.REINTENTAR || solicitudes == ResultadoEnvio.REINTENTAR) {
            Result.retry()   // el servidor no respondió: WorkManager reintenta más tarde
        } else {
            Result.success()
        }
    }
}
