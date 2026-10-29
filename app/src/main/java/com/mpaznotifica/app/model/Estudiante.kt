package com.mpaznotifica.app.model

data class Estudiante(
    val id: String,
    val nombreReferencial: String,
    val curso: String,
    val talleresIds: List<String> = emptyList()
)

data class Taller(
    val id: String,
    val nombre: String
)
