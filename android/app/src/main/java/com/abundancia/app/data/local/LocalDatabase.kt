package com.abundancia.app.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/** Estados de un registro creado en el celular mientras viaja hacia el servidor. */
object EstadoSync {
    const val BORRADOR = "BORRADOR"     // formulario a medio llenar (no se envía)
    const val PENDIENTE = "PENDIENTE"   // listo, esperando conexión
    const val ENVIANDO = "ENVIANDO"
    const val ENVIADO = "ENVIADO"       // confirmado por el servidor
    const val ERROR = "ERROR"           // el servidor lo rechazó (se conserva para corregir o descartar)
}

// ─────────────────────────────────────────────────────────────────────────────
// Entidades
// ─────────────────────────────────────────────────────────────────────────────

/** Copia local de las publicaciones que devolvió el servidor (para ver la lista sin internet). */
@Entity(tableName = "publicaciones_cache")
data class PublicacionCacheEntity(
    @PrimaryKey val id: String,
    val titulo: String,
    val descripcion: String,
    val categoria: String,
    val cantidad: Int,
    val ubicacion: String,
    val estado: String,
    val autorId: String,
    val autorNombre: String,
    val createdAt: String?,
    val guardadoEn: Long,
    val imagen: String? = null,
)

/** Borrador del formulario "Publicar" y cola de publicaciones por enviar. */
@Entity(tableName = "borradores_publicacion")
data class BorradorPublicacionEntity(
    @PrimaryKey val localId: String,
    val usuarioId: String,
    val titulo: String = "",
    val descripcion: String = "",
    val categoria: String = "",
    val cantidad: String = "1",
    val ubicacion: String = "",
    val estadoSync: String = EstadoSync.BORRADOR,
    val error: String? = null,
    val servidorId: String? = null,
    val creadoEn: Long,
    val actualizadoEn: Long,
    /** Foto elegida en el celular (archivo comprimido en la memoria de la app) pendiente de subir. */
    val imagenLocal: String? = null,
)

/** Solicitudes hechas desde el celular, en cola hasta que el servidor las confirme. */
@Entity(tableName = "solicitudes_pendientes")
data class SolicitudPendienteEntity(
    @PrimaryKey val localId: String,
    val usuarioId: String,
    val publicacionId: String,
    val publicacionTitulo: String,
    val mensaje: String,
    val estadoSync: String = EstadoSync.PENDIENTE,
    val error: String? = null,
    val creadoEn: Long,
)

/** Copia local de las solicitudes que devolvió el servidor. */
@Entity(tableName = "solicitudes_cache")
data class SolicitudCacheEntity(
    @PrimaryKey val id: String,
    val publicacionId: String,
    val publicacionTitulo: String,
    val categoria: String,
    val ubicacion: String,
    val duenoId: String,
    val solicitanteId: String,
    val solicitanteNombre: String,
    val mensaje: String,
    val estado: String,
    val createdAt: String?,
    val guardadoEn: Long,
)

// ─────────────────────────────────────────────────────────────────────────────
// DAOs
// ─────────────────────────────────────────────────────────────────────────────

@Dao
abstract class PublicacionCacheDao {
    @Query("SELECT * FROM publicaciones_cache ORDER BY createdAt DESC")
    abstract fun observarTodas(): Flow<List<PublicacionCacheEntity>>

    @Query("SELECT * FROM publicaciones_cache WHERE id = :id LIMIT 1")
    abstract suspend fun porId(id: String): PublicacionCacheEntity?

    @Query("SELECT MAX(guardadoEn) FROM publicaciones_cache")
    abstract fun observarUltimaActualizacion(): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun guardar(lista: List<PublicacionCacheEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun guardarUna(item: PublicacionCacheEntity)

    @Query("DELETE FROM publicaciones_cache WHERE id = :id")
    abstract suspend fun borrar(id: String)

    @Query("DELETE FROM publicaciones_cache")
    abstract suspend fun borrarTodas()

    /** La copia local se reemplaza completa por lo que tiene el servidor. */
    @Transaction
    open suspend fun reemplazarTodas(lista: List<PublicacionCacheEntity>) {
        borrarTodas()
        guardar(lista)
    }
}

@Dao
abstract class BorradorDao {
    @Query("SELECT * FROM borradores_publicacion WHERE usuarioId = :usuarioId AND estadoSync = 'BORRADOR' LIMIT 1")
    abstract suspend fun borradorActual(usuarioId: String): BorradorPublicacionEntity?

    @Query("SELECT * FROM borradores_publicacion WHERE usuarioId = :usuarioId AND estadoSync != 'BORRADOR' ORDER BY creadoEn DESC")
    abstract fun observarEnvios(usuarioId: String): Flow<List<BorradorPublicacionEntity>>

    @Query("SELECT * FROM borradores_publicacion WHERE usuarioId = :usuarioId AND estadoSync = 'PENDIENTE' ORDER BY creadoEn")
    abstract suspend fun pendientes(usuarioId: String): List<BorradorPublicacionEntity>

    @Query("SELECT COUNT(*) FROM borradores_publicacion WHERE usuarioId = :usuarioId AND estadoSync IN ('PENDIENTE', 'ENVIANDO')")
    abstract fun observarPorEnviar(usuarioId: String): Flow<Int>

    /** Si la app se cerró a mitad de un envío, se vuelve a intentar (el clienteId evita duplicados). */
    @Query("UPDATE borradores_publicacion SET estadoSync = 'PENDIENTE' WHERE estadoSync = 'ENVIANDO'")
    abstract suspend fun reiniciarEnviando()

