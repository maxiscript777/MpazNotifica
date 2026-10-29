package com.mpaznotifica.app.reglas

import com.mpaznotifica.app.model.AlcanceEnvio
import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.CanalDespacho
import com.mpaznotifica.app.model.Comunicacion
import com.mpaznotifica.app.model.ErroresComunicado
import com.mpaznotifica.app.model.Estudiante
import com.mpaznotifica.app.model.Perfil
import com.mpaznotifica.app.model.Taller
import com.mpaznotifica.app.model.TipoComunicacion
import com.mpaznotifica.app.model.Usuario

data class DatosReferencia(
    val cursos: List<String>,
    val talleres: List<Taller>,
    val estudiantes: List<Estudiante>,
    val apoderados: List<Apoderado>
)

object ReglasComunicado {

    const val MAX_ASUNTO = 100
    const val MSG_SOLO_CURSO = "Solo puedes enviar comunicados a tu curso."
    const val MSG_TIPO_NO_DISPONIBLE = "Este tipo de comunicado no está disponible para tu perfil."
    const val MSG_SIN_SESION = "Debes iniciar sesión para enviar comunicados."
    const val MSG_SIN_CURSO = "No tienes un curso asignado"
    const val MSG_SIN_DESTINATARIOS = "No hay destinatarios para este alcance"
    const val MSG_FORMATO_FECHA = "Formato de fecha: AAAA-MM-DD"
    const val MSG_FECHAS_INVERTIDAS = "La fecha Desde no puede ser mayor que Hasta"
    const val AVISO_FIRMA = "Esta autorización debe firmarse en papel y entregarse al establecimiento."

    fun tiposPermitidos(usuario: Usuario?): List<TipoComunicacion> = when (usuario?.perfil) {
        Perfil.INSPECTORIA_GENERAL -> TipoComunicacion.entries.toList()
        Perfil.PROFESOR_JEFE -> listOf(TipoComunicacion.PEDAGOGICA, TipoComunicacion.CITACION)
        null -> emptyList()
    }

    fun alcancesPermitidos(usuario: Usuario?): List<AlcanceEnvio> = when (usuario?.perfil) {
        Perfil.INSPECTORIA_GENERAL -> AlcanceEnvio.entries.toList()
        Perfil.PROFESOR_JEFE -> listOf(AlcanceEnvio.CURSO, AlcanceEnvio.APODERADO_INDIVIDUAL)
        null -> emptyList()
    }

    fun apoderadosDelCurso(curso: String, datos: DatosReferencia): List<Apoderado> {
        val ids = datos.estudiantes.filter { it.curso == curso }.map { it.id }.toSet()
        return datos.apoderados.filter { a -> a.pupilosIds.any { it in ids } }
    }

    fun destinosPara(alcance: AlcanceEnvio?, usuario: Usuario?, datos: DatosReferencia): List<String> {
        if (usuario == null || alcance == null) return emptyList()
        return if (usuario.perfil == Perfil.PROFESOR_JEFE) {
            val curso = usuario.cursoAsignado ?: return emptyList()
            when (alcance) {
                AlcanceEnvio.CURSO -> listOf(curso)
                AlcanceEnvio.APODERADO_INDIVIDUAL -> apoderadosDelCurso(curso, datos).map { it.nombreCompleto }
                AlcanceEnvio.TALLER, AlcanceEnvio.COLEGIO_COMPLETO -> emptyList()
            }
        } else {
            when (alcance) {
                AlcanceEnvio.CURSO -> datos.cursos
                AlcanceEnvio.TALLER -> datos.talleres.map { it.nombre }
                AlcanceEnvio.APODERADO_INDIVIDUAL -> datos.apoderados.map { it.nombreCompleto }
                AlcanceEnvio.COLEGIO_COMPLETO -> emptyList()
            }
        }
    }

    fun resolverApoderados(alcance: AlcanceEnvio, destino: String?, datos: DatosReferencia): List<Apoderado> =
        when (alcance) {
            AlcanceEnvio.COLEGIO_COMPLETO -> datos.apoderados
            AlcanceEnvio.CURSO -> apoderadosDelCurso(destino.orEmpty(), datos)
            AlcanceEnvio.TALLER -> {
                val tallerId = datos.talleres.find { it.nombre == destino }?.id
                val ids = datos.estudiantes.filter { tallerId != null && tallerId in it.talleresIds }.map { it.id }.toSet()
                datos.apoderados.filter { a -> a.pupilosIds.any { it in ids } }
            }
            AlcanceEnvio.APODERADO_INDIVIDUAL -> datos.apoderados.filter { it.nombreCompleto == destino }
        // distinctBy saca los apoderados repetidos
        }.distinctBy { it.id }

