package com.mpaznotifica.app.ui.screen.comunicados

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mpaznotifica.app.model.AlcanceEnvio
import com.mpaznotifica.app.model.ErroresComunicado
import com.mpaznotifica.app.model.TipoComunicacion
import com.mpaznotifica.app.recursosnativos.ArchivosPdf
import com.mpaznotifica.app.recursosnativos.Vibracion
import com.mpaznotifica.app.ui.components.BarraSuperior
import com.mpaznotifica.app.ui.components.SelectorDesplegable
import com.mpaznotifica.app.viewmodel.MpazNotificaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RedactarComunicadoScreen(navController: NavHostController, viewModel: MpazNotificaViewModel) {
    val usuario by viewModel.usuarioActivo.collectAsState()
    val apoderados by viewModel.apoderados.collectAsState()
    val enviando by viewModel.enviando.collectAsState()
    val resultado by viewModel.resultadoEnvio.collectAsState()
    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()

    var asunto by remember { mutableStateOf("") }
    var cuerpo by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf<TipoComunicacion?>(null) }
    var alcance by remember { mutableStateOf<AlcanceEnvio?>(null) }
    var destino by remember { mutableStateOf<String?>(null) }
    var adjunto by remember { mutableStateOf<String?>(null) }
    var errorAdjunto by remember { mutableStateOf<String?>(null) }
    var requiereFirma by remember { mutableStateOf(false) }
    var errores by remember { mutableStateOf(ErroresComunicado()) }
    var confirmando by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.limpiarResultadoEnvio() }

    // vibra cuando llega el resultado del envio
    LaunchedEffect(resultado) {
        if (resultado != null) Vibracion.confirmar(contexto)
    }

    val destinos = remember(alcance, usuario, apoderados) { viewModel.destinosPara(alcance, usuario) }

    // selector de archivos del sistema, solo pdf
    val selectorPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            if (!ArchivosPdf.esPdf(contexto, uri)) {
                errorAdjunto = "El archivo debe ser un PDF"
            } else {
                scope.launch {
                    // la copia se hace fuera del hilo principal
                    val nombre = withContext(Dispatchers.IO) { ArchivosPdf.guardarCopia(contexto, uri) }
                    if (nombre != null) {
                        adjunto = nombre
                        errorAdjunto = null
                    } else {
                        errorAdjunto = "No se pudo leer el archivo seleccionado"
                    }
                }
            }
        }
    }

    Scaffold(topBar = { BarraSuperior("Redactar comunicado", navController) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = asunto,
                onValueChange = {
                    asunto = it
                    errores = errores.copy(asunto = null)
                },
                label = { Text("Asunto") },
                singleLine = true,
                isError = errores.asunto != null,
                trailingIcon = if (errores.asunto != null) {
                    { Icon(Icons.Filled.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error) }
                } else {
                    null
                },
                supportingText = if (errores.asunto != null) {
                    { Text(errores.asunto.orEmpty()) }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = cuerpo,
                onValueChange = {
                    cuerpo = it
                    errores = errores.copy(cuerpo = null)
                },
                label = { Text("Cuerpo del mensaje") },
                minLines = 4,
                isError = errores.cuerpo != null,
                trailingIcon = if (errores.cuerpo != null) {
                    { Icon(Icons.Filled.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error) }
                } else {
                    null
                },
                supportingText = if (errores.cuerpo != null) {
                    { Text(errores.cuerpo.orEmpty()) }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth()
            )

            SelectorDesplegable(
                etiqueta = "Tipo de comunicación",
                opciones = viewModel.tiposPermitidos(usuario),
                seleccion = tipo,
                textoDe = { it.etiqueta },
                onSeleccion = {
                    tipo = it
                    errores = errores.copy(tipo = null)
                },
                error = errores.tipo
            )
            SelectorDesplegable(
                etiqueta = "Alcance (destinatarios)",
                opciones = viewModel.alcancesPermitidos(usuario),
                seleccion = alcance,
                textoDe = { it.etiqueta },
                onSeleccion = {
                    alcance = it
                    destino = null
                    errores = errores.copy(alcance = null, destino = null)
                },
                error = errores.alcance
            )
            if (alcance != null && alcance != AlcanceEnvio.COLEGIO_COMPLETO) {
                SelectorDesplegable(
                    etiqueta = "Destino",
                    opciones = destinos,
                    seleccion = destino,
                    textoDe = { it },
                    onSeleccion = {
                        destino = it
                        errores = errores.copy(destino = null)
                    },
                    error = errores.destino
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { selectorPdf.launch(arrayOf(ArchivosPdf.TIPO_PDF)) }) {
                    Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(if (adjunto == null) "Adjuntar PDF (opcional)" else "Cambiar PDF")
                }
                AnimatedVisibility(visible = adjunto != null) {
                    TextButton(onClick = { adjunto = null }) { Text("Quitar") }
                }
            }
            AnimatedVisibility(visible = adjunto != null) {
                Text("Adjunto: ${adjunto.orEmpty()}", style = MaterialTheme.typography.bodySmall)
            }
            errorAdjunto?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = requiereFirma, onCheckedChange = { requiereFirma = it })
                Text("Es una autorización que requiere firma física del apoderado")
            }
            AnimatedVisibility(visible = requiereFirma) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Rol solo informativo: la app notifica y avisa que la autorización debe firmarse en papel. " +
                            "No se capturan firmas en la app.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                enabled = !enviando,
                onClick = {
                    // la validacion esta en el viewmodel
                    errores = viewModel.validarComunicado(asunto, cuerpo, tipo, alcance, destino)
                    if (!errores.hayErrores && tipo != null && alcance != null) confirmando = true
                }
            ) { Text("Enviar") }

            AnimatedVisibility(visible = enviando) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Enviando comunicado…")
                }
            }
        }
    }

    // confirmacion antes de enviar
    if (confirmando) {
        AlertDialog(
            onDismissRequest = { confirmando = false },
            title = { Text("¿Enviar comunicado?") },
            text = { Text("Una vez enviado no se puede editar: queda registrado en el historial.") },
            confirmButton = {
                TextButton(onClick = {
                    val tipoElegido = tipo
                    val alcanceElegido = alcance
                    if (tipoElegido != null && alcanceElegido != null) {
                        viewModel.enviarComunicado(
                            asunto = asunto.trim(),
                            cuerpo = cuerpo.trim(),
                            tipo = tipoElegido,
                            alcance = alcanceElegido,
                            destino = destino,
                            adjuntoPdfNombre = adjunto,
                            requiereFirmaFisica = requiereFirma
                        )
                    }
                    confirmando = false
                }) { Text("Enviar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmando = false }) { Text("Cancelar") }
            }
        )
    }

    // alerta con el resultado, si salio bien vuelve al panel
    resultado?.let { r ->
        AlertDialog(
            onDismissRequest = { },
            title = { Text(if (r.exito) "Comunicado enviado" else "No se pudo enviar") },
            text = { Text(r.mensaje) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.limpiarResultadoEnvio()
                    if (r.exito) navController.popBackStack()
                }) { Text("Aceptar") }
            }
        )
    }
}
