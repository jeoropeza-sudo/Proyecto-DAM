package cl.duocuc.Induztek.model

data class Usuario(
    val idUsuario: Long = 0,
    val rut: String,
    val nombre: String,
    val correo: String,
    val telefono: String? = null,
    val rolCodigo: String,
    val idEmpresaCliente: Long? = null,
    val activo: Boolean = true
)