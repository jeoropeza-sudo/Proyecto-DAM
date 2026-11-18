package cl.duocuc.Induztek.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FiltroProtocolosTest {

    private val protocolos = listOf(
        ProtocoloTerreno(
            idProtocolo = 1L,
            numeroProtocolo = "PT-2026-001",
            tagEquipo = "TR-01",
            tipoEquipo = "Transformador",
            marcaEquipo = "Rhona",
            clienteNombre = "Compañía Eléctrica",
            fechaHora = "2026-11-18 10:30:00",
            estado = "FORMALIZADO"
        ),
        ProtocoloTerreno(
            idProtocolo = 2L,
            numeroProtocolo = "PT-2026-002",
            tagEquipo = "TR-02",
            tipoEquipo = "Transformador",
            marcaEquipo = "ABB",
            clienteNombre = "Compañía Eléctrica",
            fechaHora = "2026-11-17 10:30:00",
            estado = "OBSERVADO"
        ),
        ProtocoloTerreno(
            idProtocolo = 3L,
            numeroProtocolo = "PT-2026-003",
            tagEquipo = "TR-03",
            clienteNombre = "Otra empresa",
            fechaHora = "2026-11-18 10:30:00",
            estado = "ANULADO"
        )
    )

    @Test
    fun filtraPorClienteSinSensibilidadAAcentos() {
        val resultado = FiltroProtocolos.aplicar(
            protocolos = FiltroProtocolos.indexar(protocolos),
            consulta = "Cliente: compania electrica"
        )

        assertEquals(listOf(1L, 2L), resultado.map { it.idProtocolo })
    }

    @Test
    fun filtraPorEquipoYTagConUnaConsulta() {
        val resultado = FiltroProtocolos.aplicar(
            protocolos = FiltroProtocolos.indexar(protocolos),
            consulta = "Equipo/TAG: TR-01"
        )

        assertEquals(listOf(1L), resultado.map { it.idProtocolo })
    }

    @Test
    fun admiteFechaIsoYFiltraPendientesDelCliente() {
        val resultado = FiltroProtocolos.aplicar(
            protocolos = FiltroProtocolos.indexar(protocolos),
            consulta = "2026-11-17",
            soloVisiblesCliente = true
        )

        assertEquals(listOf(2L), resultado.map { it.idProtocolo })
    }

    @Test
    fun clienteNoVeProtocolosAnuladosOBorradores() {
        val resultado = FiltroProtocolos.aplicar(
            protocolos = FiltroProtocolos.indexar(protocolos),
            consulta = "Estado: Todos",
            soloVisiblesCliente = true
        )

        assertEquals(listOf(1L, 2L), resultado.map { it.idProtocolo })
    }

    @Test
    fun filtraEstadoPendienteElegidoEnCombo() {
        val resultado = FiltroProtocolos.aplicar(
            protocolos = FiltroProtocolos.indexar(protocolos),
            consulta = "Estado: Pendiente",
            soloVisiblesCliente = true
        )

        assertEquals(listOf(2L), resultado.map { it.idProtocolo })
    }

    @Test
    fun sugerenciasEncuentranMarcaDeEquipo() {
        val sugerencias = FiltroProtocolos.sugerencias(
            FiltroProtocolos.indexar(protocolos),
            "ABB"
        )

        assertEquals(true, sugerencias.any { it.contains("ABB") })
    }

    @Test
    fun aplicaFiltrosDeDistintasCategoriasEnConjunto() {
        val resultado = FiltroProtocolos.aplicarSeleccion(
            protocolos = FiltroProtocolos.indexar(protocolos),
            seleccion = setOf(
                "Cliente: Compañía Eléctrica",
                "Fecha: 2026-11-18"
            )
        )

        assertEquals(listOf(1L), resultado.map { it.idProtocolo })
    }

    @Test
    fun aceptaMultiplesValoresDeEstadoComoAlternativas() {
        val resultado = FiltroProtocolos.aplicarSeleccion(
            protocolos = FiltroProtocolos.indexar(protocolos),
            seleccion = setOf("Estado: Formalizado", "Estado: Observado"),
            soloVisiblesCliente = true
        )

        assertEquals(listOf(1L, 2L), resultado.map { it.idProtocolo })
    }

    @Test
    fun seleccionaEquipoConDatosDescriptivosDelTAG() {
        val resultado = FiltroProtocolos.aplicarSeleccion(
            protocolos = FiltroProtocolos.indexar(protocolos),
            seleccion = setOf("Equipo/TAG: TR-01 · Transformador · Rhona")
        )

        assertEquals(listOf(1L), resultado.map { it.idProtocolo })
    }
}
