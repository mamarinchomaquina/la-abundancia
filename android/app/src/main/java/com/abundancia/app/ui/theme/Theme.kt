package com.abundancia.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val EsquemaAbundancia = lightColorScheme(
    primary = Morado,
    onPrimary = Blanco,
    primaryContainer = MoradoSuave,
    onPrimaryContainer = Morado,
    secondary = MoradoMedio,
    onSecondary = Blanco,
    secondaryContainer = MoradoSuave,
    onSecondaryContainer = Morado,
    background = Fondo,
    onBackground = Texto,
    surface = Blanco,
    onSurface = Texto,
    surfaceVariant = Fondo,
    onSurfaceVariant = TextoSuave,
    outline = Borde,
    outlineVariant = Borde,
    error = Rojo,
    onError = Blanco,
    errorContainer = RojoSuave,
    onErrorContainer = Rojo,
)

@Composable
fun AbundanciaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaAbundancia,
        typography = AbundanciaTypography,
        shapes = Shapes(
            small = RoundedCornerShape(9.dp),
            medium = RoundedCornerShape(10.dp),
            large = RoundedCornerShape(14.dp),
        ),
        content = content,
    )
}
