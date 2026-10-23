package cl.duocuc.Induztek.model

data class ProtocoloTerreno(
    val idProtocolo: Long = 0L,
    val numeroProtocolo: String = "",
    val idEquipo: Long = 0L,
    val tagEquipo: String = "",
    val tipoEquipo: String = "",
    val marcaEquipo: String = "",
    val modeloEquipo: String = "",
    val numeroSerieEquipo: String = "",
    val anoFabricacion: String = "",
    val potenciaKva: String = "",
    val tensiones: String = "",
    val posicionTap: String = "",
    val grupoConexion: String = "",
    val clienteNombre: String = "",
    val instalacionNombre: String = "",
    val tecnicoNombre: String = "",
    val fechaHora: String = "",
    val fechaRegistro: String = "",
    val temperaturaC: Double = 0.0,
    val humedadPct: Double = 0.0,
    val estado: String = "",
    val resultadoGlobal: String = "",
    val resultadoEvaluado: String = "",
    val observaciones: String? = null,

    // --- NUEVOS CAMPOS AÑADIDOS (RN-14, Aclaración 4, RF-08) ---
    val tipoVisita: String = "Solo pruebas",
    val descripcionMantenimiento: String? = null,
    val instrumentoUtilizado: String = "",
    val receptorNombre: String = "",
    val receptorRut: String = "",
    val receptorCargo: String = "",
    val numeroOt: String = "",
    val fechaRecepcion: String = "",
    val idVisita: Long = 0L,
    val fechaFormalizacion: String? = null,
    val usuarioFormalizaNombre: String? = null,
    val motivoObservacion: String? = null,

    // Mantenemos estos por compatibilidad con tu BD actual de SQLite
    val lectura1Min: Double = 0.0,
    val lectura10Min: Double = 0.0,

    // Lista para manejar múltiples devanados en la vista (requeriría TypeConverter si usas Room)
    val medicionesAislacion: List<FilaAislacion> = emptyList(),
    val mediciones: List<MedicionProtocolo> = emptyList(),
    val historialEstados: List<HistorialEstadoProtocolo> = emptyList()
)

data class HistorialEstadoProtocolo(
    val estadoAnterior: String?,
    val estadoNuevo: String,
    val usuarioNombre: String,
    val fechaHora: String,
    val motivo: String?
)

// Data class auxiliar para manejar las mediciones en la UI y calcular el IP automáticamente
data class FilaAislacion(
    val devanado: String = "",
    val lectura1Min: String = "",
    val lectura10Min: String = ""
) {
    // Calcula 10 min / 1 min. Si los valores no son números válidos, devuelve "-"
    val indicePolarizacionCalculado: String
        get() {
            val v1 = lectura1Min.toDoubleOrNull()
            val v10 = lectura10Min.toDoubleOrNull()
            return if (v1 != null && v10 != null && v1 > 0) {
                // Reemplazamos la coma por punto para evitar problemas regionales en el cálculo
                String.format(java.util.Locale.US, "%.2f", v10 / v1)
            } else {
                "-"
            }
        }
}