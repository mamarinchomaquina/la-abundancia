package com.abundancia.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Mismos colores que las variables CSS del panel web (web-angular/src/styles.css)
val Morado = Color(0xFF6B21A8)        // --morado
val MoradoHover = Color(0xFF581C87)   // --morado-hover
val MoradoSuave = Color(0xFFF3E8FF)   // --morado-suave
val MoradoMedio = Color(0xFF7C3AED)   // --morado-medio
val MoradoOscuro = Color(0xFF4C1D95)
val MoradoNoche = Color(0xFF2D0A6E)
val Fondo = Color(0xFFF8F7FA)         // --fondo
val Blanco = Color(0xFFFFFFFF)        // --blanco
val Texto = Color(0xFF1A1A1F)         // --texto
val TextoSuave = Color(0xFF72726D)    // --texto-suave
val Borde = Color(0xFFE5E3EC)         // --borde
val Placeholder = Color(0xFFBCBAB5)
val Rojo = Color(0xFFC0392B)          // --rojo
val RojoSuave = Color(0xFFFBECEB)     // --rojo-suave
val Amarillo = Color(0xFFB07D2D)      // --amarillo
val AmarilloSuave = Color(0xFFFDF5E6) // --amarillo-suave

// Fondo de la pantalla de login del panel (.pagina)
val GradienteLogin = Brush.linearGradient(listOf(MoradoNoche, MoradoOscuro, Morado, MoradoMedio))

// Sidebar del panel (.sidebar)
val GradienteSidebar = Brush.linearGradient(listOf(MoradoOscuro, Morado, MoradoMedio))

// Botón principal del panel
val GradienteBoton = Brush.linearGradient(listOf(Morado, MoradoMedio))
