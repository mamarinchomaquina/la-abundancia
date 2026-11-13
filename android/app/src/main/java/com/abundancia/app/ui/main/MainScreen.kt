package com.abundancia.app.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.abundancia.app.data.AppContainer
import com.abundancia.app.data.Session
import com.abundancia.app.data.Usuario
import com.abundancia.app.data.repo.totalPorEnviar
import com.abundancia.app.ui.ajustes.AjustesScreen
import com.abundancia.app.ui.auth.LoginScreen
import com.abundancia.app.ui.auth.RegistroScreen
import com.abundancia.app.ui.components.BannerConexion
import com.abundancia.app.ui.onboarding.OnboardingScreen
import com.abundancia.app.ui.perfil.PerfilScreen
import com.abundancia.app.ui.publicaciones.DetallePublicacionScreen
import com.abundancia.app.ui.publicaciones.NuevaPublicacionScreen
import com.abundancia.app.ui.publicaciones.PublicacionesScreen
import com.abundancia.app.ui.solicitudes.SolicitudesScreen
import com.abundancia.app.ui.theme.Blanco
import com.abundancia.app.ui.theme.Borde
import com.abundancia.app.ui.theme.Fondo
import com.abundancia.app.ui.theme.Morado
import com.abundancia.app.ui.theme.MoradoSuave
import com.abundancia.app.ui.theme.TextoSuave

/** Raíz: onboarding (solo la primera vez) → login/registro → app con pestañas. */
@Composable
fun AbundanciaApp() {
    val onboardingCompletado by Session.onboardingCompletado.collectAsState()
    val usuario by Session.usuario.collectAsState()
    val u = usuario

    when {
        !onboardingCompletado -> OnboardingScreen(onTerminar = { Session.completarOnboarding() })

        u == null -> {
            var registro by rememberSaveable { mutableStateOf(false) }
            var ajustes by rememberSaveable { mutableStateOf(false) }
            when {
                ajustes -> {
                    BackHandler { ajustes = false }
                    AjustesScreen(onAtras = { ajustes = false })
                }
                registro -> RegistroScreen(onVolver = { registro = false }, onAjustes = { ajustes = true })
                else -> LoginScreen(onIrRegistro = { registro = true }, onAjustes = { ajustes = true })
            }
        }

        else -> {
            // ViewModels propios de cada sesión: si cambia el usuario, se crean de nuevo
            val owner = remember(u.id) {
                object : ViewModelStoreOwner {
                    override val viewModelStore = ViewModelStore()
                }
            }
            DisposableEffect(owner) {
                onDispose { owner.viewModelStore.clear() }
            }
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                MainScreen(u)
            }
        }
    }
}

enum class Pestana(val titulo: String, val icono: ImageVector) {
    INICIO("Inicio", Icons.Filled.Home),
    PUBLICAR("Publicar", Icons.Filled.AddCircle),
    SOLICITUDES("Solicitudes", Icons.AutoMirrored.Filled.List),
    PERFIL("Perfil", Icons.Filled.Person),
    AJUSTES("Ajustes", Icons.Filled.Settings),
}

@Composable
fun MainScreen(usuario: Usuario) {
    var pestana by rememberSaveable { mutableStateOf(Pestana.INICIO) }
    var detalleId by rememberSaveable { mutableStateOf<String?>(null) }

    val enLinea by AppContainer.conectividad.enLinea.collectAsState()
    val porEnviarFlow = remember(usuario.id) {
        totalPorEnviar(AppContainer.publicaciones, AppContainer.solicitudes, usuario.id)
    }
    val porEnviar by porEnviarFlow.collectAsState(initial = 0)

    val abierto = detalleId
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            if (abierto != null) {
                BackHandler { detalleId = null }
                DetallePublicacionScreen(id = abierto, usuario = usuario, onCerrar = { detalleId = null })
            } else {
                if (pestana != Pestana.INICIO) {
                    BackHandler { pestana = Pestana.INICIO }
                }
                Scaffold(
                    containerColor = Fondo,
                    bottomBar = {
                        Column {
                            HorizontalDivider(color = Borde)
                            NavigationBar(containerColor = Blanco, tonalElevation = 0.dp) {
                                Pestana.entries.forEach { p ->
                                    NavigationBarItem(
                                        selected = pestana == p,
                                        onClick = { pestana = p },
                                        icon = { Icon(p.icono, contentDescription = p.titulo) },
                                        label = { Text(p.titulo, fontSize = 11.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Morado,
                                            selectedTextColor = Morado,
                                            indicatorColor = MoradoSuave,
                                            unselectedIconColor = TextoSuave,
                                            unselectedTextColor = TextoSuave,
                                        ),
                                    )
                                }
                            }
                        }
                    },
                ) { padding ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(bottom = padding.calculateBottomPadding())
                    ) {
                        Box(Modifier.weight(1f)) {
                            when (pestana) {
                                Pestana.INICIO -> PublicacionesScreen(usuario, onAbrir = { detalleId = it })
                                Pestana.PUBLICAR -> NuevaPublicacionScreen()
                                Pestana.SOLICITUDES -> SolicitudesScreen(usuario)
                                Pestana.PERFIL -> PerfilScreen(usuario)
                                Pestana.AJUSTES -> AjustesScreen()
                            }
                        }
                        // Aviso de "sin conexión" / "enviando pendientes", justo encima de la barra inferior
                        BannerConexion(enLinea = enLinea, porEnviar = porEnviar)
                    }
                }
            }
        }
    }
}
