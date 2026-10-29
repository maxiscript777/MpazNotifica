package com.mpaznotifica.app.ui.screen.comunicados

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.EnvioDestinatario
import com.mpaznotifica.app.model.EstadoEnvio
import com.mpaznotifica.app.ui.components.BarraSuperior
import com.mpaznotifica.app.viewmodel.MpazNotificaViewModel

@Composable
fun DetalleComunicadoScreen(navController: NavHostController, viewModel: MpazNotificaViewModel) {
    val seleccionadoId by viewModel.comunicadoSeleccionadoId.collectAsState()
    val comunicados by viewModel.comunicados.collectAsState()
    val todosLosEnvios by viewModel.envios.collectAsState()
    val apoderados by viewModel.apoderados.collectAsState()
    val comunicado = comunicados.find { it.id == seleccionadoId }

    Scaffold(topBar = { BarraSuperior("Detalle del comunicado", navController) }) { padding ->
        if (comunicado == null) {
            Text("Comunicado no encontrado.", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }
        val envios = remember(todosLosEnvios, comunicado.id) { viewModel.enviosDe(comunicado.id) }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(comunicado.asunto, style = MaterialTheme.typography.titleLarge)
                    Text(comunicado.cuerpo, style = MaterialTheme.typography.bodyMedium)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Tipo: ${comunicado.tipo.etiqueta}")
                    Text("Alcance: ${comunicado.alcance.etiqueta}" + (comunicado.destinoReferencia?.let { " ($it)" } ?: ""))
                    Text("Autor: ${comunicado.autorNombre}")
                    Text("Fecha y hora: ${comunicado.fechaHora}")
                    Text("Adjunto: ${comunicado.adjuntoPdfNombre ?: "sin adjunto"}")
                    if (comunicado.requiereFirmaFisica) {
                        Text(
                            "Autorización con firma física: la firma se entrega en papel.",
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            val despachados = envios.count { it.estado == EstadoEnvio.DESPACHADO }
            Text("Destinatarios y estado del envío (${envios.size})", style = MaterialTheme.typography.titleSmall)
            if (envios.isNotEmpty()) {
                Text(
                    "$despachados despachado(s) · ${envios.size - despachados} fallido(s)",
                    style = MaterialTheme.typography.labelMedium
                )
            } else {
                Text("Sin registros de envío.", style = MaterialTheme.typography.bodySmall)
            }
            envios.forEach { envio ->
                FilaEnvio(envio = envio, apoderado = apoderados.find { it.id == envio.apoderadoId })
            }
        }
    }
}

@Composable
private fun FilaEnvio(envio: EnvioDestinatario, apoderado: Apoderado?) {
    val fallido = envio.estado == EstadoEnvio.FALLIDO
    val colorEstado = if (fallido) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (fallido) Icons.Filled.Error else Icons.Filled.CheckCircle,
                contentDescription = envio.estado.etiqueta,
                tint = colorEstado
            )
            Column {
                Text(apoderado?.nombreCompleto ?: envio.apoderadoId, style = MaterialTheme.typography.titleSmall)
                Text("${envio.canal.etiqueta} · ${envio.estado.etiqueta}", color = colorEstado)
            }
        }
    }
}
