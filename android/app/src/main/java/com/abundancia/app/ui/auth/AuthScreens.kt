package com.abundancia.app.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abundancia.app.R
import com.abundancia.app.data.Session
import com.abundancia.app.ui.components.BotonPrincipal
import com.abundancia.app.ui.components.CampoTexto
import com.abundancia.app.ui.components.MensajeError
import com.abundancia.app.ui.components.ServidorDialog
import com.abundancia.app.ui.theme.Blanco
import com.abundancia.app.ui.theme.GradienteLogin
import com.abundancia.app.ui.theme.Texto
import com.abundancia.app.ui.theme.TextoSuave

@Composable
fun LoginScreen(onIrRegistro: () -> Unit, onAjustes: () -> Unit, vm: LoginViewModel = viewModel()) {
    FondoAuth(textoPie = "¿No tienes cuenta?", accionPie = "Regístrate", onPie = onIrRegistro, onAjustes = onAjustes) {
        Marca()
        Text("Bienvenido", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Texto)
        Spacer(Modifier.height(4.dp))
        Text("Ingresa para donar y solicitar ayuda", fontSize = 13.sp, color = TextoSuave)
        Spacer(Modifier.height(24.dp))

        CampoTexto(
            "Correo electrónico", vm.email, vm::cambiarEmail,
            placeholder = "tu@correo.com", teclado = KeyboardType.Email, error = vm.errorEmail,
        )
        Spacer(Modifier.height(14.dp))
        CampoTexto(
            "Contraseña", vm.password, vm::cambiarPassword,
            placeholder = "••••••••", esPassword = true, error = vm.errorPassword,
        )

        val e = vm.error
        if (e != null) {
            Spacer(Modifier.height(14.dp))
            MensajeError(e)
        }
        Spacer(Modifier.height(20.dp))

        BotonPrincipal(texto = "Ingresar", cargando = vm.cargando, onClick = vm::ingresar)
    }
}

@Composable
fun RegistroScreen(onVolver: () -> Unit, onAjustes: () -> Unit, vm: RegistroViewModel = viewModel()) {
    BackHandler(onBack = onVolver)

    FondoAuth(textoPie = "¿Ya tienes cuenta?", accionPie = "Inicia sesión", onPie = onVolver, onAjustes = onAjustes) {
        Marca()
        Text("Crear cuenta", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Texto)
        Spacer(Modifier.height(4.dp))
        Text("Únete a la comunidad de La Abundancia", fontSize = 13.sp, color = TextoSuave)
        Spacer(Modifier.height(24.dp))

        CampoTexto("Nombre", vm.nombre, vm::cambiarNombre, placeholder = "Tu nombre", error = vm.errorNombre)
        Spacer(Modifier.height(14.dp))
        CampoTexto(
            "Correo electrónico", vm.email, vm::cambiarEmail,
            placeholder = "tu@correo.com", teclado = KeyboardType.Email, error = vm.errorEmail,
        )
        Spacer(Modifier.height(14.dp))
        CampoTexto(
            "Contraseña", vm.password, vm::cambiarPassword,
            placeholder = "Mínimo 8 caracteres", esPassword = true, error = vm.errorPassword,
        )
        Spacer(Modifier.height(14.dp))
        CampoTexto(
            "Confirmar contraseña", vm.confirmar, vm::cambiarConfirmar,
            placeholder = "••••••••", esPassword = true, error = vm.errorConfirmar,
        )

        val e = vm.error
        if (e != null) {
            Spacer(Modifier.height(14.dp))
            MensajeError(e)
        }
        Spacer(Modifier.height(20.dp))

        BotonPrincipal(texto = "Crear cuenta", cargando = vm.cargando, onClick = vm::registrar)
    }
}

/** Logo como en la tarjeta de login del panel. */
@Composable
private fun Marca() {
    Image(
        painter = painterResource(R.drawable.logo_abundancia),
        contentDescription = "La Abundancia",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
    )
    Spacer(Modifier.height(18.dp))
}

/**
 * Fondo morado degradado con círculos animados, igual a la página de login del panel,
 * con la tarjeta blanca centrada.
 */
@Composable
private fun FondoAuth(
    textoPie: String,
    accionPie: String,
    onPie: () -> Unit,
    onAjustes: () -> Unit,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    val servidor by Session.servidor.collectAsState()

    val transicion = rememberInfiniteTransition(label = "fondo")
    val mov by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse),
        label = "mov",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(GradienteLogin)
    ) {
        // blob arriba izquierda
        Box(
            Modifier
                .offset(x = (-120 + 40 * mov).dp, y = (-120 + 30 * mov).dp)
                .size(360.dp)
                .background(
                    Brush.radialGradient(listOf(Color(0x59A78BFA), Color.Transparent)),
                    CircleShape,
                )
        )
        // blob abajo derecha
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (100 - 50 * mov).dp, y = (140 - 40 * mov).dp)
                .size(400.dp)
                .background(
                    Brush.radialGradient(listOf(Color(0x666D28D9), Color.Transparent)),
                    CircleShape,
                )
        )
        // anillos decorativos
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-30).dp, y = (110 - 18 * mov).dp)
                .size(120.dp)
                .border(1.5.dp, Color.White.copy(alpha = 0.12f), CircleShape)
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset(x = 30.dp, y = (-130 + 18 * mov).dp)
                .size(80.dp)
                .border(1.5.dp, Color.White.copy(alpha = 0.1f), CircleShape)
        )

        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
        ) {
            val alto = maxHeight
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = alto)
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val forma = RoundedCornerShape(20.dp)
                Column(
                    Modifier
                        .widthIn(max = 420.dp)
                        .fillMaxWidth()
                        .shadow(24.dp, forma)
                        .clip(forma)
                        .background(Blanco.copy(alpha = 0.97f))
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    content = contenido,
                )

                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(textoPie, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    TextButton(onClick = onPie) {
                        Text(accionPie, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
                TextButton(onClick = onAjustes) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Servidor: $servidor · Ajustes",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }

}
