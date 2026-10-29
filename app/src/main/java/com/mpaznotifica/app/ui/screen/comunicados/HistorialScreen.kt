package com.mpaznotifica.app.ui.screen.comunicados

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mpaznotifica.app.model.TipoComunicacion
import com.mpaznotifica.app.model.Usuario
import com.mpaznotifica.app.navigation.Rutas
import com.mpaznotifica.app.ui.components.BarraSuperior
import com.mpaznotifica.app.ui.components.SelectorDesplegable
import com.mpaznotifica.app.viewmodel.MpazNotificaViewModel

@Composable
fun HistorialScreen(navController: NavHostController, viewModel: MpazNotificaViewModel) {
    val todos by viewModel.comunicados.collectAsState()
    val envios by viewModel.envios.collectAsState()
    var tipo by remember { mutableStateOf<TipoComunicacion?>(null) }
    var emisor by remember { mutableStateOf<Usuario?>(null) }
    var desde by remember { mutableStateOf("") }
    var hasta by remember { mutableStateOf("") }

    val lista = remember(todos, tipo, emisor, desde, hasta) {
        viewModel.historialFiltrado(tipo, emisor?.id, desde.ifBlank { null }, hasta.ifBlank { null })
    }
    val errorFechas = viewModel.errorFechasHistorial(desde, hasta)

    Scaffold(topBar = { BarraSuperior("Historial de comunicados", navController) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            SelectorDesplegable<TipoComunicacion?>(
                etiqueta = "Tipo",
                opciones = listOf<TipoComunicacion?>(null) + TipoComunicacion.entries,
                seleccion = tipo,
                textoDe = { it?.etiqueta ?: "Todos los tipos" },
                onSeleccion = { tipo = it },
                textoVacio = "Todos los tipos",
                modifier = Modifier.padding(top = 8.dp)
            )
            SelectorDesplegable<Usuario?>(
                etiqueta = "Emisor",
                opciones = listOf<Usuario?>(null) + viewModel.emisores,
                seleccion = emisor,
                textoDe = { it?.nombre ?: "Todos los emisores" },
                onSeleccion = { emisor = it },
                textoVacio = "Todos los emisores"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = desde,
                    onValueChange = { desde = it },
                    label = { Text("Desde (AAAA-MM-DD)") },
                    singleLine = true,
                    isError = errorFechas != null,
                    trailingIcon = if (errorFechas != null) {
                        { Icon(Icons.Filled.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error) }
                    } else {
                        null
                    },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = hasta,
                    onValueChange = { hasta = it },
                    label = { Text("Hasta (AAAA-MM-DD)") },
                    singleLine = true,
                    isError = errorFechas != null,
                    trailingIcon = if (errorFechas != null) {
                        { Icon(Icons.Filled.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error) }
                    } else {
                        null
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            AnimatedVisibility(visible = errorFechas != null) {
                Text(
                    errorFechas.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text(
                "${lista.size} comunicado(s)",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (todos.isEmpty()) {
                Text("Todavía no se han enviado comunicados")
            } else if (lista.isEmpty()) {
                Text("No hay comunicados que coincidan con los filtros")
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(lista, key = { it.id }) { comunicado ->
                    val resumen = remember(envios, comunicado.id) { viewModel.resumenEnvios(comunicado.id) }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.seleccionarComunicado(comunicado.id)
                                navController.navigate(Rutas.DETALLE)
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(comunicado.asunto, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${comunicado.tipo.etiqueta} · ${comunicado.alcance.etiqueta}" +
                                    (comunicado.destinoReferencia?.let { " · $it" } ?: ""),
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (comunicado.requiereFirmaFisica) {
                                Text(
                                    "Requiere firma física (rol informativo)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Text(resumen, style = MaterialTheme.typography.labelSmall)
                            Text("${comunicado.fechaHora} · ${comunicado.autorNombre}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
