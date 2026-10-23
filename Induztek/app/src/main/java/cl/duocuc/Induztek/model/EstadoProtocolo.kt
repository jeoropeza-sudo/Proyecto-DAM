package cl.duocuc.Induztek.model

enum class EstadoProtocolo(val codigo: String, val etiquetaTecnica: String) {
    BORRADOR("BORRADOR", "Borrador"),
    REGISTRADO_TERRENO("REGISTRADO_TERRENO", "Registrado en Terreno"),
    OBSERVADO("OBSERVADO", "Observado por Ingeniería"),
    FORMALIZADO("FORMALIZADO", "Formalizado"),
    ANULADO("ANULADO", "Anulado");

    /**
     * RN-19: Enmascaramiento de estados para el perfil Usuario Cliente
     */
    fun obtenerEtiquetaCliente(): String {
        return when (this) {
            REGISTRADO_TERRENO, OBSERVADO -> "Pendiente de formalización"
            FORMALIZADO -> "Formalizado"
            else -> "" // Borrador y Anulado no son visibles para el cliente
        }
    }

    companion object {
        fun desdeCodigo(codigo: String?): EstadoProtocolo {
            return entries.find { it.codigo.equals(codigo, ignoreCase = true) } ?: BORRADOR
        }
    }
}