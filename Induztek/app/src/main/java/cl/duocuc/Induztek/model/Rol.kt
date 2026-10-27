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

}