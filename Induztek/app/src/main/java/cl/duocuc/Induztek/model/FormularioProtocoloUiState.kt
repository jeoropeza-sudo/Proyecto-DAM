package cl.duocuc.Induztek.model
enum class TipoEquipoOption(val etiqueta: String) {
    TRANSFORMADOR("Transformador"),
    CELDA_MT("Celda MT"),
    INTERRUPTOR("Interruptor"),
    CABLE_PODER("Cable de poder"),
    RELE_PROTECCION("Relé de protección")
}

enum class DevanadoOption(val etiqueta: String) {
    AT_T("AT - T (Alta Tensión a Tierra)"),
    AT_BT("AT - BT (Alta a Baja Tensión)"),
    BT_T("BT - T (Baja Tensión a Tierra)")
}

data class FormularioProtocoloUiState(
    // Identificación y Selección
    val tagEquipo: String = "TR-01",
    val tagsDisponibles: List<String> = listOf("TR-01", "TR-02", "TR-A", "CEL-MT-03", "INT-52-1"),
    val tipoEquipo: TipoEquipoOption = TipoEquipoOption.TRANSFORMADOR,
    val devanadoSeleccionado: DevanadoOption = DevanadoOption.AT_T,
    val tensionPruebaKv: String = "5.0",
    val tensionesDisponibles: List<String> = listOf("1.0", "2.5", "5.0", "10.0"),

    // Valores de Medición en Terreno
    val lectura1Min: String = "",
    val lectura10Min: String = "",
    val observaciones: String = "",

    // Valores Calculados e Indicadores
    val ipCalculado: Double = 0.0,
    val resultadoEvaluado: String = "PENDIENTE", // APROBADO, OBSERVADO, RECHAZADO
    val variacionDetectada: Boolean = false,

    // Manejo de Errores e Indicadores de Estado
    val errorLectura1Min: String? = null,
    val errorLectura10Min: String? = null,
    val errorObservaciones: String? = null,
    val registroExitoso: Boolean = false
)