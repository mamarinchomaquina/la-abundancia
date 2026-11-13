package com.abundancia.app.ui.publicaciones

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.abundancia.app.data.urlImagen
import com.abundancia.app.ui.theme.Fondo
import java.io.File
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abundancia.app.data.AppContainer
import com.abundancia.app.data.Session
import com.abundancia.app.data.CATEGORIAS
import com.abundancia.app.data.Usuario
import com.abundancia.app.data.local.BorradorPublicacionEntity
import com.abundancia.app.data.local.EstadoSync
import com.abundancia.app.data.repo.PublicacionItem
import com.abundancia.app.ui.components.AvisoCopiaLocal
import com.abundancia.app.ui.components.BotonPrincipal
import com.abundancia.app.ui.components.BotonSecundario
import com.abundancia.app.ui.components.CampoTexto
import com.abundancia.app.ui.components.Cargando
import com.abundancia.app.ui.components.ChipCategoria
import com.abundancia.app.ui.components.EncabezadoMorado
import com.abundancia.app.ui.components.EstadoBadge
import com.abundancia.app.ui.components.EstadoError
import com.abundancia.app.ui.components.EstadoSyncBadge
import com.abundancia.app.ui.components.EstadoVacio
import com.abundancia.app.ui.components.FilaInfo
import com.abundancia.app.ui.components.FiltroChip
import com.abundancia.app.ui.components.MensajeError
import com.abundancia.app.ui.components.MensajeExito
import com.abundancia.app.ui.components.Tarjeta
import com.abundancia.app.ui.components.formatearFecha
import com.abundancia.app.ui.components.formatearMomento
import com.abundancia.app.ui.theme.Amarillo
import com.abundancia.app.ui.theme.Blanco
import com.abundancia.app.ui.theme.Borde
import com.abundancia.app.ui.theme.Morado
import com.abundancia.app.ui.theme.Rojo
import com.abundancia.app.ui.theme.RojoSuave
import com.abundancia.app.ui.theme.Texto
import com.abundancia.app.ui.theme.TextoSuave

private fun unidades(n: Int) = if (n == 1) "1 unidad" else "$n unidades"

