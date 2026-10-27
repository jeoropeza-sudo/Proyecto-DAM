package cl.duocuc.Induztek.model

data class RegistroProtocolo(
    val idEquipo: Long,
    val numeroOt: String,
    val fechaHora: String,
    val temperaturaC: Double,
    val humedadPct: Double,
    val observaciones: String,
    val tipoVisita: String,
    val descripcionMantenimiento: String,
    val receptorNombre: String,
    val receptorRut: String,
    val receptorCargo: String,
    val fechaRecepcion: String,
    val idUsuarioRegistra: Long,
    val resultadoGlobal: String,
    val estado: String,
    val mediciones: List<MedicionProtocolo>,
    val ingenieroNombre: String = ""
)

data class MedicionProtocolo(
    val tipoPrueba: String,
    val nombre: String,
    val valor1: String,
    val valor2: String = "",
    val valor3: String = "",
    val valor4: String = "",
    val referencia: String = "",
    val unidad: String = "",
    val resultado: String,
    val idInstrumento: Long? = null,
    val instrumento: String = ""
)

data class EquipoCatalogoLocal(
    val idEquipo: Long,
    val idInstalacion: Long,
    val tag: String,
    val tipo: String,
    val clasificacion: String,
    val marca: String,
    val modelo: String,
    val numeroSerie: String,
    val potenciaKva: Double?,
    val tensiones: String,
    val anoFabricacion: String = "",
    val posicionTap: String = "",
    val grupoConexion: String = "",
    val resistenciaContactoMaxUohm: Double? = null
)

data class ClienteCatalogoLocal(val idEmpresa: Long, val razonSocial: String)

data class InstalacionCatalogoLocal(val idInstalacion: Long, val nombre: String)

data class InstalacionAsignacionLocal(
    val idInstalacion: Long,
    val nombre: String,
    val empresa: String
)
