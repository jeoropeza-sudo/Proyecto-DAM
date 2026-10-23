package cl.duocuc.Induztek.model

data class Usuario(
    val idUsuario: Long,
    val rut: String,
    val nombre: String,
    val correo: String,
    val telefono: String,
    val rolCodigo: String,
    val idEmpresaCliente: Long? = null,
    val activo: Boolean = true,
    val avatar: ByteArray? = null
)