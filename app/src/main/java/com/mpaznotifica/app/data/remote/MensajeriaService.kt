package com.mpaznotifica.app.data.remote

import com.mpaznotifica.app.model.CanalDespacho
import com.mpaznotifica.app.model.EstadoEnvio

data class MensajeDto(
    val asunto: String,
    val cuerpo: String,
    val canal: CanalDespacho,
    val correo: String,
    val telefono: String
)

data class RespuestaEnvioDto(val estado: EstadoEnvio)

interface MensajeriaService {
    suspend fun enviar(mensaje: MensajeDto): RespuestaEnvioDto
}

class MensajeriaSimulada : MensajeriaService {

    // falla si falta el dato que pide el canal
    fun decidirEstado(mensaje: MensajeDto): EstadoEnvio {
        val tieneElDato = when (mensaje.canal) {
            CanalDespacho.EMAIL -> mensaje.correo.isNotBlank()
            CanalDespacho.SMS, CanalDespacho.WHATSAPP -> mensaje.telefono.isNotBlank()
        }
        return if (tieneElDato) EstadoEnvio.DESPACHADO else EstadoEnvio.FALLIDO
    }

    override suspend fun enviar(mensaje: MensajeDto): RespuestaEnvioDto = RespuestaEnvioDto(decidirEstado(mensaje))
}
