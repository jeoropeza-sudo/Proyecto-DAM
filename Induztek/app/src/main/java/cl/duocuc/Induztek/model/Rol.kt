package cl.duocuc.Induztek.model

data class Rol(
    val idRol: Long,
    val codigo: String,
    val nombre: String
) {
    companion object {
        const val TECNICO = "TECNICO"
        const val INGENIERO = "INGENIERO"
        const val JEFE_TECNICO = "JEFE_TECNICO"
        const val CLIENTE = "CLIENTE"
    }
}