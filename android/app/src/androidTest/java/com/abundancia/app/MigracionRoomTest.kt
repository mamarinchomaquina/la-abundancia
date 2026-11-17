package com.abundancia.app

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.abundancia.app.data.local.AbundanciaDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pruebas de actualización de la base local (Room).
 * Usan un archivo aparte ("prueba_migracion.db"): no tocan los datos reales de la app.
 */
@RunWith(AndroidJUnit4::class)
class MigracionRoomTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val nombre = "prueba_migracion.db"

    @Before
    fun limpiar() {
        context.deleteDatabase(nombre)
    }

    /** Crea a mano la base como la dejaría otra versión de la app, con un borrador guardado. */
    private fun crearBase(version: Int, conFotos: Boolean) {
        val db = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(nombre), null)
        val imagen = if (conFotos) ", `imagen` TEXT" else ""
        val imagenLocal = if (conFotos) ", `imagenLocal` TEXT" else ""
        db.execSQL("CREATE TABLE `publicaciones_cache` (`id` TEXT NOT NULL, `titulo` TEXT NOT NULL, `descripcion` TEXT NOT NULL, `categoria` TEXT NOT NULL, `cantidad` INTEGER NOT NULL, `ubicacion` TEXT NOT NULL, `estado` TEXT NOT NULL, `autorId` TEXT NOT NULL, `autorNombre` TEXT NOT NULL, `createdAt` TEXT, `guardadoEn` INTEGER NOT NULL$imagen, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE `borradores_publicacion` (`localId` TEXT NOT NULL, `usuarioId` TEXT NOT NULL, `titulo` TEXT NOT NULL, `descripcion` TEXT NOT NULL, `categoria` TEXT NOT NULL, `cantidad` TEXT NOT NULL, `ubicacion` TEXT NOT NULL, `estadoSync` TEXT NOT NULL, `error` TEXT, `servidorId` TEXT, `creadoEn` INTEGER NOT NULL, `actualizadoEn` INTEGER NOT NULL$imagenLocal, PRIMARY KEY(`localId`))")
        db.execSQL("CREATE TABLE `solicitudes_pendientes` (`localId` TEXT NOT NULL, `usuarioId` TEXT NOT NULL, `publicacionId` TEXT NOT NULL, `publicacionTitulo` TEXT NOT NULL, `mensaje` TEXT NOT NULL, `estadoSync` TEXT NOT NULL, `error` TEXT, `creadoEn` INTEGER NOT NULL, PRIMARY KEY(`localId`))")
        db.execSQL("CREATE TABLE `solicitudes_cache` (`id` TEXT NOT NULL, `publicacionId` TEXT NOT NULL, `publicacionTitulo` TEXT NOT NULL, `categoria` TEXT NOT NULL, `ubicacion` TEXT NOT NULL, `duenoId` TEXT NOT NULL, `solicitanteId` TEXT NOT NULL, `solicitanteNombre` TEXT NOT NULL, `mensaje` TEXT NOT NULL, `estado` TEXT NOT NULL, `createdAt` TEXT, `guardadoEn` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("INSERT INTO borradores_publicacion (localId, usuarioId, titulo, descripcion, categoria, cantidad, ubicacion, estadoSync, creadoEn, actualizadoEn) VALUES ('borrador-1', 'u1', 'Arroz y lentejas', 'Para una familia', 'Alimentos', '5', 'Barrio Centro', 'PENDIENTE', 1, 1)")
        db.version = version
        db.close()
    }

    /** Cuenta los borradores leyendo el archivo directamente, sin pasar por Room. */
    private fun borradoresEnElArchivo(): Int {
        val db = SQLiteDatabase.openDatabase(context.getDatabasePath(nombre).path, null, SQLiteDatabase.OPEN_READONLY)
        val c = db.rawQuery("SELECT COUNT(*) FROM borradores_publicacion", null)
        c.moveToFirst()
        val total = c.getInt(0)
        c.close()
        db.close()
        return total
    }

    @Test
    fun actualizarDeLaVersion1ALa2ConservaElBorrador() {
        crearBase(version = 1, conFotos = false)
        val room = AbundanciaDatabase.crear(context, nombre)
        val borrador = runBlocking { room.borradorDao().porId("borrador-1") }
        room.close()
        assertNotNull("El borrador se perdió al actualizar de la versión 1 a la 2", borrador)
        assertEquals("Arroz y lentejas", borrador!!.titulo)
    }

    @Test
    fun sinRutaDeMigracionNoSeBorranLosBorradores() {
        // Base de otra versión (3) para la que esta app no tiene migración
        crearBase(version = 3, conFotos = true)
        val room = AbundanciaDatabase.crear(context, nombre)
        runCatching { runBlocking { room.borradorDao().porId("borrador-1") } }
        room.close()
        assertEquals("Room borró las tablas y se perdió el borrador", 1, borradoresEnElArchivo())
    }
}
