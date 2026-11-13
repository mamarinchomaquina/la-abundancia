package com.abundancia.app.data

import android.content.Context
import com.abundancia.app.data.local.AbundanciaDatabase
import com.abundancia.app.data.repo.AuthRepository
import com.abundancia.app.data.repo.PublicacionesRepository
import com.abundancia.app.data.repo.SolicitudesRepository
import com.abundancia.app.data.sync.Conectividad

/** Crea una sola vez la base local, los repositorios y el observador de red. */
object AppContainer {
    lateinit var appContext: Context
        private set
    lateinit var db: AbundanciaDatabase
        private set
    lateinit var conectividad: Conectividad
        private set
    lateinit var auth: AuthRepository
        private set
    lateinit var publicaciones: PublicacionesRepository
        private set
    lateinit var solicitudes: SolicitudesRepository
        private set

    fun init(context: Context) {
        val app = context.applicationContext
        appContext = app
        db = AbundanciaDatabase.crear(app)
        conectividad = Conectividad(app)
        auth = AuthRepository(app, db)
        publicaciones = PublicacionesRepository(app, db)
        solicitudes = SolicitudesRepository(app, db)
    }
}
