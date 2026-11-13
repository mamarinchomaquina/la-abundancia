package com.abundancia.app.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abundancia.app.data.AppContainer
import kotlinx.coroutines.launch

private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

class LoginViewModel : ViewModel() {
    var email by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set

    var errorEmail by mutableStateOf<String?>(null)
        private set
    var errorPassword by mutableStateOf<String?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var cargando by mutableStateOf(false)
        private set

    fun cambiarEmail(v: String) { email = v; errorEmail = null }
    fun cambiarPassword(v: String) { password = v; errorPassword = null }

    fun ingresar() {
        if (cargando) return   // evita envíos repetidos
        errorEmail = if (!EMAIL_REGEX.matches(email.trim())) "Escribe un correo válido." else null
        errorPassword = if (password.isBlank()) "Escribe tu contraseña." else null
        if (errorEmail != null || errorPassword != null) return

        viewModelScope.launch {
            cargando = true
            error = null
            AppContainer.auth.login(email, password)
                .onFailure { error = it.message ?: "No se pudo iniciar sesión." }
            cargando = false
        }
    }
}

class RegistroViewModel : ViewModel() {
    var nombre by mutableStateOf("")
        private set
    var email by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var confirmar by mutableStateOf("")
        private set

    var errorNombre by mutableStateOf<String?>(null)
        private set
    var errorEmail by mutableStateOf<String?>(null)
        private set
    var errorPassword by mutableStateOf<String?>(null)
        private set
    var errorConfirmar by mutableStateOf<String?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var cargando by mutableStateOf(false)
        private set

    fun cambiarNombre(v: String) { nombre = v; errorNombre = null }
    fun cambiarEmail(v: String) { email = v; errorEmail = null }
    fun cambiarPassword(v: String) { password = v; errorPassword = null }
    fun cambiarConfirmar(v: String) { confirmar = v; errorConfirmar = null }

    fun registrar() {
        if (cargando) return
        errorNombre = if (nombre.isBlank()) "Escribe tu nombre." else null
        errorEmail = if (!EMAIL_REGEX.matches(email.trim())) "Escribe un correo válido." else null
        errorPassword = if (password.length < 8) "Mínimo 8 caracteres." else null
        errorConfirmar = if (confirmar != password) "Las contraseñas no coinciden." else null
        if (listOf(errorNombre, errorEmail, errorPassword, errorConfirmar).any { it != null }) return

        viewModelScope.launch {
            cargando = true
            error = null
            AppContainer.auth.registro(nombre, email, password)
                .onFailure { error = it.message ?: "No se pudo crear la cuenta." }
            cargando = false
        }
    }
}
