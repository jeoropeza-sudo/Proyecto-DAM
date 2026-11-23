package cl.duocuc.Induztek.data.remote

data class LoginRequest(
    val correo: String,
    val contrasena: String
)

data class LoginResponse(
    val token: String,
    val usuarioId: Long,
    val rolId: Long,
    val primerIngreso: Boolean
)

data class UsuarioResponse(
    val id: Long,
    val nombre: String,
    val apellido: String?,
    val rut: String,
    val correo: String,
    val telefono: String?,
    val empresaId: Long?,
    val rolId: Long,
    val rolNombre: String,
    val activo: Boolean,
    val debeCambiarClave: Boolean
)

data class RolResponse(
    val id: Long,
    val codigo: String?,
    val nombre: String
)

data class SolicitudCorreoRecuperacion(val correo: String)

data class SolicitudRestablecimiento(
    val correo: String,
    val codigo: String,
    val contrasenaNueva: String
)

data class SolicitudAsignacion(
    val asignadoPor: Long,
    val idsInstalacion: List<Long>
)

data class ErrorResponse(
    val message: String?,
    val error: String?,
    val detail: String?
)

data class ProtocoloPendienteRequest(
    val protocolo: ProtocoloRequest,
    val mediciones: List<MedicionRequest>
)

data class ProtocoloRequest(
    val idVisita: Long?,
    val uuidSync: String,
    val idEquipo: Long,
    val idTecnico: Long,
    val fechaHora: Long,
    val tipoPrueba: String,
    val atBt: Double?,
    val atTierra: Double?,
    val btTierra: Double?,
    val indicePolarizacion: Double?,
    val resultado: String,
    val observaciones: String?,
    val estado: String,
    val evidenciaEquipo: String?,
    val resultadoGlobal: String?
)

data class MedicionRequest(
    val idInstrumento: Long?,
    val tipoPrueba: String,
    val nombre: String?,
    val valor1: String?,
    val valor2: String?,
    val valor3: String?,
    val valor4: String?,
    val referencia: String?,
    val unidad: String?,
    val resultado: String?,
    val instrumentoNombre: String?,
    val instrumentoModelo: String?,
    val instrumentoSerie: String?,
    val calibracionVencimiento: String?
)
