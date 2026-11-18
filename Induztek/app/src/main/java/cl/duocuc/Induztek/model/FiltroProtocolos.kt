package cl.duocuc.Induztek.model

import java.text.Normalizer
import java.util.Locale

data class ProtocoloIndexado(
    val protocolo: ProtocoloTerreno,
    val cliente: String,
    val equipo: String,
    val fechas: List<String>,
    val fechaVisible: String,
    val estado: EstadoProtocolo
)

object FiltroProtocolos {

    fun indexar(protocolos: List<ProtocoloTerreno>): List<ProtocoloIndexado> =
        protocolos.map { protocolo ->
            val fecha = protocolo.fechaHora.ifBlank { protocolo.fechaRegistro }.take(10)
            val fechas = buildList {
                add(normalizarFecha(fecha))
                if (fecha.length >= 10 && fecha[4] == '-' && fecha[7] == '-') {
                    add(normalizarFecha("${fecha.substring(8, 10)}/${fecha.substring(5, 7)}/${fecha.substring(0, 4)}"))
                }
            }
            ProtocoloIndexado(
                protocolo = protocolo,
                cliente = normalizar(protocolo.clienteNombre),
                equipo = normalizar(
                    listOf(
                        protocolo.tagEquipo,
                        protocolo.tipoEquipo,
                        protocolo.marcaEquipo,
                        protocolo.modeloEquipo,
                        protocolo.numeroSerieEquipo,
                        protocolo.numeroProtocolo
                    ).joinToString(" ")
                ),
                fechas = fechas,
                fechaVisible = fecha,
                estado = EstadoProtocolo.desdeCodigo(protocolo.estado)
            )
        }

    fun opciones(protocolos: List<ProtocoloIndexado>): List<String> =
        buildList {
            add("Estado: Pendiente")
            addAll(
                EstadoProtocolo.entries
                    .filter { it != EstadoProtocolo.BORRADOR && it != EstadoProtocolo.ANULADO }
                    .map { "Estado: ${it.etiquetaTecnica}" }
            )
            protocolos.forEach { item ->
                if (item.cliente.isNotBlank()) add("Cliente: ${item.protocolo.clienteNombre}")
                val equipo = listOf(
                    item.protocolo.tagEquipo,
                    item.protocolo.tipoEquipo,
                    item.protocolo.marcaEquipo,
                    item.protocolo.modeloEquipo,
                    item.protocolo.numeroSerieEquipo
                ).filter(String::isNotBlank).joinToString(" · ")
                if (equipo.isNotBlank()) add("Equipo/TAG: $equipo")
                if (item.fechaVisible.isNotBlank()) add("Fecha: ${item.fechaVisible}")
            }
        }.distinct()

    fun sugerencias(protocolos: List<ProtocoloIndexado>, consulta: String): List<String> {
        return filtrarOpciones(opciones(protocolos), consulta).take(8)
    }

    fun filtrarOpciones(opciones: List<String>, consulta: String): List<String> {
        val consultaNormalizada = normalizar(consulta)
        return opciones.asSequence()
            .filter { consultaNormalizada.isBlank() || normalizar(it).contains(consultaNormalizada) }
            .toList()
    }

    fun aplicarSeleccion(
        protocolos: List<ProtocoloIndexado>,
        seleccion: Set<String>,
        soloVisiblesCliente: Boolean = false
    ): List<ProtocoloTerreno> {
        val filtrosPorTipo = seleccion
            .mapNotNull { opcion ->
                val (tipo, valor) = opcion.split(':', limit = 2)
                tipo.lowercase(Locale.ROOT) to valor.trim()
            }
            .groupBy({ it.first }, { it.second })

        return protocolos.filter { item ->
            if (soloVisiblesCliente &&
                (item.estado == EstadoProtocolo.BORRADOR ||
                        item.estado == EstadoProtocolo.ANULADO)
            ) {
                return@filter false
            }
            filtrosPorTipo.all { (tipo, valores) ->
                valores.any { valor -> coincideFiltro(item, tipo, valor) }
            }
        }.map { it.protocolo }
    }

    fun aplicar(
        protocolos: List<ProtocoloIndexado>,
        consulta: String,
        soloVisiblesCliente: Boolean = false
    ): List<ProtocoloTerreno> {
        val filtro = consulta.trim()
        val filtroNormalizado = normalizar(filtro)
        val (tipo, valor) = when {
            filtroNormalizado.startsWith("cliente:") ->
                "cliente" to filtro.substringAfter(':').trim()
            filtroNormalizado.startsWith("equipo/tag:") ->
                "equipo" to filtro.substringAfter(':').trim()
            filtroNormalizado.startsWith("fecha:") ->
                "fecha" to filtro.substringAfter(':').trim()
            filtroNormalizado.startsWith("estado:") ->
                "estado" to filtro.substringAfter(':').trim()
            else -> "" to filtro
        }
        val valorNormalizado = normalizar(valor)
        val fechaQuery = normalizarFecha(valor)
        val estadoQuery = normalizar(valor).uppercase(Locale.ROOT)

        return buildList {
            protocolos.forEach { item ->
                if (soloVisiblesCliente &&
                    (item.estado == EstadoProtocolo.BORRADOR ||
                            item.estado == EstadoProtocolo.ANULADO)
                ) return@forEach
                val coincide = when (tipo) {
                    "cliente" -> item.cliente.contains(valorNormalizado)
                    "equipo" -> item.equipo.contains(valorNormalizado)
                    "fecha" -> item.fechas.any { it.contains(fechaQuery) }
                    "estado" -> coincideEstado(item.estado, estadoQuery)
                    else -> valorNormalizado.isBlank() ||
                            item.cliente.contains(valorNormalizado) ||
                            item.equipo.contains(valorNormalizado) ||
                            item.fechas.any { it.contains(fechaQuery) } ||
                            item.estado.etiquetaTecnica.let { normalizar(it).contains(valorNormalizado) } ||
                            item.estado.codigo.let { normalizar(it).contains(valorNormalizado) } ||
                            (valorNormalizado == "pendiente" &&
                                    (item.estado == EstadoProtocolo.REGISTRADO_TERRENO ||
                                            item.estado == EstadoProtocolo.OBSERVADO))
                }
                if (coincide) add(item.protocolo)
            }
        }
    }

    private fun coincideFiltro(item: ProtocoloIndexado, tipo: String, valor: String): Boolean {
        val valorNormalizado = normalizar(valor)
        return when (tipo) {
            "cliente" -> item.cliente.contains(valorNormalizado)
            "equipo/tag" -> valor.split('·')
                .map(::normalizar)
                .filter(String::isNotBlank)
                .all { item.equipo.contains(it) }
            "fecha" -> item.fechas.any { it.contains(normalizarFecha(valor)) }
            "estado" -> coincideEstado(
                item.estado,
                valorNormalizado.uppercase(Locale.ROOT)
            )
            else -> false
        }
    }

    private fun coincideEstado(estado: EstadoProtocolo, query: String): Boolean = when (query) {
        "", "TODOS", "ALL" -> true
        "PENDIENTE" -> estado == EstadoProtocolo.REGISTRADO_TERRENO ||
                estado == EstadoProtocolo.OBSERVADO
        else -> estado.codigo == query ||
                normalizar(estado.etiquetaTecnica) == normalizar(query)
    }

    private fun normalizar(valor: String): String =
        Normalizer.normalize(valor.trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase(Locale.ROOT)
            .replace(Regex("\\s+"), " ")

    private fun normalizarFecha(valor: String): String =
        normalizar(valor).filter(Char::isLetterOrDigit)
}
