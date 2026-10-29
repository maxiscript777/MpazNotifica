package com.mpaznotifica.app.recursosnativos

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

object ArchivosPdf {

    const val TIPO_PDF = "application/pdf"

    fun esPdf(context: Context, uri: Uri): Boolean = context.contentResolver.getType(uri) == TIPO_PDF

    private fun nombreDe(context: Context, uri: Uri): String {
        var nombre: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val columna = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (columna >= 0 && cursor.moveToFirst()) nombre = cursor.getString(columna)
        }
        return nombre ?: "documento.pdf"
    }

    fun guardarCopia(context: Context, uri: Uri): String? = try {
        val nombre = File(nombreDe(context, uri)).name
        val carpeta = File(context.filesDir, "adjuntos").apply { mkdirs() }
        val entrada = context.contentResolver.openInputStream(uri)
        if (entrada == null) {
            null
        } else {
            entrada.use { origen -> File(carpeta, nombre).outputStream().use { destino -> origen.copyTo(destino) } }
            nombre
        }
    } catch (e: Exception) {
        null
    }
}
