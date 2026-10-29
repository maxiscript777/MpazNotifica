package com.mpaznotifica.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Query("SELECT * FROM usuarios")
    fun todos(): Flow<List<UsuarioEntity>>

    @Query("SELECT COUNT(*) FROM usuarios")
    suspend fun contar(): Int

    @Insert
    suspend fun insertarTodos(usuarios: List<UsuarioEntity>)
}

@Dao
interface ReferenciaDao {
    @Query("SELECT * FROM estudiantes")
    fun estudiantes(): Flow<List<EstudianteEntity>>

    @Query("SELECT * FROM talleres")
    fun talleres(): Flow<List<TallerEntity>>

    @Query("SELECT * FROM estudiante_taller")
    fun estudianteTaller(): Flow<List<EstudianteTallerEntity>>

    @Insert
    suspend fun insertarEstudiantes(estudiantes: List<EstudianteEntity>)

    @Insert
    suspend fun insertarTalleres(talleres: List<TallerEntity>)

    @Insert
    suspend fun insertarEstudianteTaller(relaciones: List<EstudianteTallerEntity>)
}

@Dao
interface ApoderadoDao {
    @Query("SELECT * FROM apoderados")
    fun todos(): Flow<List<ApoderadoEntity>>

    @Query("SELECT * FROM apoderado_estudiante")
    fun relaciones(): Flow<List<ApoderadoEstudianteEntity>>

    @Insert
    suspend fun insertarTodos(apoderados: List<ApoderadoEntity>)

    @Insert
    suspend fun insertarRelaciones(relaciones: List<ApoderadoEstudianteEntity>)

    @Query("UPDATE apoderados SET canalPreferente = :canal WHERE id = :apoderadoId")
    suspend fun actualizarCanal(apoderadoId: String, canal: String)
}

@Dao
abstract class ComunicacionDao {
    @Query("SELECT * FROM comunicaciones ORDER BY fechaHora DESC")
    abstract fun todas(): Flow<List<ComunicacionEntity>>

    @Query("SELECT * FROM envios")
    abstract fun todosLosEnvios(): Flow<List<EnvioEntity>>

    @Insert
    abstract suspend fun insertarComunicacion(comunicacion: ComunicacionEntity)

    @Insert
    abstract suspend fun insertarEnvios(envios: List<EnvioEntity>)

    // junta los dos insert: o se guarda todo o nada
    @Transaction
    open suspend fun registrarComunicado(comunicacion: ComunicacionEntity, envios: List<EnvioEntity>) {
        insertarComunicacion(comunicacion)
        insertarEnvios(envios)
    }
}