    @Query("SELECT * FROM borradores_publicacion WHERE localId = :localId LIMIT 1")
    abstract suspend fun porId(localId: String): BorradorPublicacionEntity?

    @Query("UPDATE borradores_publicacion SET imagenLocal = NULL WHERE localId = :localId")
    abstract suspend fun quitarImagenLocal(localId: String)

    /** servidorId solo se reemplaza si llega uno nuevo (no se pierde el id ya asignado). */
    @Query("UPDATE borradores_publicacion SET estadoSync = :estado, error = :error, servidorId = COALESCE(:servidorId, servidorId), actualizadoEn = :ahora WHERE localId = :localId")
    abstract suspend fun actualizarEstado(localId: String, estado: String, error: String?, servidorId: String?, ahora: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun guardar(borrador: BorradorPublicacionEntity)

    @Query("DELETE FROM borradores_publicacion WHERE localId = :localId")
    abstract suspend fun borrar(localId: String)

    @Query("DELETE FROM borradores_publicacion WHERE usuarioId = :usuarioId AND estadoSync = 'ENVIADO'")
    abstract suspend fun limpiarEnviados(usuarioId: String)
}

@Dao
abstract class SolicitudPendienteDao {
    @Query("SELECT * FROM solicitudes_pendientes WHERE usuarioId = :usuarioId ORDER BY creadoEn DESC")
    abstract fun observar(usuarioId: String): Flow<List<SolicitudPendienteEntity>>

    @Query("SELECT * FROM solicitudes_pendientes WHERE usuarioId = :usuarioId AND publicacionId = :publicacionId ORDER BY creadoEn DESC LIMIT 1")
    abstract fun observarPara(usuarioId: String, publicacionId: String): Flow<SolicitudPendienteEntity?>

    @Query("SELECT * FROM solicitudes_pendientes WHERE usuarioId = :usuarioId AND publicacionId = :publicacionId AND estadoSync IN ('PENDIENTE', 'ENVIANDO', 'ENVIADO') LIMIT 1")
    abstract suspend fun activaPara(usuarioId: String, publicacionId: String): SolicitudPendienteEntity?

    @Query("SELECT * FROM solicitudes_pendientes WHERE usuarioId = :usuarioId AND estadoSync = 'PENDIENTE' ORDER BY creadoEn")
    abstract suspend fun pendientes(usuarioId: String): List<SolicitudPendienteEntity>

    @Query("SELECT COUNT(*) FROM solicitudes_pendientes WHERE usuarioId = :usuarioId AND estadoSync IN ('PENDIENTE', 'ENVIANDO')")
    abstract fun observarPorEnviar(usuarioId: String): Flow<Int>

    @Query("UPDATE solicitudes_pendientes SET estadoSync = 'PENDIENTE' WHERE estadoSync = 'ENVIANDO'")
    abstract suspend fun reiniciarEnviando()

    @Query("UPDATE solicitudes_pendientes SET estadoSync = :estado, error = :error WHERE localId = :localId")
    abstract suspend fun actualizarEstado(localId: String, estado: String, error: String?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun guardar(item: SolicitudPendienteEntity)

    @Query("DELETE FROM solicitudes_pendientes WHERE localId = :localId")
    abstract suspend fun borrar(localId: String)
}

@Dao
abstract class SolicitudCacheDao {
    @Query("SELECT * FROM solicitudes_cache ORDER BY createdAt DESC")
    abstract fun observarTodas(): Flow<List<SolicitudCacheEntity>>

    @Query("SELECT MAX(guardadoEn) FROM solicitudes_cache")
    abstract fun observarUltimaActualizacion(): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun guardar(lista: List<SolicitudCacheEntity>)

    @Query("DELETE FROM solicitudes_cache")
    abstract suspend fun borrarTodas()

    @Transaction
    open suspend fun reemplazarTodas(lista: List<SolicitudCacheEntity>) {
        borrarTodas()
        guardar(lista)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Base de datos
// ─────────────────────────────────────────────────────────────────────────────

@Database(
    entities = [
        PublicacionCacheEntity::class,
        BorradorPublicacionEntity::class,
        SolicitudPendienteEntity::class,
        SolicitudCacheEntity::class,
    ],
    version = 2,
    exportSchema = true,   // guarda el esquema de cada versión en app/schemas
)
abstract class AbundanciaDatabase : RoomDatabase() {
    abstract fun publicacionCacheDao(): PublicacionCacheDao
    abstract fun borradorDao(): BorradorDao
    abstract fun solicitudPendienteDao(): SolicitudPendienteDao
    abstract fun solicitudCacheDao(): SolicitudCacheDao

    companion object {
        fun crear(context: Context, nombre: String = "abundancia.db"): AbundanciaDatabase =
            Room.databaseBuilder(context.applicationContext, AbundanciaDatabase::class.java, nombre)
                .addMigrations(MIGRACION_1_2)   // cada versión nueva agrega aquí su Migration(n, n + 1)
                .build()
    }
}

/** Versión 2: fotos en la copia local y en los borradores (no se pierden los datos guardados). */
val MIGRACION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE publicaciones_cache ADD COLUMN imagen TEXT")
        db.execSQL("ALTER TABLE borradores_publicacion ADD COLUMN imagenLocal TEXT")
    }
}
