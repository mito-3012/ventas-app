
package com.example.ventasapp.model.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Punto de entrada de Room.
 *
 * VERSION 2: Se agregó la tabla comisiones.
 * VERSION 3: Se eliminaron las cuentas con perfil SUPERVISOR.
 * VERSION 4: Se agregó convenioExternoId a la tabla ventas.
 *
 * Se conservan los datos existentes mediante migraciones.
 */
@Database(
    entities = [
        UsuarioEntity::class,
        VentaEntity::class,
        HistorialEntity::class,
        NotificacionEntity::class,
        ComisionEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class VentasDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun ventaDao(): VentaDao
    abstract fun historialDao(): HistorialDao
    abstract fun notificacionDao(): NotificacionDao
    abstract fun comisionDao(): ComisionDao

    companion object {

        @Volatile
        private var INSTANCE: VentasDatabase? = null

        /**
         * Migración 1 -> 2:
         * Crea la tabla de comisiones.
         *
         * IMPORTANTE:
         * Este SQL debe coincidir exactamente con ComisionEntity.
         * Debes conservar aquí el SQL original de tu proyecto.
         */
        private val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Coloca aquí el SQL original que crea la tabla comisiones.
            }
        }

        /**
         * Migración 2 -> 3:
         * Elimina las cuentas con perfil SUPERVISOR.
         */
        private val MIGRACION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "DELETE FROM `usuarios` WHERE `perfil` = 'SUPERVISOR'"
                )
            }
        }

        /**
         * Migración 3 -> 4:
         * Agrega el ID del convenio externo a las ventas.
         */
        private val MIGRACION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `ventas` ADD COLUMN `convenioExternoId` INTEGER"
                )
            }
        }

        /**
         * Singleton: mantiene una sola instancia de la base de datos.
         */
        fun getDatabase(context: Context): VentasDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VentasDatabase::class.java,
                    "ventas_database"
                )
                    .addMigrations(
                        MIGRACION_1_2,
                        MIGRACION_2_3,
                        MIGRACION_3_4
                    )
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
