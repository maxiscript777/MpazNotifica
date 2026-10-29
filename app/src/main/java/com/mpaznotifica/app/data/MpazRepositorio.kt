package com.mpaznotifica.app.data

import com.mpaznotifica.app.data.local.ApoderadoEstudianteEntity
import com.mpaznotifica.app.data.local.AppDatabase
import com.mpaznotifica.app.data.local.EstudianteEntity
import com.mpaznotifica.app.data.local.EstudianteTallerEntity
import com.mpaznotifica.app.data.local.aEntidad
import com.mpaznotifica.app.data.local.aModelo
import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.CanalDespacho
import com.mpaznotifica.app.model.Comunicacion
import com.mpaznotifica.app.model.EnvioDestinatario
import com.mpaznotifica.app.model.Estudiante
import com.mpaznotifica.app.model.Taller
import com.mpaznotifica.app.model.Usuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class MpazRepositorio(db: AppDatabase) {

    private val usuarioDao = db.usuarioDao()
    private val referenciaDao = db.referenciaDao()
    private val apoderadoDao = db.apoderadoDao()
    private val comunicacionDao = db.comunicacionDao()

    val usuarios: Flow<List<Usuario>> = usuarioDao.todos().map { l -> l.map { it.aModelo() } }

    val talleres: Flow<List<Taller>> = referenciaDao.talleres().map { l -> l.map { it.aModelo() } }

    val estudiantes: Flow<List<Estudiante>> =
        combine(referenciaDao.estudiantes(), referenciaDao.estudianteTaller()) { estudiantes, relaciones ->
            estudiantes.map { e ->
                Estudiante(e.id, e.nombreReferencial, e.curso, relaciones.filter { it.estudianteId == e.id }.map { it.tallerId })
            }
        }

    val apoderados: Flow<List<Apoderado>> =
        combine(apoderadoDao.todos(), apoderadoDao.relaciones()) { apoderados, relaciones ->
            apoderados.map { a -> a.aModelo(relaciones.filter { it.apoderadoId == a.id }.map { it.estudianteId }) }
        }

    val comunicados: Flow<List<Comunicacion>> = comunicacionDao.todas().map { l -> l.map { it.aModelo() } }

    val envios: Flow<List<EnvioDestinatario>> = comunicacionDao.todosLosEnvios().map { l -> l.map { it.aModelo() } }

    // solo se insert los comunicados no se editan
    suspend fun registrarComunicado(comunicado: Comunicacion, envios: List<EnvioDestinatario>) {
        comunicacionDao.registrarComunicado(comunicado.aEntidad(), envios.map { it.aEntidad() })
    }

    suspend fun actualizarCanal(apoderadoId: String, canal: CanalDespacho) {
        apoderadoDao.actualizarCanal(apoderadoId, canal.name)
    }

    // datos de prueba solo si la base esta vacia
    suspend fun cargarDatosIniciales() {
        if (usuarioDao.contar() > 0) return
        usuarioDao.insertarTodos(MockData.usuarios.map { it.aEntidad() })
        referenciaDao.insertarTalleres(MockData.talleres.map { it.aEntidad() })
        referenciaDao.insertarEstudiantes(MockData.estudiantes.map { EstudianteEntity(it.id, it.nombreReferencial, it.curso) })
        referenciaDao.insertarEstudianteTaller(
            MockData.estudiantes.flatMap { e -> e.talleresIds.map { EstudianteTallerEntity(e.id, it) } }
        )
        apoderadoDao.insertarTodos(MockData.apoderados.map { it.aEntidad() })
        apoderadoDao.insertarRelaciones(
            MockData.apoderados.flatMap { a -> a.pupilosIds.map { ApoderadoEstudianteEntity(a.id, it) } }
        )
        MockData.comunicados.forEach { c ->
            registrarComunicado(c, MockData.envios.filter { it.comunicacionId == c.id })
        }
    }
}
