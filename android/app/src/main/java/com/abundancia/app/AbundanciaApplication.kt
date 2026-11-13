package com.abundancia.app

import android.app.Application
import com.abundancia.app.data.AppContainer
import com.abundancia.app.data.Session
import com.abundancia.app.data.sync.Sincronizador

class AbundanciaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Session.init(this)        // DataStore
        AppContainer.init(this)   // Room + repositorios + red
        Sincronizador.programar(this) // envía lo que haya quedado pendiente
    }
}
