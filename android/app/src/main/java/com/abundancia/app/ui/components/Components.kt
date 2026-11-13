package com.abundancia.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abundancia.app.R
import com.abundancia.app.data.Session
import com.abundancia.app.data.local.EstadoSync
import com.abundancia.app.data.normalizarServidor
import com.abundancia.app.data.probarServidor
import com.abundancia.app.ui.theme.Amarillo
import com.abundancia.app.ui.theme.AmarilloSuave
import com.abundancia.app.ui.theme.Blanco
import com.abundancia.app.ui.theme.Borde
import com.abundancia.app.ui.theme.Fondo
import com.abundancia.app.ui.theme.GradienteBoton
import com.abundancia.app.ui.theme.GradienteSidebar
import com.abundancia.app.ui.theme.Inter
import com.abundancia.app.ui.theme.Morado
import com.abundancia.app.ui.theme.MoradoSuave
import com.abundancia.app.ui.theme.Placeholder
import com.abundancia.app.ui.theme.Rojo
import com.abundancia.app.ui.theme.RojoSuave
import com.abundancia.app.ui.theme.Texto
import com.abundancia.app.ui.theme.TextoSuave
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Encabezado con el degradado morado del sidebar del panel web. */
@Composable
fun EncabezadoMorado(
    titulo: String,
    subtitulo: String? = null,
    onAtras: (() -> Unit)? = null,
    acciones: @Composable RowScope.() -> Unit = {},
) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(GradienteSidebar)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onAtras != null) {
                IconButton(onClick = onAtras) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }
            } else {
                Box(
                    Modifier
                        .padding(start = 4.dp)
                        .size(width = 58.dp, height = 40.dp)
                        .shadow(4.dp, RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .padding(3.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.logo_abundancia),
                        contentDescription = "La Abundancia",
                        contentScale = ContentScale.Fit,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(titulo, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (subtitulo != null) {
                    Text(subtitulo, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }
            acciones()
        }
    }
}

/** Tarjeta blanca con borde, igual a las tarjetas del panel. */
@Composable
fun Tarjeta(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val forma = RoundedCornerShape(14.dp)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(1.dp, forma)
            .clip(forma)
            .background(Blanco)
            .border(1.dp, Borde, forma)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        content = content,
    )
}

/** Insignia de estado (.estado-badge del panel). */
@Composable
fun EstadoBadge(estado: String) {
    val (fondo, color) = when (estado) {
        "pendiente" -> AmarilloSuave to Amarillo
        "aprobada", "aceptada" -> MoradoSuave to Morado
        "rechazada" -> RojoSuave to Rojo
        else -> Fondo to TextoSuave
    }
    val forma = RoundedCornerShape(20.dp)
    Text(
        text = estado.replaceFirstChar { it.uppercase() },
        color = color,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(forma)
            .background(fondo)
            .then(if (estado == "entregada") Modifier.border(1.dp, Borde, forma) else Modifier)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** Chip de categoría (.chip-cat del panel). */
@Composable
fun ChipCategoria(categoria: String) {
    val forma = RoundedCornerShape(20.dp)
    Text(
        text = categoria,
        color = TextoSuave,
        fontSize = 11.5.sp,
        modifier = Modifier
            .clip(forma)
            .background(Fondo)
            .border(1.dp, Borde, forma)
            .padding(horizontal = 9.dp, vertical = 3.dp),
    )
}

/** Botón de filtro (.filtro-btn del panel): morado cuando está activo. */
@Composable
fun FiltroChip(texto: String, activo: Boolean, onClick: () -> Unit) {
    val forma = RoundedCornerShape(8.dp)
    Text(
        text = texto,
        color = if (activo) Color.White else TextoSuave,
        fontSize = 13.sp,
        fontWeight = if (activo) FontWeight.Medium else FontWeight.Normal,
        modifier = Modifier
            .clip(forma)
            .background(if (activo) Morado else Blanco)
            .border(1.dp, if (activo) Morado else Borde, forma)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

/** Botón principal con el degradado morado del panel. */
@Composable
fun BotonPrincipal(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cargando: Boolean = false,
    enabled: Boolean = true,
) {
    val forma = RoundedCornerShape(9.dp)
    val activo = enabled && !cargando
    Box(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .alpha(if (activo) 1f else 0.6f)
            .shadow(if (activo) 4.dp else 0.dp, forma)
            .clip(forma)
            .background(GradienteBoton)
            .clickable(enabled = activo, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (cargando) {
            CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Text(texto, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Botón secundario con borde (.btn-aprobar / .btn-eliminar del panel). */
@Composable
fun BotonSecundario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Morado,
    fondo: Color = MoradoSuave,
    cargando: Boolean = false,
    enabled: Boolean = true,
) {
    val forma = RoundedCornerShape(9.dp)
    val activo = enabled && !cargando
    Box(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .alpha(if (activo) 1f else 0.6f)
            .clip(forma)
            .background(fondo)
            .border(1.5.dp, color.copy(alpha = 0.3f), forma)
            .clickable(enabled = activo, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (cargando) {
            CircularProgressIndicator(Modifier.size(18.dp), color = color, strokeWidth = 2.dp)
        } else {
            Text(texto, color = color, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** Campo de texto con etiqueta arriba, como los inputs del panel. */
@Composable
fun CampoTexto(
    etiqueta: String?,
    valor: String,
    onCambio: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    esPassword: Boolean = false,
    teclado: KeyboardType = KeyboardType.Text,
    lineas: Int = 1,
    icono: ImageVector? = null,
    error: String? = null,
) {
    val iconoInicial: (@Composable () -> Unit)? = icono?.let { img ->
        @Composable { Icon(img, contentDescription = null, tint = TextoSuave) }
    }
    Column(modifier.fillMaxWidth()) {
        if (etiqueta != null) {
            Text(etiqueta, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Texto)
            Spacer(Modifier.height(6.dp))
        }
        OutlinedTextField(
            value = valor,
            onValueChange = onCambio,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = Placeholder, fontSize = 14.sp) },
            leadingIcon = iconoInicial,
            singleLine = lineas == 1,
            minLines = lineas,
            maxLines = if (lineas == 1) 1 else lineas + 3,
            visualTransformation = if (esPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (esPassword) KeyboardType.Password else teclado),
            shape = RoundedCornerShape(9.dp),
            textStyle = TextStyle(fontFamily = Inter, fontSize = 14.sp, color = Texto),
            isError = error != null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Morado,
                unfocusedBorderColor = Borde,
                focusedContainerColor = Blanco,
                unfocusedContainerColor = Blanco,
                errorContainerColor = Blanco,
                errorBorderColor = Rojo,
                cursorColor = Morado,
            ),
        )
        if (error != null) {
            Spacer(Modifier.height(4.dp))
            Text(error, color = Rojo, fontSize = 12.sp)
        }
    }
}

@Composable
fun MensajeError(texto: String) {
    Text(
        texto,
        color = Rojo,
        fontSize = 13.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(RojoSuave)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

@Composable
fun MensajeExito(texto: String) {
    Text(
        texto,
        color = Morado,
        fontSize = 13.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(MoradoSuave)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

@Composable
fun Cargando() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = Morado)
    }
}

/** .estado-vacio del panel */
@Composable
fun EstadoVacio(texto: String) {
    Tarjeta {
        Text(
            texto,
            color = TextoSuave,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
        )
    }
}

/** .estado-error del panel, con botón de reintentar */
@Composable
fun EstadoError(texto: String, onReintentar: () -> Unit) {
    Tarjeta {
        Text(
            texto,
            color = Rojo,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        )
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onReintentar, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Reintentar", color = Morado, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Avatar con la inicial del nombre (.avatar del panel). */
@Composable
fun Avatar(nombre: String, tamano: Int = 40) {
    Box(
        Modifier
            .size(tamano.dp)
            .clip(CircleShape)
            .background(GradienteBoton),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            nombre.trim().firstOrNull()?.uppercase() ?: "?",
            color = Color.White,
            fontSize = (tamano * 0.4f).sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun FilaInfo(etiqueta: String, valor: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(etiqueta, color = TextoSuave, fontSize = 13.sp)
        Spacer(Modifier.width(12.dp))
        Text(valor, color = Texto, fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End)
    }
}

/** Diálogo para escribir y probar la IP del PC donde corre el backend. */
@Composable
fun ServidorDialog(onCerrar: () -> Unit) {
    val actual by Session.servidor.collectAsState()
    var valor by remember { mutableStateOf(actual) }
    var probando by remember { mutableStateOf(false) }
    var resultado by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = Blanco,
        shape = RoundedCornerShape(16.dp),
        title = { Text("Servidor", fontWeight = FontWeight.SemiBold, color = Texto) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Escribe la IP de tu PC en la red Wi-Fi (en Windows: ipconfig → Dirección IPv4). " +
                        "El celular y el PC deben estar en el mismo Wi-Fi y el backend encendido.",
                    fontSize = 13.sp,
                    color = TextoSuave,
                )
                CampoTexto(
                    etiqueta = "IP del PC",
                    valor = valor,
                    onCambio = { valor = it; resultado = null },
                    placeholder = "192.168.1.100",
                    teclado = KeyboardType.Uri,
                )
                val r = resultado
                if (r != null) {
                    if (r.first) MensajeExito(r.second) else MensajeError(r.second)
                }
                TextButton(
                    enabled = !probando,
                    onClick = {
                        probando = true
                        resultado = null
                        scope.launch {
                            val prueba = probarServidor(valor)
                            resultado = if (prueba.isSuccess) {
                                true to "Conexión exitosa con ${normalizarServidor(valor)}"
                            } else {
                                false to (prueba.exceptionOrNull()?.message ?: "No se pudo conectar.")
                            }
                            probando = false
                        }
                    },
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(if (probando) "Probando…" else "Probar conexión", color = Morado, fontWeight = FontWeight.Medium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                Session.cambiarServidor(valor)
                onCerrar()
            }) { Text("Guardar", color = Morado, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text("Cancelar", color = TextoSuave) }
        },
    )
}

fun formatearFecha(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try {
        DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .withZone(ZoneId.systemDefault())
            .format(Instant.parse(iso))
    } catch (e: Exception) {
        ""
    }
}

/** Insignia del estado de envío de algo creado en el celular. */
@Composable
fun EstadoSyncBadge(estadoSync: String) {
    val (texto, fondo, color) = when (estadoSync) {
        EstadoSync.PENDIENTE -> Triple("Pendiente de envío", AmarilloSuave, Amarillo)
        EstadoSync.ENVIANDO -> Triple("Enviando…", MoradoSuave, Morado)
        EstadoSync.ENVIADO -> Triple("Enviado ✓", MoradoSuave, Morado)
        EstadoSync.ERROR -> Triple("Error al enviar", RojoSuave, Rojo)
        else -> Triple("Borrador", Fondo, TextoSuave)
    }
    Text(
        text = texto,
        color = color,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(fondo)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** Franja que avisa que no hay red o que hay cambios esperando para enviarse. */
@Composable
fun BannerConexion(enLinea: Boolean, porEnviar: Int) {
    if (enLinea && porEnviar == 0) return
    val texto = when {
        !enLinea && porEnviar > 0 ->
            "Sin conexión · $porEnviar ${if (porEnviar == 1) "cambio guardado" else "cambios guardados"} en el teléfono. Se enviarán al reconectar."
        !enLinea -> "Sin conexión · Mostrando la copia guardada en el teléfono."
        else -> "Enviando $porEnviar ${if (porEnviar == 1) "cambio pendiente" else "cambios pendientes"}…"
    }
    Text(
        texto,
        color = if (enLinea) Morado else Amarillo,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (enLinea) MoradoSuave else AmarilloSuave)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

/** Aviso cuando los datos vienen de la copia local y no del servidor. */
@Composable
fun AvisoCopiaLocal(error: String?, ultimaActualizacion: Long?) {
    val cuando = formatearMomento(ultimaActualizacion)
    val texto = buildString {
        append("No se pudo actualizar desde el servidor. ")
        append(if (cuando != null) "Mostrando la copia guardada el $cuando." else "Aún no hay copia guardada.")
        if (!error.isNullOrBlank()) append("\n").append(error)
    }
    Text(
        texto,
        color = Amarillo,
        fontSize = 12.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(AmarilloSuave)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

fun formatearMomento(millis: Long?): String? {
    if (millis == null) return null
    return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(millis))
}
