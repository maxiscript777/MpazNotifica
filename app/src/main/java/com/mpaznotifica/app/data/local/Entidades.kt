package com.mpaznotifica.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mpaznotifica.app.model.AlcanceEnvio
import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.CanalDespacho
import com.mpaznotifica.app.model.Comunicacion
import com.mpaznotifica.app.model.EnvioDestinatario
import com.mpaznotifica.app.model.EstadoEnvio
import com.mpaznotifica.app.model.Perfil
import com.mpaznotifica.app.model.Taller
import com.mpaznotifica.app.model.TipoComunicacion
import com.mpaznotifica.app.model.Usuario

@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val usuario: String,
    val clave: String,
    val perfil: String,
    val cursoAsignado: String?
)

@Entity(tableName = "estudiantes")
data class EstudianteEntity(
    @PrimaryKey val id: String,
    val nombreReferencial: String,
    val curso: String
)

@Entity(tableName = "talleres")
data class TallerEntity(
    @PrimaryKey val id: String,
    val nombre: String
)

@Entity(tableName = "estudiante_taller", primaryKeys = ["estudianteId", "tallerId"])
data class EstudianteTallerEntity(
    val estudianteId: String,
    val tallerId: String
)

@Entity(tableName = "apoderados")
data class ApoderadoEntity(
    @PrimaryKey val id: String,
    val nombreCompleto: String,
    val correo: String,
    val telefono: String,
    val canalPreferente: String
)

@Entity(tableName = "apoderado_estudiante", primaryKeys = ["apoderadoId", "estudianteId"])
data class ApoderadoEstudianteEntity(
    val apoderadoId: String,
    val estudianteId: String
)

@Entity(
    tableName = "comunicaciones",
    foreignKeys = [ForeignKey(entity = UsuarioEntity::class, parentColumns = ["id"], childColumns = ["autorId"])],
    indices = [Index("autorId")]
)
data class ComunicacionEntity(
    @PrimaryKey val id: String,
    val asunto: String,
    val cuerpo: String,
    val tipo: String,
    val alcance: String,
    val destinoReferencia: String?,
    val adjuntoPdfNombre: String?,
    val requiereFirmaFisica: Boolean,
    val autorId: String,
    val autorNombre: String,
    val fechaHora: String
)

@Entity(
    tableName = "envios",
    foreignKeys = [
        ForeignKey(entity = ComunicacionEntity::class, parentColumns = ["id"], childColumns = ["comunicacionId"]),
        ForeignKey(entity = ApoderadoEntity::class, parentColumns = ["id"], childColumns = ["apoderadoId"])
    ],
    // no deja guardar dos envios al mismo apoderado en el mismo comunicado
    indices = [
        Index(value = ["comunicacionId", "apoderadoId"], unique = true),
        Index("apoderadoId")
    ]
)
data class EnvioEntity(
    @PrimaryKey val id: String,
    val comunicacionId: String,
    val apoderadoId: String,
    val canal: String,
    val estado: String
)

fun UsuarioEntity.aModelo() = Usuario(id, nombre, usuario, clave, Perfil.valueOf(perfil), cursoAsignado)
fun Usuario.aEntidad() = UsuarioEntity(id, nombre, usuario, clave, perfil.name, cursoAsignado)

fun TallerEntity.aModelo() = Taller(id, nombre)
fun Taller.aEntidad() = TallerEntity(id, nombre)

fun ApoderadoEntity.aModelo(pupilosIds: List<String>) =
    Apoderado(id, nombreCompleto, correo, telefono, CanalDespacho.valueOf(canalPreferente), pupilosIds)
fun Apoderado.aEntidad() = ApoderadoEntity(id, nombreCompleto, correo, telefono, canalPreferente.name)

fun ComunicacionEntity.aModelo() = Comunicacion(
    id = id, asunto = asunto, cuerpo = cuerpo,
    tipo = TipoComunicacion.valueOf(tipo), alcance = AlcanceEnvio.valueOf(alcance),
    destinoReferencia = destinoReferencia, adjuntoPdfNombre = adjuntoPdfNombre,
    requiereFirmaFisica = requiereFirmaFisica, autorId = autorId, autorNombre = autorNombre, fechaHora = fechaHora
)
fun Comunicacion.aEntidad() = ComunicacionEntity(
    id = id, asunto = asunto, cuerpo = cuerpo, tipo = tipo.name, alcance = alcance.name,
    destinoReferencia = destinoReferencia, adjuntoPdfNombre = adjuntoPdfNombre,
    requiereFirmaFisica = requiereFirmaFisica, autorId = autorId, autorNombre = autorNombre, fechaHora = fechaHora
)

fun EnvioEntity.aModelo() =
    EnvioDestinatario(id, comunicacionId, apoderadoId, CanalDespacho.valueOf(canal), EstadoEnvio.valueOf(estado))
fun EnvioDestinatario.aEntidad() = EnvioEntity(id, comunicacionId, apoderadoId, canal.name, estado.name)
