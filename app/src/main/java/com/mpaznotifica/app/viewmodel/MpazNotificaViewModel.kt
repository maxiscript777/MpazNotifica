package com.mpaznotifica.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mpaznotifica.app.data.MockData
import com.mpaznotifica.app.data.MpazRepositorio
import com.mpaznotifica.app.data.local.AppDatabase
import com.mpaznotifica.app.data.remote.MensajeDto
import com.mpaznotifica.app.data.remote.MensajeriaService
import com.mpaznotifica.app.data.remote.MensajeriaSimulada
import com.mpaznotifica.app.model.AlcanceEnvio
import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.CanalDespacho
import com.mpaznotifica.app.model.Comunicacion
import com.mpaznotifica.app.model.EnvioDestinatario
import com.mpaznotifica.app.model.ErroresComunicado
import com.mpaznotifica.app.model.EstadoEnvio
import com.mpaznotifica.app.model.Estudiante
import com.mpaznotifica.app.model.ResultadoEnvio
import com.mpaznotifica.app.model.Taller
import com.mpaznotifica.app.model.TipoComunicacion
import com.mpaznotifica.app.model.Usuario
import com.mpaznotifica.app.reglas.DatosReferencia
import com.mpaznotifica.app.reglas.ReglasComunicado
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MpazNotificaViewModel(application: Application) : AndroidViewModel(application) {

    // aca se crea la base y el repositorio
    private val repositorio = MpazRepositorio(AppDatabase.obtener(application))
    // servicio simulado, no usa internet
    private val mensajeria: MensajeriaService = MensajeriaSimulada()

    init {
        // carga los datos de prueba solo la primera vez
        viewModelScope.launch { repositorio.cargarDatosIniciales() }
    }

    val usuarios: StateFlow<List<Usuario>> = repositorio.usuarios.aEstado(emptyList())
    val talleres: StateFlow<List<Taller>> = repositorio.talleres.aEstado(emptyList())
    val estudiantes: StateFlow<List<Estudiante>> = repositorio.estudiantes.aEstado(emptyList())
    val apoderados: StateFlow<List<Apoderado>> = repositorio.apoderados.aEstado(emptyList())
    val comunicados: StateFlow<List<Comunicacion>> = repositorio.comunicados.aEstado(emptyList())
    val envios: StateFlow<List<EnvioDestinatario>> = repositorio.envios.aEstado(emptyList())

    private fun <T> kotlinx.coroutines.flow.Flow<T>.aEstado(inicial: T): StateFlow<T> =
        // el Flow de room lo paso a StateFlow para leerlo en la pantalla con collectAsState
        stateIn(viewModelScope, SharingStarted.Eagerly, inicial)

    val cursos: List<String> = MockData.cursos
    val emisores: List<Usuario> get() = usuarios.value

    private fun datos() = DatosReferencia(cursos, talleres.value, estudiantes.value, apoderados.value)

    private val _usuarioActivo = MutableStateFlow<Usuario?>(null)
    val usuarioActivo: StateFlow<Usuario?> = _usuarioActivo.asStateFlow()

    private val _errorLogin = MutableStateFlow<String?>(null)
    val errorLogin: StateFlow<String?> = _errorLogin.asStateFlow()

    fun login(usuario: String, clave: String): Boolean {
        val correo = usuario.trim()
        val error = when {
            correo.isEmpty() -> "Ingresa tu correo institucional"
            !correo.contains("@") -> "El correo no es válido"
            clave.isEmpty() -> "Ingresa tu clave"
            else -> null
        }
        if (error != null) {
            _errorLogin.value = error
            return false
        }
        // aca se compara con los usuarios guardados en room
        val encontrado = usuarios.value.find { it.usuario.equals(correo, ignoreCase = true) && it.clave == clave }
        if (encontrado == null) {
            _errorLogin.value = "Correo o clave incorrectos"
            return false
        }
        _usuarioActivo.value = encontrado
        _errorLogin.value = null
        return true
    }

    fun logout() {
        _usuarioActivo.value = null
        _errorLogin.value = null
        _comunicadoSeleccionadoId.value = null
        _resultadoEnvio.value = null
    }

    fun nombresPupilos(apoderado: Apoderado): List<String> =
        estudiantes.value.filter { it.id in apoderado.pupilosIds }.map { "${it.nombreReferencial} (${it.curso})" }

    fun tiposPermitidos(usuario: Usuario?): List<TipoComunicacion> = ReglasComunicado.tiposPermitidos(usuario)

    fun alcancesPermitidos(usuario: Usuario?): List<AlcanceEnvio> = ReglasComunicado.alcancesPermitidos(usuario)

    fun destinosPara(alcance: AlcanceEnvio?, usuario: Usuario?): List<String> =
        ReglasComunicado.destinosPara(alcance, usuario, datos())

    fun validarComunicado(
        asunto: String,
        cuerpo: String,
        tipo: TipoComunicacion?,
        alcance: AlcanceEnvio?,
        destino: String?
    ): ErroresComunicado =
        ReglasComunicado.validar(asunto, cuerpo, tipo, alcance, destino, _usuarioActivo.value, datos())

    private val _enviando = MutableStateFlow(false)
    val enviando: StateFlow<Boolean> = _enviando.asStateFlow()

    private val _resultadoEnvio = MutableStateFlow<ResultadoEnvio?>(null)
    val resultadoEnvio: StateFlow<ResultadoEnvio?> = _resultadoEnvio.asStateFlow()

    fun limpiarResultadoEnvio() {
        _resultadoEnvio.value = null
    }

    fun enviarComunicado(
        asunto: String,
        cuerpo: String,
        tipo: TipoComunicacion,
        alcance: AlcanceEnvio,
        destino: String?,
        adjuntoPdfNombre: String?,
        requiereFirmaFisica: Boolean
    ) {
        // el autor siempre es el usuario con sesion
        val autor = _usuarioActivo.value
        if (autor == null) {
            _resultadoEnvio.value = ResultadoEnvio(false, ReglasComunicado.MSG_SIN_SESION)
            return
        }
        // valido de nuevo por si llaman la funcion sin pasar por la pantalla
        val errores = ReglasComunicado.validar(asunto, cuerpo, tipo, alcance, destino, autor, datos())
        if (errores.hayErrores) {
            _resultadoEnvio.value = ResultadoEnvio(false, ReglasComunicado.primerError(errores).orEmpty())
            return
        }
        // aca saco los apoderados sin repetir
        val destinatarios = ReglasComunicado.resolverApoderados(alcance, destino, datos())
        if (destinatarios.isEmpty()) {
            _resultadoEnvio.value = ResultadoEnvio(false, ReglasComunicado.MSG_SIN_DESTINATARIOS)
            return
        }

        // prende el progreso
        _enviando.value = true
        viewModelScope.launch {
            try {
                val ahora = Date()
                // fecha y hora las pone el sistema
                val fechaHora = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(ahora)
                val comunicacionId = "C${ahora.time}"
                val cuerpoFinal = ReglasComunicado.cuerpoFinal(cuerpo, requiereFirmaFisica)

                val comunicacion = Comunicacion(
                    id = comunicacionId, asunto = asunto, cuerpo = cuerpoFinal, tipo = tipo, alcance = alcance,
                    destinoReferencia = destino, adjuntoPdfNombre = adjuntoPdfNombre,
                    requiereFirmaFisica = requiereFirmaFisica,
                    autorId = autor.id, autorNombre = autor.nombre, fechaHora = fechaHora
                )

                // aca se arma un envio por apoderado (RF-08)
                val enviosNuevos = destinatarios.map { apoderado ->
                    val canal = ReglasComunicado.canalDe(apoderado, requiereFirmaFisica)
                    val estado = try {
                        mensajeria.enviar(MensajeDto(asunto, cuerpoFinal, canal, apoderado.correo, apoderado.telefono)).estado
                    } catch (e: CancellationException) {
                        // si cancelan la corrutina la vuelvo a lanzar, no la puedo dejar pasar
                        throw e
                    } catch (e: Exception) {
                        // si el servicio falla queda fallido y la app sigue
                        EstadoEnvio.FALLIDO
                    }
                    EnvioDestinatario("EV-$comunicacionId-${apoderado.id}", comunicacionId, apoderado.id, canal, estado)
                }

                // aca se guarda todo en room
                repositorio.registrarComunicado(comunicacion, enviosNuevos)
                val ok = enviosNuevos.count { it.estado == EstadoEnvio.DESPACHADO }
                _resultadoEnvio.value = ResultadoEnvio(
                    true,
                    "Se envió a ${enviosNuevos.size} apoderado(s): $ok despachado(s), ${enviosNuevos.size - ok} fallido(s)."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _resultadoEnvio.value = ResultadoEnvio(false, "No se pudo registrar el comunicado. Intenta nuevamente.")
            } finally {
                // se apaga siempre, salga bien o mal
                _enviando.value = false
            }
        }
    }

    private val _comunicadoSeleccionadoId = MutableStateFlow<String?>(null)
    val comunicadoSeleccionadoId: StateFlow<String?> = _comunicadoSeleccionadoId.asStateFlow()

    fun seleccionarComunicado(id: String?) {
        _comunicadoSeleccionadoId.value = id
    }

    fun apoderadoPorId(id: String): Apoderado? = apoderados.value.find { it.id == id }

    fun enviosDe(comunicacionId: String): List<EnvioDestinatario> =
        envios.value.filter { it.comunicacionId == comunicacionId }

    fun historialFiltrado(
        tipo: TipoComunicacion?,
        emisorId: String?,
        fechaDesde: String?,
        fechaHasta: String?
    ): List<Comunicacion> = ReglasComunicado.filtrarHistorial(comunicados.value, tipo, emisorId, fechaDesde, fechaHasta)

    fun errorFechasHistorial(desde: String, hasta: String): String? = ReglasComunicado.errorFechas(desde, hasta)

    fun resumenEnvios(comunicacionId: String): String {
        val lista = enviosDe(comunicacionId)
        val ok = lista.count { it.estado == EstadoEnvio.DESPACHADO }
        return "$ok despachado(s), ${lista.size - ok} fallido(s)"
    }

    fun canalesDisponibles(apoderado: Apoderado): List<CanalDespacho> = ReglasComunicado.canalesDisponibles(apoderado)

    fun actualizarCanalPreferente(apoderadoId: String, canal: CanalDespacho): Boolean {
        val apoderado = apoderadoPorId(apoderadoId) ?: return false
        if (canal !in ReglasComunicado.canalesDisponibles(apoderado)) return false
        viewModelScope.launch { repositorio.actualizarCanal(apoderadoId, canal) }
        return true
    }
}
