package cl.duocuc.Induztek.model

data class ProtocoloTerreno(
    val idProtocolo: Long = 0,
    val numeroProtocolo: String = "",
    val tagEquipo: String = "",
    val tipoEquipo: String = "",
    val clienteNombre: String = "",
    val instalacionNombre: String = "",
    val tecnicoNombre: String = "",
    val fechaHora: String = "",
    val temperaturaC: Double = 0.0,
    val humedadPct: Double = 0.0,
    val estado: String = "",
    val resultadoGlobal: String = ""
)

data class PruebaMedicion(
    val idPrueba: Long = 0,
    val elemento: String = "AT-T",
    val lectura1Min: Double = 0.0,
    val lectura10Min: Double = 0.0,
    val ip: Double = 0.0,
    val resultado: String = ""
)