// ─────────────────────────────────────────────────────────────────────────────
// Lista de publicaciones (copia local en Room + actualización desde el servidor)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PublicacionesScreen(
    usuario: Usuario,
    onAbrir: (String) -> Unit,
    vm: PublicacionesViewModel = viewModel(),
) {
    val lista by vm.publicaciones.collectAsState()
    val ultima by vm.ultimaActualizacion.collectAsState()
    val enLinea by AppContainer.conectividad.enLinea.collectAsState()
    val filtradas = remember(lista, vm.filtro, vm.busqueda) { vm.filtrar(lista) }

    // Al recuperar la red, se actualiza solo
    val servidor by Session.servidor.collectAsState()
    // Al recuperar la red o cuando la app encuentra el servidor en otra IP, se actualiza sola
    LaunchedEffect(enLinea, servidor) { if (enLinea) vm.refrescar() }

    Column(Modifier.fillMaxSize()) {
        EncabezadoMorado("Publicaciones", "Donaciones de la comunidad") {
            if (vm.cargando) {
                CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
            } else {
                IconButton(onClick = vm::refrescar) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Actualizar", tint = Color.White)
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                CampoTexto(
                    etiqueta = null,
                    valor = vm.busqueda,
                    onCambio = vm::cambiarBusqueda,
                    placeholder = "Buscar por título, descripción o ubicación",
                    icono = Icons.Filled.Search,
                )
            }
            item {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PublicacionesViewModel.FILTROS.forEach { f ->
                        FiltroChip(texto = f, activo = vm.filtro == f, onClick = { vm.cambiarFiltro(f) })
                    }
                }
            }

            val errorServidor = vm.errorServidor
            if (errorServidor != null && !vm.cargando) {
                item { AvisoCopiaLocal(errorServidor, ultima) }
            }

            when {
                vm.cargando && lista.isEmpty() -> item { Cargando() }
                lista.isEmpty() && errorServidor != null -> item { EstadoError("No hay datos guardados todavía.", vm::refrescar) }
                filtradas.isEmpty() -> item { EstadoVacio("No hay publicaciones para mostrar.") }
                else -> {
                    item {
                        val cuando = formatearMomento(ultima)
                        Text(
                            "${filtradas.size} ${if (filtradas.size == 1) "publicación" else "publicaciones"}" +
                                if (cuando != null) " · actualizado $cuando" else "",
                            fontSize = 12.sp,
                            color = TextoSuave,
                        )
                    }
                    items(filtradas, key = { it.id }) { p ->
                        TarjetaPublicacion(p, esMia = p.autorId == usuario.id) { onAbrir(p.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaPublicacion(p: PublicacionItem, esMia: Boolean, onClick: () -> Unit) {
    Tarjeta(onClick = onClick) {
        val foto = urlImagen(p.imagen)
        if (foto != null) {
            FotoPublicacion(modelo = foto, alto = 150)
            Spacer(Modifier.height(12.dp))
        }
        Row(verticalAlignment = Alignment.Top) {
            Text(
                p.titulo,
                modifier = Modifier.weight(1f),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Texto,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(8.dp))
            EstadoBadge(p.estado)
        }
        Spacer(Modifier.height(6.dp))
        Text(p.descripcion, fontSize = 13.sp, color = TextoSuave, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChipCategoria(p.categoria)
            Text(unidades(p.cantidad), fontSize = 12.sp, color = TextoSuave)
        }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = Borde)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = TextoSuave, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                p.ubicacion,
                modifier = Modifier.weight(1f),
                fontSize = 12.sp,
                color = TextoSuave,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (esMia) "Tú" else p.autorNombre.ifBlank { "—" },
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Texto,
            )
            val fecha = formatearFecha(p.createdAt)
            if (fecha.isNotEmpty()) Text("  ·  $fecha", fontSize = 12.sp, color = TextoSuave)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Detalle: solicitar (funciona sin internet) o eliminar (necesita conexión)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DetallePublicacionScreen(id: String, usuario: Usuario, onCerrar: () -> Unit) {
    val vm: DetalleViewModel = viewModel(key = "detalle-$id")
    val miSolicitud by vm.miSolicitud.collectAsState()
    val enLinea by AppContainer.conectividad.enLinea.collectAsState()
    var confirmarEliminar by remember { mutableStateOf(false) }

    LaunchedEffect(id) { vm.cargar(id) }

    Column(Modifier.fillMaxSize()) {
        EncabezadoMorado("Detalle", "Publicación", onAtras = onCerrar)

        val p = vm.publicacion
        val e = vm.error
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                vm.cargando && p == null -> Cargando()
                p == null -> EstadoError(e ?: "No se encontró la publicación.", vm::recargar)
                else -> {
                    val esMia = p.autorId == usuario.id

                    if (vm.desdeCopiaLocal) {
                        Text(
                            "Sin conexión con el servidor: es la copia guardada en el teléfono, el estado puede haber cambiado.",
                            fontSize = 12.sp,
                            color = Amarillo,
                        )
                    }

                    Tarjeta {
                        val foto = urlImagen(p.imagen)
                        if (foto != null) {
                            FotoPublicacion(modelo = foto, alto = 220)
                            Spacer(Modifier.height(14.dp))
                        }
                        Row(verticalAlignment = Alignment.Top) {
                            Text(p.titulo, modifier = Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Texto)
                            Spacer(Modifier.width(8.dp))
                            EstadoBadge(p.estado)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(p.descripcion, fontSize = 14.sp, color = Texto, lineHeight = 21.sp)
                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = Borde)
                        Spacer(Modifier.height(6.dp))
                        FilaInfo("Categoría", p.categoria)
                        FilaInfo("Cantidad", unidades(p.cantidad))
                        FilaInfo("Ubicación", p.ubicacion)
                        FilaInfo("Publicado por", if (esMia) "Tú" else p.autorNombre.ifBlank { "—" })
                        val fecha = formatearFecha(p.createdAt)
                        if (fecha.isNotEmpty()) FilaInfo("Fecha", fecha)
                    }

                    when {
                        esMia && p.estado == "pendiente" -> Tarjeta {
                            Text("Pendiente de aprobación", fontWeight = FontWeight.SemiBold, color = Texto)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Un administrador la revisará desde el panel web. Mientras tanto puedes eliminarla.",
                                fontSize = 13.sp,
                                color = TextoSuave,
                            )
                            Spacer(Modifier.height(14.dp))
                            BotonSecundario(
                                texto = if (enLinea) "Eliminar publicación" else "Eliminar (requiere conexión)",
                                onClick = { confirmarEliminar = true },
                                color = Rojo,
                                fondo = RojoSuave,
                                cargando = vm.eliminando,
                                enabled = enLinea,
                            )
                        }

                        esMia -> Tarjeta {
                            Text(
                                "Esta es tu publicación. Revisa quién la ha solicitado en la pestaña Solicitudes.",
                                fontSize = 13.sp,
                                color = TextoSuave,
                            )
                        }

                        p.estado == "aprobada" -> Tarjeta {
                            Text("Solicitar esta donación", fontWeight = FontWeight.SemiBold, color = Texto)
                            Spacer(Modifier.height(12.dp))
                            val s = miSolicitud
                            if (s != null && s.estadoSync != EstadoSync.ERROR) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Tu solicitud: ", fontSize = 13.sp, color = TextoSuave)
                                    EstadoSyncBadge(s.estadoSync)
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    when (s.estadoSync) {
                                        EstadoSync.ENVIADO -> "El donante ya puede verla en sus solicitudes."
                                        else -> "Está guardada en el teléfono y se enviará apenas haya conexión con el servidor."
                                    },
                                    fontSize = 13.sp,
                                    color = TextoSuave,
                                )
                            } else {
                                if (s != null) {
                                    MensajeError("No se pudo enviar tu solicitud: ${s.error ?: "error desconocido"}. Puedes intentarlo de nuevo.")
                                    Spacer(Modifier.height(12.dp))
                                }
                                CampoTexto(
                                    etiqueta = "Mensaje para el donante (opcional)",
                                    valor = vm.mensaje,
                                    onCambio = vm::cambiarMensaje,
                                    placeholder = "Cuéntale por qué la necesitas",
                                    lineas = 3,
                                )
                                val aviso = vm.avisoSolicitud
                                if (aviso != null) {
                                    Spacer(Modifier.height(12.dp))
                                    MensajeError(aviso)
                                }
                                Spacer(Modifier.height(14.dp))
                                BotonPrincipal(texto = "Enviar solicitud", cargando = vm.enviando, onClick = vm::solicitar)
                                if (!enLinea) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Sin conexión: se guardará en el teléfono y se enviará sola al reconectar.",
                                        fontSize = 12.sp,
                                        color = TextoSuave,
                                    )
                                }
                            }
                        }

                        else -> EstadoVacio("Esta donación ya fue entregada.")
                    }

                    if (e != null && !vm.desdeCopiaLocal) MensajeError(e)
                }
            }
        }
    }

    if (confirmarEliminar) {
        AlertDialog(
            onDismissRequest = { confirmarEliminar = false },
            containerColor = Blanco,
            title = { Text("Eliminar publicación", fontWeight = FontWeight.SemiBold) },
            text = { Text("Esta acción no se puede deshacer.", color = TextoSuave) },
            confirmButton = {
                TextButton(onClick = {
                    confirmarEliminar = false
                    vm.eliminar(onListo = onCerrar)
                }) { Text("Eliminar", color = Rojo, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarEliminar = false }) { Text("Cancelar", color = TextoSuave) }
            },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Publicar: el formulario se guarda solo como borrador (Room) y se envía
// en cuanto hay conexión.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun NuevaPublicacionScreen(vm: NuevaPublicacionViewModel = viewModel()) {
    val envios by vm.envios.collectAsState()
    var descartarId by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        EncabezadoMorado("Publicar", "Comparte una donación")

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!vm.listo) Cargando()
            if (vm.listo) Tarjeta {
                CampoTexto("Título", vm.titulo, vm::cambiarTitulo, placeholder = "Ej: Mercado para una familia", error = vm.errorTitulo)
                Spacer(Modifier.height(14.dp))
                CampoTexto(
                    "Descripción", vm.descripcion, vm::cambiarDescripcion,
                    placeholder = "Describe lo que vas a donar", lineas = 4, error = vm.errorDescripcion,
                )
                Spacer(Modifier.height(14.dp))
                Text("Categoría", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Texto)
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CATEGORIAS.forEach { c ->
                        FiltroChip(texto = c, activo = vm.categoria == c, onClick = { vm.cambiarCategoria(c) })
                    }
                }
                val errorCategoria = vm.errorCategoria
                if (errorCategoria != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(errorCategoria, color = Rojo, fontSize = 12.sp)
                }
                Spacer(Modifier.height(14.dp))
                CampoTexto(
                    "Cantidad", vm.cantidad, vm::cambiarCantidad,
                    placeholder = "1", teclado = KeyboardType.Number, error = vm.errorCantidad,
                )
                Spacer(Modifier.height(14.dp))
                CampoTexto(
                    "Ubicación", vm.ubicacion, vm::cambiarUbicacion,
                    placeholder = "Barrio o dirección de entrega", error = vm.errorUbicacion,
                )
                Spacer(Modifier.height(14.dp))
                SelectorFoto(vm)
            }

            Text(
                "Lo que escribes se guarda como borrador en el teléfono, aunque cierres la app. " +
                    "Al publicar, queda pendiente hasta que un administrador la apruebe desde el panel web.",
                fontSize = 12.sp,
                color = TextoSuave,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            val aviso = vm.aviso
            if (aviso != null) MensajeExito(aviso)

            BotonPrincipal(texto = "Publicar donación", cargando = vm.enviando, onClick = vm::publicar)

            if (envios.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Mis envíos desde este teléfono",
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.SemiBold,
                        color = Texto,
                    )
                    if (envios.any { it.estadoSync == EstadoSync.ENVIADO }) {
                        TextButton(onClick = vm::limpiarEnviados) {
                            Text("Limpiar enviados", color = Morado, fontSize = 12.sp)
                        }
                    }
                }
                envios.forEach { b ->
                    TarjetaEnvio(
                        b = b,
                        onReintentar = { vm.reintentar(b.localId) },
                        onDescartar = { descartarId = b.localId },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    val aDescartar = descartarId
    if (aDescartar != null) {
        AlertDialog(
            onDismissRequest = { descartarId = null },
            containerColor = Blanco,
            title = { Text("Descartar publicación", fontWeight = FontWeight.SemiBold) },
            text = { Text("Se borrará del teléfono y no se enviará al servidor.", color = TextoSuave) },
            confirmButton = {
                TextButton(onClick = {
                    vm.descartar(aDescartar)
                    descartarId = null
                }) { Text("Descartar", color = Rojo, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { descartarId = null }) { Text("Cancelar", color = TextoSuave) }
            },
        )
    }
}

@Composable
private fun TarjetaEnvio(b: BorradorPublicacionEntity, onReintentar: () -> Unit, onDescartar: () -> Unit) {
    Tarjeta {
        val local = b.imagenLocal?.let { File(it) }?.takeIf { it.exists() }
        if (local != null) {
            FotoPublicacion(modelo = local, alto = 110)
            Spacer(Modifier.height(10.dp))
        }
        Row(verticalAlignment = Alignment.Top) {
            Text(
                b.titulo.ifBlank { "(sin título)" },
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Texto,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(8.dp))
            EstadoSyncBadge(b.estadoSync)
        }
        Spacer(Modifier.height(6.dp))
        Text("${b.categoria} · ${b.cantidad} · ${b.ubicacion}", fontSize = 12.sp, color = TextoSuave, maxLines = 1)
        val detalle = when (b.estadoSync) {
            EstadoSync.ENVIADO -> "Recibida por el servidor. Espera la aprobación del administrador."
            EstadoSync.ERROR -> b.error ?: "El servidor la rechazó."
            else -> b.error ?: "Se enviará automáticamente cuando haya conexión."
        }
        Spacer(Modifier.height(6.dp))
        Text(detalle, fontSize = 12.sp, color = if (b.estadoSync == EstadoSync.ERROR) Rojo else TextoSuave)
        if (b.estadoSync == EstadoSync.ERROR || b.estadoSync == EstadoSync.PENDIENTE) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BotonSecundario("Reintentar", onReintentar, modifier = Modifier.weight(1f))
                BotonSecundario("Descartar", onDescartar, modifier = Modifier.weight(1f), color = Rojo, fondo = RojoSuave)
            }
        }
    }
}

/** Foto con esquinas redondeadas; acepta una URL del servidor o un archivo del celular. */
@Composable
private fun FotoPublicacion(modelo: Any, alto: Int) {
    AsyncImage(
        model = modelo,
        contentDescription = "Foto de la donación",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .height(alto.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Fondo),
    )
}

/** Elegir foto de la galería (Photo Picker). Se guarda en el celular y se sube con la publicación. */
@Composable
private fun SelectorFoto(vm: NuevaPublicacionViewModel) {
    val selector = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.elegirFoto(uri)
    }
    val abrir = { selector.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    Text("Foto (opcional)", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Texto)
    Spacer(Modifier.height(6.dp))
    val ruta = vm.imagenLocal
    if (ruta != null) {
        FotoPublicacion(modelo = File(ruta), alto = 170)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BotonSecundario("Cambiar foto", { abrir() }, modifier = Modifier.weight(1f), cargando = vm.procesandoFoto)
            BotonSecundario("Quitar", vm::quitarFoto, modifier = Modifier.weight(1f), color = Rojo, fondo = RojoSuave)
        }
    } else {
        Column(
            Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Fondo)
                .border(1.dp, Borde, RoundedCornerShape(10.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (vm.procesandoFoto) {
                CircularProgressIndicator(Modifier.size(24.dp), color = Morado, strokeWidth = 2.dp)
            } else {
                Text("📷", fontSize = 26.sp)
                TextButton(onClick = { abrir() }) {
                    Text("Agregar foto", color = Morado, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
    val error = vm.errorFoto
    if (error != null) {
        Spacer(Modifier.height(4.dp))
        Text(error, color = Rojo, fontSize = 12.sp)
    } else {
        Spacer(Modifier.height(4.dp))
        Text("Se reduce automáticamente (JPG, máx. 2 MB). Si no hay internet, se sube después.", color = TextoSuave, fontSize = 12.sp)
    }
}
