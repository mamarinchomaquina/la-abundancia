package com.abundancia.app.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abundancia.app.R
import com.abundancia.app.ui.components.BotonPrincipal
import com.abundancia.app.ui.theme.Blanco
import com.abundancia.app.ui.theme.Borde
import com.abundancia.app.ui.theme.GradienteLogin
import com.abundancia.app.ui.theme.Morado
import com.abundancia.app.ui.theme.MoradoSuave
import com.abundancia.app.ui.theme.Texto
import com.abundancia.app.ui.theme.TextoSuave

private data class PaginaOnboarding(val emoji: String, val titulo: String, val texto: String)

private val PAGINAS = listOf(
    PaginaOnboarding(
        "🤝",
        "Bienvenido a La Abundancia",
        "Comparte lo que te sobra y ayuda a quien lo necesita: alimentos, ropa, útiles y más.",
    ),
    PaginaOnboarding(
        "📦",
        "Publica y solicita",
        "Publica una donación y el administrador la aprueba desde el panel web. " +
            "También puedes solicitar las donaciones de otros.",
    ),
    PaginaOnboarding(
        "📶",
        "Funciona sin internet",
        "Si te quedas sin conexión, lo que publiques o solicites se guarda en el teléfono " +
            "y se envía solo apenas vuelva la red.",
    ),
)

/** Se muestra solo la primera vez. Al terminar se guarda en DataStore (onboarding_completado). */
@Composable
fun OnboardingScreen(onTerminar: () -> Unit) {
    var pagina by rememberSaveable { mutableIntStateOf(0) }
    val ultima = pagina == PAGINAS.lastIndex

    Box(
        Modifier
            .fillMaxSize()
            .background(GradienteLogin)
            .systemBarsPadding()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (!ultima) {
                    TextButton(onClick = onTerminar) {
                        Text("Omitir", color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            val forma = RoundedCornerShape(20.dp)
            Column(
                Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .shadow(24.dp, forma)
                    .clip(forma)
                    .background(Blanco)
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_abundancia),
                    contentDescription = "La Abundancia",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                )
                Spacer(Modifier.height(20.dp))

                AnimatedContent(targetState = pagina, label = "pagina") { i ->
                    val p = PAGINAS[i]
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MoradoSuave),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(p.emoji, fontSize = 32.sp)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            p.titulo,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Texto,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            p.texto,
                            fontSize = 14.sp,
                            color = TextoSuave,
                            textAlign = TextAlign.Center,
                            lineHeight = 21.sp,
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PAGINAS.indices.forEach { i ->
                        Box(
                            Modifier
                                .height(8.dp)
                                .width(if (i == pagina) 22.dp else 8.dp)
                                .clip(CircleShape)
                                .background(if (i == pagina) Morado else Borde)
                        )
                    }
                }
                Spacer(Modifier.height(22.dp))

                BotonPrincipal(
                    texto = if (ultima) "Comenzar" else "Siguiente",
                    onClick = { if (ultima) onTerminar() else pagina++ },
                )
                if (pagina > 0) {
                    TextButton(onClick = { pagina-- }) {
                        Text("Atrás", color = TextoSuave)
                    }
                }
            }
        }
    }
}
