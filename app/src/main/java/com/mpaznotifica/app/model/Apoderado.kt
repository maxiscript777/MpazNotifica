package com.mpaznotifica.app.model

enum class CanalDespacho(val etiqueta: String) {
    EMAIL("Correo electrónico"),
    SMS("SMS"),
    WHATSAPP("WhatsApp")
}

data class Apoderado(
    val id: String,
    val nombreCompleto: String,
    val correo: String,
    val telefono: String,
    val canalPreferente: CanalDespacho = CanalDespacho.EMAIL,
    val pupilosIds: List<String> = emptyList()
)
