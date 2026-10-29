package com.mpaznotifica.app.ui.screen.apoderados

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mpaznotifica.app.ui.components.BarraSuperior
import com.mpaznotifica.app.ui.components.SelectorDesplegable
import com.mpaznotifica.app.viewmodel.MpazNotificaViewModel
import kotlinx.coroutines.launch

@Composable
fun ApoderadosScreen(navController: NavHostController, viewModel: MpazNotificaViewModel) {
    val apoderados by viewModel.apoderados.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { BarraSuperior("Apoderados", navController) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(apoderados, key = { it.id }) { apoderado ->
                var canal by remember(apoderado.id, apoderado.canalPreferente) { mutableStateOf(apoderado.canalPreferente) }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(apoderado.nombreCompleto, style = MaterialTheme.typography.titleSmall)
                        Text("${apoderado.correo} · ${apoderado.telefono}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Pupilos: " + viewModel.nombresPupilos(apoderado).joinToString(", ").ifBlank { "sin pupilos" },
                            style = MaterialTheme.typography.bodySmall
                        )
                        SelectorDesplegable(
                            etiqueta = "Canal preferente",
                            opciones = viewModel.canalesDisponibles(apoderado),
                            seleccion = canal,
                            textoDe = { it.etiqueta },
                            onSeleccion = { canal = it }
                        )
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = canal != apoderado.canalPreferente,
                            onClick = {
                                val guardado = viewModel.actualizarCanalPreferente(apoderado.id, canal)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (guardado) {
                                            "Canal de ${apoderado.nombreCompleto} actualizado a ${canal.etiqueta}"
                                        } else {
                                            "No se pudo guardar: falta el correo o el teléfono del apoderado"
                                        }
                                    )
                                }
                            }
                        ) { Text("Guardar canal") }
                    }
                }
            }
        }
    }
}
