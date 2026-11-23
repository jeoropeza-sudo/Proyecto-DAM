package cl.duocuc.Induztek.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class InstrumentoItem(
    val idInstrumento: Long = 0L,
    val modelo: String,
    val numeroSerie: String,
    val fechaVencimiento: String,
    val nombre: String = modelo,
    val fechaUltimaCalibracion: String = ""
)

data class MedicionAislacionUiState(
    val devanado: String,
    val tensionPrueba: String = "5 kV",
    val lectura1Min: String = "",
    val lectura10Min: String = "",
    val ipCalculado: Double = 0.0,
    val resultado: String = "PENDIENTE"
)

data class MedicionTtrUiState(
    val fase: String,
    val relacionTeorica: Double = 30.00,
    val relacionMedida: String = "",
    val errorPct: Double = 0.0,
    val resultado: String = "PENDIENTE"
)

data class MedicionDevanadoUiState(
    val devanado: String,
    val faseU: String = "",
    val faseV: String = "",
    val faseW: String = "",
    val unidad: String = "mΩ",
    val resultadoManual: String = "PENDIENTE"
)

data class MedicionContactosUiState(
    val polo: String,
    val valorMaximoPlacaUohm: Double = 60.0,
    val valorMedidoUohm: String = "",
    val errorPctSobrePlaca: Double = 0.0,
    val resultado: String = "PENDIENTE"
)

data class MedicionReleUiState(
    val funcion: String = "50/51 Sobrecorriente",
    val corrienteAjusteA: String = "5.0",
    val corrienteMedidaA: String = "",
    val tiempoEsperadoMs: String = "100.0",
    val tiempoMedidoMs: String = "",
    val diferenciaTiempoPct: Double = 0.0,
    val operaCorrectamente: Boolean? = null,
    val resultado: String = "PENDIENTE"
)

data class FormularioProtocoloUiState(
    val idVisita: Long = 0L,
    val idEmpresaSeleccionada: Long = 0L,
    val otNumero: String = "",
    val fechaVisita: String = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
    val horaInicio: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
    val clienteNombre: String = "",
    val clientesDisponibles: List<String> = emptyList(),
    val instalacionNombre: String = "",
    val idInstalacionSeleccionada: Long = 0L,
    val instalacionesDisponibles: List<String> = emptyList(),
    val tecnicoNombre: String = "Matías Godoy Herrera",
    val ingenieroNombre: String = "",
    val tempAmbiente: String = "",
    val humedadPct: String = "",
    val tipoVisita: String = "SOLO PRUEBAS",
    val descripcionMantenimiento: String = "",
    val esMedicionPosterior: Boolean = false,
    val instrumentosDisponibles: List<String> = emptyList(),

    val idEquipo: Long = 0L,
    val tagEquipo: String = "",
    val tagsDisponibles: List<String> = emptyList(),
    val tipoEquipo: String = "Transformador",
    val clasificacionEquipo: String = "PRIMARIO",
    val marcaEquipo: String = "",
    val modeloEquipo: String = "",
    val numeroSerieEquipo: String = "",
    val potenciaKva: String = "",
    val anoFabricacion: String = "",
    val tensiones: String = "",
    val tapPosition: String = "",
    val conexionGrupo: String = "",

    val instrumentoAislacionSeleccionado: InstrumentoItem = InstrumentoItem(0L, "", "", ""),
    val instrumentosDisponiblesAislacion: List<InstrumentoItem> = emptyList(),

    val instrumentoTtrSeleccionado: InstrumentoItem = InstrumentoItem(0L, "", "", ""),
    val instrumentosDisponiblesTtr: List<InstrumentoItem> = emptyList(),

    val instrumentoDevanadosSeleccionado: InstrumentoItem = InstrumentoItem(0L, "", "", ""),
    val instrumentosDisponiblesDevanados: List<InstrumentoItem> = emptyList(),
    val instrumentoReleSeleccionado: InstrumentoItem = InstrumentoItem(0L, "", "", ""),
    val instrumentosDisponiblesRele: List<InstrumentoItem> = emptyList(),

    val aislacionFilas: List<MedicionAislacionUiState> = listOf(
        MedicionAislacionUiState(devanado = "AT – BT", tensionPrueba = "5 kV"),
        MedicionAislacionUiState(devanado = "AT – T", tensionPrueba = "5 kV"),
        MedicionAislacionUiState(devanado = "BT – T", tensionPrueba = "1 kV")
    ),

    val ttrFilas: List<MedicionTtrUiState> = listOf(
        MedicionTtrUiState(fase = "H1 – X1"),
        MedicionTtrUiState(fase = "H2 – X2"),
        MedicionTtrUiState(fase = "H3 – X3")
    ),

    val devanadoFilas: List<MedicionDevanadoUiState> = listOf(
        MedicionDevanadoUiState(devanado = "AT", unidad = "mΩ"),
        MedicionDevanadoUiState(devanado = "BT", unidad = "mΩ")
    ),

    val contactosFilas: List<MedicionContactosUiState> = listOf(
        MedicionContactosUiState(polo = "Polo A"),
        MedicionContactosUiState(polo = "Polo B"),
        MedicionContactosUiState(polo = "Polo C")
    ),

    val releFilas: List<MedicionReleUiState> = listOf(
        MedicionReleUiState()
    ),

    val pruebaRealizada: Boolean = true,
    val motivoNoRealizada: String = "",
    val observaciones: String = "",

    val clienteRecibeNombre: String = "",
    val clienteRecibeRut: String = "",
    val clienteRecibeCargo: String = "",
    val errorRutClienteRecibe: Boolean = false,
    val mensajeErrorRutClienteRecibe: String? = null,
    val mensajeErrorNombreClienteRecibe: String? = null,
    val mensajeErrorCargoClienteRecibe: String? = null,
    val mensajeErrorTemperatura: String? = null,
    val mensajeErrorHumedad: String? = null,
    val mensajeErrorDescripcionMantenimiento: String? = null,
    val mensajeErrorMediciones: String? = null,

    val lecturaAnterior1Min: Double? = null,
    val variacionDetectada: Boolean = false,
    val variacionPctCalculada: Double = 0.0,
    val resultadoGlobal: String = "PENDIENTE",
    val errorMotivoNoRealizada: String? = null,
    val mensajeErrorFormulario: String? = null,
    val mensajeNotificacionExito: String? = null,
    val idProtocoloRegistrado: Long? = null,
    val protocolosPendientesVisita: Int = 0,
    val tagsPendientesVisita: List<String> = emptyList()
)