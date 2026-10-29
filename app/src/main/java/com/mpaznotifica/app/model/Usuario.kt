package com.mpaznotifica.app.model

data class Usuario(
    val id: String,
    val nombre: String,
    val usuario: String,
    val clave: String,
    val perfil: Perfil,
    val cursoAsignado: String? = null
)
