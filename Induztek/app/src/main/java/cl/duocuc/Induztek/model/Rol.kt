package cl.duocuc.Induztek.model

data class Rol(
    val idRol: Long,
    val codigo: String,
    val nombre: String
) {
    companion object {
        const val ID_TECNICO = 1L
        const val ID_INGENIERO = 2L
        const val ID_JEFE_TECNICO = 3L
        const val ID_CLIENTE = 4L

        const val CODIGO_TECNICO = "TECNICO"
        const val CODIGO_INGENIERO = "INGENIERO"
        const val CODIGO_JEFE_TECNICO = "JEFE_TECNICO"
        const val CODIGO_CLIENTE = "CLIENTE"
    }

    fun esCliente(): Boolean = idRol == ID_CLIENTE || codigo.equals(CODIGO_CLIENTE, ignoreCase = true)
    fun esTecnico(): Boolean = idRol == ID_TECNICO || codigo.equals(CODIGO_TECNICO, ignoreCase = true)
    fun esIngeniero(): Boolean = idRol == ID_INGENIERO || codigo.equals(CODIGO_INGENIERO, ignoreCase = true)
    fun esJefeTecnico(): Boolean = idRol == ID_JEFE_TECNICO || codigo.equals(CODIGO_JEFE_TECNICO, ignoreCase = true)

    // Matriz de Permisos del Anexo 7
    fun puedeRegistrarEquipos(): Boolean = !esCliente()
    fun puedeCrearProtocolos(): Boolean = !esCliente()
    fun puedeCerrarVisita(): Boolean = esTecnico() || esIngeniero()
    fun puedeFormalizarUObservar(): Boolean = esIngeniero() || esJefeTecnico()
    fun puedeAnular(): Boolean = esIngeniero() || esJefeTecnico()
    fun puedeGestionarInstrumentos(): Boolean = esJefeTecnico()
    fun puedeOtorgarCuentasCliente(): Boolean = esJefeTecnico()
    fun puedeConsultarCualquierEquipo(): Boolean = !esCliente()
}