    fun validar(
        asunto: String,
        cuerpo: String,
        tipo: TipoComunicacion?,
        alcance: AlcanceEnvio?,
        destino: String?,
        usuario: Usuario?,
        datos: DatosReferencia
    ): ErroresComunicado {
        var errAsunto: String? = null
        var errCuerpo: String? = null
        var errTipo: String? = null
        var errAlcance: String? = null
        var errDestino: String? = null

        if (asunto.isBlank()) errAsunto = "El asunto es obligatorio"
        else if (asunto.trim().length > MAX_ASUNTO) errAsunto = "El asunto no puede superar los $MAX_ASUNTO caracteres"
        if (cuerpo.isBlank()) errCuerpo = "El cuerpo del mensaje es obligatorio"

        if (usuario == null) {
            errAlcance = MSG_SIN_SESION
        } else {
            if (tipo == null) errTipo = "Selecciona el tipo de comunicación"
            else if (tipo !in tiposPermitidos(usuario)) errTipo = MSG_TIPO_NO_DISPONIBLE

            if (alcance == null) {
                errAlcance = "Selecciona el alcance"
            } else if (usuario.perfil == Perfil.PROFESOR_JEFE && usuario.cursoAsignado == null) {
                errAlcance = MSG_SIN_CURSO
            } else if (alcance !in alcancesPermitidos(usuario)) {
                errAlcance = MSG_SOLO_CURSO
            } else if (alcance != AlcanceEnvio.COLEGIO_COMPLETO) {
                if (destino.isNullOrBlank()) {
                    errDestino = "Selecciona el destino"
                } else if (destino !in destinosPara(alcance, usuario, datos)) {
                    errDestino = if (usuario.perfil == Perfil.PROFESOR_JEFE) MSG_SOLO_CURSO else "El destino no es válido"
                }
            }
        }
        return ErroresComunicado(errAsunto, errCuerpo, errTipo, errAlcance, errDestino)
    }

    fun primerError(e: ErroresComunicado): String? =
        listOfNotNull(e.alcance, e.destino, e.tipo, e.asunto, e.cuerpo).firstOrNull()

    // con firma fisica va siempre por correo
    fun canalDe(apoderado: Apoderado, requiereFirmaFisica: Boolean): CanalDespacho =
        if (requiereFirmaFisica) CanalDespacho.EMAIL else apoderado.canalPreferente

    fun cuerpoFinal(cuerpo: String, requiereFirmaFisica: Boolean): String =
        if (requiereFirmaFisica) cuerpo.trim() + "\n\n" + AVISO_FIRMA else cuerpo.trim()

    fun canalesDisponibles(apoderado: Apoderado): List<CanalDespacho> = CanalDespacho.entries.filter {
        when (it) {
            CanalDespacho.EMAIL -> apoderado.correo.isNotBlank()
            CanalDespacho.SMS, CanalDespacho.WHATSAPP -> apoderado.telefono.isNotBlank()
        }
    }

    fun fechaValida(texto: String): Boolean {
        if (!Regex("""\d{4}-\d{2}-\d{2}""").matches(texto)) return false
        val mes = texto.substring(5, 7).toInt()
        val dia = texto.substring(8, 10).toInt()
        return mes in 1..12 && dia in 1..31
    }

    fun errorFechas(desde: String?, hasta: String?): String? {
        val d = desde?.trim().orEmpty()
        val h = hasta?.trim().orEmpty()
        if ((d.isNotEmpty() && !fechaValida(d)) || (h.isNotEmpty() && !fechaValida(h))) return MSG_FORMATO_FECHA
        if (d.isNotEmpty() && h.isNotEmpty() && d > h) return MSG_FECHAS_INVERTIDAS
        return null
    }

    fun filtrarHistorial(
        todos: List<Comunicacion>,
        tipo: TipoComunicacion?,
        emisorId: String?,
        desde: String?,
        hasta: String?
    ): List<Comunicacion> {
        val d = desde?.trim().orEmpty()
        val h = hasta?.trim().orEmpty()
        val invertidas = d.isNotEmpty() && h.isNotEmpty() && fechaValida(d) && fechaValida(h) && d > h
        val usarDesde = d.isNotEmpty() && fechaValida(d) && !invertidas
        val usarHasta = h.isNotEmpty() && fechaValida(h) && !invertidas
        return todos
            .filter { tipo == null || it.tipo == tipo }
            .filter { emisorId == null || it.autorId == emisorId }
            .filter { !usarDesde || it.fechaHora.take(10) >= d }
            .filter { !usarHasta || it.fechaHora.take(10) <= h }
            .sortedByDescending { it.fechaHora }
    }
}
