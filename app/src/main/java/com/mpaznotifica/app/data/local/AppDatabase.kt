package com.mpaznotifica.app.data.local

import  android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UsuarioEntity::class, EstudianteEntity::class, TallerEntity::class, EstudianteTallerEntity::class,
        ApoderadoEntity::class, ApoderadoEstudianteEntity::class, ComunicacionEntity::class, EnvioEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun referenciaDao(): ReferenciaDao
    abstract fun apoderadoDao(): ApoderadoDao
    abstract fun comunicacionDao(): ComunicacionDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        // una sola base para toda la app
        fun obtener(context: Context): AppDatabase = instancia ?: synchronized(this) {
            instancia ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "mpaz_notifica.db")
                .build().also { instancia = it }
        }
    }
}
