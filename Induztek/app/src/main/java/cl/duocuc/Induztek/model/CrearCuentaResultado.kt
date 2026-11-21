package cl.duocuc.Induztek.model

data class CrearCuentaResultado(
    val idUsuario: Long? = null,
    val contrasenaTemporal: String? = null,
    val campoError: String? = null,
    val mensajeError: String? = null
) {
    val exitosa: Boolean
        get() = idUsuario != null && contrasenaTemporal != null
}
