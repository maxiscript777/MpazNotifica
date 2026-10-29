package com.mpaznotifica.app

import com.mpaznotifica.app.data.MockData
import com.mpaznotifica.app.data.remote.MensajeDto
import com.mpaznotifica.app.data.remote.MensajeriaSimulada
import com.mpaznotifica.app.model.AlcanceEnvio
import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.CanalDespacho
import com.mpaznotifica.app.model.EstadoEnvio
import com.mpaznotifica.app.model.Estudiante
import com.mpaznotifica.app.model.TipoComunicacion
import com.mpaznotifica.app.reglas.DatosReferencia
import com.mpaznotifica.app.reglas.ReglasComunicado
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReglasComunicadoTest {

    private val inspectoria = MockData.usuarios.first { it.id == "U1" }
    private val profesor = MockData.usuarios.first { it.id == "U2" }
    private val datos = DatosReferencia(MockData.cursos, MockData.talleres, MockData.estudiantes, MockData.apoderados)

    private fun validar(
        usuario: com.mpaznotifica.app.model.Usuario?,
        tipo: TipoComunicacion? = TipoComunicacion.PEDAGOGICA,
        alcance: AlcanceEnvio? = AlcanceEnvio.CURSO,
        destino: String? = "4° Básico A",
        asunto: String = "Asunto",
        cuerpo: String = "Cuerpo"
    ) = ReglasComunicado.validar(asunto, cuerpo, tipo, alcance, destino, usuario, datos)

    @Test fun rn01_profesor_no_ve_colegio_completo_ni_taller() {
        val alcances = ReglasComunicado.alcancesPermitidos(profesor)
        assertFalse(AlcanceEnvio.COLEGIO_COMPLETO in alcances)
        assertFalse(AlcanceEnvio.TALLER in alcances)
    }

    @Test fun rn01_profesor_solo_ve_su_curso_y_sus_apoderados() {
        assertEquals(listOf("4° Básico A"), ReglasComunicado.destinosPara(AlcanceEnvio.CURSO, profesor, datos))
        assertEquals(
            listOf("Apoderado Ficticio 1", "Apoderado Ficticio 2"),
            ReglasComunicado.destinosPara(AlcanceEnvio.APODERADO_INDIVIDUAL, profesor, datos)
        )
    }

    @Test fun rn01_profesor_con_destino_de_otro_curso_da_error_en_destino() {
        val e = validar(profesor, destino = "2° Básico A")
        assertEquals(ReglasComunicado.MSG_SOLO_CURSO, e.destino)
    }

    @Test fun rn01_validar_colegio_completo_siendo_profesor_da_error_en_alcance() {
        val e = validar(profesor, alcance = AlcanceEnvio.COLEGIO_COMPLETO, destino = null)
        assertEquals(ReglasComunicado.MSG_SOLO_CURSO, e.alcance)
    }

    @Test fun rn01_profesor_valido_a_su_curso_no_tiene_errores() {
        assertFalse(validar(profesor).hayErrores)
    }

    @Test fun rn03_profesor_no_puede_emitir_institucionales() {
        assertFalse(TipoComunicacion.INSTITUCIONAL in ReglasComunicado.tiposPermitidos(profesor))
    }

    @Test fun rn03_validar_tipo_institucional_siendo_profesor_da_error_en_tipo() {
        val e = validar(profesor, tipo = TipoComunicacion.INSTITUCIONAL)
        assertEquals(ReglasComunicado.MSG_TIPO_NO_DISPONIBLE, e.tipo)
    }

    @Test fun inspectoria_ve_los_cuatro_alcances_y_todos_los_tipos() {
        assertEquals(4, ReglasComunicado.alcancesPermitidos(inspectoria).size)
        assertEquals(TipoComunicacion.entries.size, ReglasComunicado.tiposPermitidos(inspectoria).size)
    }

    @Test fun sin_sesion_las_listas_quedan_vacias() {
        assertTrue(ReglasComunicado.alcancesPermitidos(null).isEmpty())
        assertTrue(ReglasComunicado.tiposPermitidos(null).isEmpty())
    }

    @Test fun formulario_vacio_marca_error_en_cada_campo() {
        val e = ReglasComunicado.validar("", "", null, null, null, inspectoria, datos)
        assertNotNull(e.asunto); assertNotNull(e.cuerpo); assertNotNull(e.tipo); assertNotNull(e.alcance)
    }

    @Test fun asunto_de_mas_de_100_caracteres_da_error() {
        assertNotNull(validar(inspectoria, asunto = "a".repeat(101)).asunto)
    }

    @Test fun alcance_curso_sin_destino_da_error_en_destino() {
        assertNotNull(validar(inspectoria, destino = null).destino)
    }

    @Test fun formulario_valido_no_tiene_errores() {
        assertFalse(validar(inspectoria, alcance = AlcanceEnvio.COLEGIO_COMPLETO, destino = null).hayErrores)
    }

    @Test fun curso_4A_resuelve_a_AP1_y_AP2() {
        val ids = ReglasComunicado.resolverApoderados(AlcanceEnvio.CURSO, "4° Básico A", datos).map { it.id }
        assertEquals(listOf("AP1", "AP2"), ids)
    }

    @Test fun taller_robotica_resuelve_solo_a_AP1() {
        val ids = ReglasComunicado.resolverApoderados(AlcanceEnvio.TALLER, "Taller de Robótica", datos).map { it.id }
        assertEquals(listOf("AP1"), ids)
    }

    @Test fun colegio_completo_resuelve_a_los_tres_apoderados() {
        assertEquals(3, ReglasComunicado.resolverApoderados(AlcanceEnvio.COLEGIO_COMPLETO, null, datos).size)
    }

    @Test fun rn07_apoderado_con_dos_pupilos_en_el_mismo_curso_aparece_una_vez() {
        val estudiantes = MockData.estudiantes + Estudiante("E4", "Estudiante Ficticio 4", "4° Básico A")
        val apoderados = listOf(
            Apoderado("AP9", "Apoderado Ficticio 9", "a9@ejemplo.cl", "+56900000009", CanalDespacho.EMAIL, listOf("E2", "E4"))
        )
        val d = DatosReferencia(MockData.cursos, MockData.talleres, estudiantes, apoderados)
        assertEquals(1, ReglasComunicado.resolverApoderados(AlcanceEnvio.CURSO, "4° Básico A", d).size)
    }

    @Test fun rn05_se_usa_el_canal_preferente() {
        val ap2 = MockData.apoderados.first { it.id == "AP2" }
        assertEquals(CanalDespacho.WHATSAPP, ReglasComunicado.canalDe(ap2, false))
    }

    @Test fun rn04_autorizacion_con_firma_va_por_correo_y_agrega_el_aviso() {
        val ap2 = MockData.apoderados.first { it.id == "AP2" }
        assertEquals(CanalDespacho.EMAIL, ReglasComunicado.canalDe(ap2, true))
        assertTrue(ReglasComunicado.cuerpoFinal("Hola", true).endsWith(ReglasComunicado.AVISO_FIRMA))
        assertEquals("Hola", ReglasComunicado.cuerpoFinal("Hola", false))
    }

    @Test fun simulador_falla_si_falta_el_dato_del_canal() {
        val sim = MensajeriaSimulada()
        assertEquals(EstadoEnvio.FALLIDO, sim.decidirEstado(MensajeDto("a", "b", CanalDespacho.SMS, "x@y.cl", "")))
        assertEquals(EstadoEnvio.FALLIDO, sim.decidirEstado(MensajeDto("a", "b", CanalDespacho.EMAIL, "", "+56900000001")))
        assertEquals(EstadoEnvio.DESPACHADO, sim.decidirEstado(MensajeDto("a", "b", CanalDespacho.WHATSAPP, "", "+56900000001")))
    }

    @Test fun no_se_puede_elegir_sms_si_el_apoderado_no_tiene_telefono() {
        val sinTel = Apoderado("AP8", "Apoderado Ficticio 8", "a8@ejemplo.cl", "", CanalDespacho.EMAIL, emptyList())
        assertEquals(listOf(CanalDespacho.EMAIL), ReglasComunicado.canalesDisponibles(sinTel))
    }

    @Test fun historial_viene_del_mas_reciente_al_mas_antiguo() {
        val ids = ReglasComunicado.filtrarHistorial(MockData.comunicados, null, null, null, null).map { it.id }
        assertEquals(listOf("C2", "C1"), ids)
    }

    @Test fun filtro_por_tipo_citacion_deja_solo_C1() {
        val ids = ReglasComunicado.filtrarHistorial(MockData.comunicados, TipoComunicacion.CITACION, null, null, null).map { it.id }
        assertEquals(listOf("C1"), ids)
    }

    @Test fun filtro_por_emisor_inspectoria_deja_solo_C2() {
        val ids = ReglasComunicado.filtrarHistorial(MockData.comunicados, null, "U1", null, null).map { it.id }
        assertEquals(listOf("C2"), ids)
    }

    @Test fun filtro_desde_2026_09_10_deja_solo_C2() {
        val ids = ReglasComunicado.filtrarHistorial(MockData.comunicados, null, null, "2026-09-10", null).map { it.id }
        assertEquals(listOf("C2"), ids)
    }

    @Test fun desde_mayor_que_hasta_no_filtra_por_fecha() {
        assertEquals(2, ReglasComunicado.filtrarHistorial(MockData.comunicados, null, null, "2026-09-20", "2026-09-01").size)
        assertEquals(ReglasComunicado.MSG_FECHAS_INVERTIDAS, ReglasComunicado.errorFechas("2026-09-20", "2026-09-01"))
    }

    @Test fun fecha_con_formato_invalido_se_ignora_y_da_aviso() {
        assertEquals(2, ReglasComunicado.filtrarHistorial(MockData.comunicados, null, null, "10/09/2026", null).size)
        assertEquals(ReglasComunicado.MSG_FORMATO_FECHA, ReglasComunicado.errorFechas("10/09/2026", null))
    }
}
