package com.example.ventasapp.model.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Punto de entrada de Room: declara las tablas y entrega los DAOs.
 * Mismo patrón de la guía de persistencia (UsuarioDatabase), con varias tablas.
 *
 * VERSIÓN 2: se agregó la tabla "comisiones" (porcentajes configurables por el administrador).
 * La migración 1 -> 2 crea solo esa tabla y CONSERVA los usuarios y ventas que ya existían en el
 * teléfono (si no hubiera migración, Room lanzaría un error al abrir la base).
 *
 * VERSIÓN 3: se eliminó el perfil SUPERVISOR. La migración 2 -> 3 borra las cuentas que lo tenían
 * (si no, Perfil.desde() las convertiría en vendedores). No cambia el esquema de las tablas.
 *
 * exportSchema = false porque no estamos versionando el esquema en archivos JSON.
 */
@Database(
    entities = [
        UsuarioEntity::class,
        VentaEntity::class,
        HistorialEntity::class,
        NotificacionEntity::class,
        ComisionEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class VentasDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun ventaDao(): VentaDao
    abstract fun historialDao(): HistorialDao
    abstract fun notificacionDao(): NotificacionDao
    abstract fun comisionDao(): ComisionDao

    companion object {
        // @Volatile asegura que todos los hilos vean la instancia apenas se crea.
        @Volatile
        private var INSTANCE: VentasDatabase? = null

        /** Migración 1 -> 2: crea la tabla de comisiones. El SQL debe coincidir con la entidad. */
        private val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `comisiones` " +
                        "(`tipoConvenio` TEXT NOT NULL, `porcentaje` REAL NOT NULL, PRIMARY KEY(`tipoConvenio`))"
                )
            }
        }

        /** Migración 2 -> 3: el perfil SUPERVISOR ya no existe, se eliminan sus cuentas. */
        private val MIGRACION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM `usuarios` WHERE `perfil` = 'SUPERVISOR'")
            }
        }

        /** Singleton: se crea una sola base para toda la app (abrirla varias veces es costoso). */
        fun getDatabase(context: Context): VentasDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    VentasDatabase::class.java,
                    "ventas_database"
                ).addMigrations(MIGRACION_1_2, MIGRACION_2_3).build().also { INSTANCE = it }
            }
        }
    }
}
