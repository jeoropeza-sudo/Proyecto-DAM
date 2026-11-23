package cl.duocuc.Induztek.model

data class CrearCuentaResultado(
    val aceptada: Boolean = false,
    val campoError: String? = null,
    val mensajeError: String? = null
) {
    val exitosa: Boolean
        get() = aceptada
}
