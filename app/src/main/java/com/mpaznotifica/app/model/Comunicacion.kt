package com.mpaznotifica.app.model

enum class TipoComunicacion(val etiqueta: String) {
    INSTITUCIONAL("Institucional"),
    PEDAGOGICA("Pedagógica"),
    DISCIPLINARIA("Disciplinaria"),
    CITACION("Citación"),
    EVENTO("Evento"),
    TALLER("Taller"),
    SALIDA_PEDAGOGICA("Salida pedagógica")
}

enum class AlcanceEnvio(val etiqueta: String) {
    APODERADO_INDIVIDUAL("Apoderado individual"),
    CURSO("Curso"),
    TALLER("Taller"),
    COLEGIO_COMPLETO("Colegio completo")
}

enum class EstadoEnvio(val etiqueta: String) {
    DESPACHADO("Despachado"),
    FALLIDO("Fallido")
}

data class Comunicacion(
    val id: String,
    val asunto: String,
    val cuerpo: String,
    val tipo: TipoComunicacion,
    val alcance: AlcanceEnvio,
    val destinoReferencia: String? = null,
    val adjuntoPdfNombre: String? = null,
    val requiereFirmaFisica: Boolean = false,
    val autorId: String,
    val autorNombre: String,
    val fechaHora: String
)

data class EnvioDestinatario(
    val id: String,
    val comunicacionId: String,
    val apoderadoId: String,
    val canal: CanalDespacho,
    val estado: EstadoEnvio
)

data class ErroresComunicado(
    val asunto: String? = null,
    val cuerpo: String? = null,
    val tipo: String? = null,
    val alcance: String? = null,
    val destino: String? = null
) {
    val hayErrores: Boolean
        get() = listOf(asunto, cuerpo, tipo, alcance, destino).any { it != null }
}

data class ResultadoEnvio(
    val exito: Boolean,
    val mensaje: String
)
