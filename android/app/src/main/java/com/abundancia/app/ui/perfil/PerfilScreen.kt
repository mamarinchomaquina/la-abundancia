package com.abundancia.app.ui.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abundancia.app.data.AppContainer
import com.abundancia.app.data.Session
import com.abundancia.app.ui.components.FiltroChip
import com.abundancia.app.ui.publicaciones.PublicacionesViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.abundancia.app.data.Usuario
import com.abundancia.app.ui.components.Avatar
import com.abundancia.app.ui.components.BotonSecundario
import com.abundancia.app.ui.components.EncabezadoMorado
import com.abundancia.app.ui.components.ServidorDialog
import com.abundancia.app.ui.components.Tarjeta
import com.abundancia.app.ui.theme.Morado
import com.abundancia.app.ui.theme.MoradoSuave
import com.abundancia.app.ui.theme.Rojo
import com.abundancia.app.ui.theme.RojoSuave
import com.abundancia.app.ui.theme.Texto
import com.abundancia.app.ui.theme.TextoSuave

class PerfilViewModel : ViewModel() {
    var cerrando by mutableStateOf(false)
        private set

    fun cerrarSesion() {
        if (cerrando) return
        viewModelScope.launch {
            cerrando = true
            AppContainer.auth.cerrarSesion()
            cerrando = false
        }
    }
}

@Composable
fun PerfilScreen(usuario: Usuario, vm: PerfilViewModel = viewModel()) {

    Column(Modifier.fillMaxSize()) {
        EncabezadoMorado("Mi perfil", "Tu cuenta en La Abundancia")

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Tarjeta {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Avatar(usuario.nombre, tamano = 72)
                    Spacer(Modifier.height(12.dp))
                    Text(usuario.nombre, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Texto)
                    Text(usuario.email, fontSize = 13.sp, color = TextoSuave)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (usuario.esAdmin) "Administrador" else "Usuario",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Morado,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MoradoSuave)
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
            }

            Text(
                "La IP del servidor y la categoría preferida están en la pestaña Ajustes.",
                fontSize = 12.sp,
                color = TextoSuave,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            BotonSecundario(
                texto = "Cerrar sesión",
                onClick = vm::cerrarSesion,
                cargando = vm.cerrando,
                color = Rojo,
                fondo = RojoSuave,
            )
        }
    }
}
