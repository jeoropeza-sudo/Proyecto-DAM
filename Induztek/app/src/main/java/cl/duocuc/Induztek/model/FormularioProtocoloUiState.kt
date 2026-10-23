package cl.duocuc.Induztek.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class InstrumentoItem(
    val idInstrumento: Long = 1L,
    val modelo: String,
    val numeroSerie: String,
    val fechaVencimiento: String,
    val nombre: String = modelo,
    val fechaUltimaCalibracion: String = ""
)

// MEDICIONES TRANSFORMADOR Y CABLES (RN-06)
data class MedicionAislacionUiState(
    val devanado: String,
    val tensionPrueba: String = "5 kV",
    val lectura1Min: String = "",
    val lectura10Min: String = "",
    val ipCalculado: Double = 0.0,
    val resultado: String = "PENDIENTE"
)

// MEDICIONES TTR TRANSFORMADOR (RN-07)
data class MedicionTtrUiState(
    val fase: String,
    val relacionTeorica: Double = 30.00,
    val relacionMedida: String = "",
    val errorPct: Double = 0.0,
    val resultado: String = "PENDIENTE"
)

// MEDICIONES DEVANADOS TRANSFORMADOR (RN-08c)
data class MedicionDevanadoUiState(
    val devanado: String,
    val faseU: String = "",
    val faseV: String = "",
    val faseW: String = "",
    val unidad: String = "mΩ",
    val resultadoManual: String = "PENDIENTE"
)

// MEDICIONES RESISTENCIA DE CONTACTOS - INTERRUPTOR / CELDA MT (RN-08)
data class MedicionContactosUiState(
    val polo: String,
    val valorMaximoPlacaUohm: Double = 60.0,
    val valorMedidoUohm: String = "",
    val errorPctSobrePlaca: Double = 0.0,
    val resultado: String = "PENDIENTE"
)

// MEDICIONES INYECCIÓN SECUNDARIA - RELÉ DE PROTECCIÓN (RN-08b)
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
    // 1. Datos de la Visita
    val idVisita: Long = 1L,
    val idEmpresaSeleccionada: Long = 0L,
    val otNumero: String = "2026-1187",
    val fechaVisita: String = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
    val horaInicio: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
    val clienteNombre: String = "Envasadora del Pacífico S.A.",
    val clientesDisponibles: List<String> = emptyList(),
    val instalacionNombre: String = "Planta Lampa",
    val idInstalacionSeleccionada: Long = 0L,
    val instalacionesDisponibles: List<String> = emptyList(),
    val tecnicoNombre: String = "Matías Godoy Herrera",
    val ingenieroCargo: String = "Andrea Contreras Silva",
    val tempAmbiente: String = "19",
    val humedadPct: String = "60",
    val tipoVisita: String = "SOLO_PRUEBAS",
    val descripcionMantenimiento: String = "",
    val esMedicionPosterior: Boolean = false,
    val instrumentosDisponibles: List<String> = emptyList(),

    // 2. Datos del Equipo
    val idEquipo: Long = 1L,
    val tagEquipo: String = "TR-01",
    val tagsDisponibles: List<String> = emptyList(),
    val tipoEquipo: String = "Transformador", // Transformador, Celda MT, Interruptor, Cable de poder, Relé de protección
    val clasificacionEquipo: String = "PRIMARIO",
    val marcaEquipo: String = "Rhona",
    val modeloEquipo: String = "TR-1",
    val numeroSerieEquipo: String = "RH-18733",
    val potenciaKva: String = "1.500 kVA",
    val anoFabricacion: String = "2012",
    val tensiones: String = "12000 / 400 V",
    val tapPosition: String = "3",
    val conexionGrupo: String = "Dyn11",

    // Instrumentos (RN-11 / RN-12)
    val instrumentoAislacionSeleccionado: InstrumentoItem = InstrumentoItem(1L, "Megger MIT525", "MIT-0045821", "15/03/2027"),
    val instrumentosDisponiblesAislacion: List<InstrumentoItem> = listOf(
        InstrumentoItem(1L, "Megger MIT525", "MIT-0045821", "15/03/2027"),
        InstrumentoItem(2L, "Megger MIT1025", "MIT-0071233", "10/10/2026")
    ),

    val instrumentoTtrSeleccionado: InstrumentoItem = InstrumentoItem(3L, "Megger TTR300", "TTR-30044", "02/06/2027"),
    val instrumentosDisponiblesTtr: List<InstrumentoItem> = listOf(
        InstrumentoItem(3L, "Megger TTR300", "TTR-30044", "02/06/2027")
    ),

    val instrumentoDevanadosSeleccionado: InstrumentoItem = InstrumentoItem(4L, "Megger DLRO10X", "DLR-10882", "20/08/2026"),
    val instrumentosDisponiblesDevanados: List<InstrumentoItem> = listOf(
        InstrumentoItem(4L, "Megger DLRO10X", "DLR-10882", "20/08/2026")
    ),
    val instrumentoReleSeleccionado: InstrumentoItem = InstrumentoItem(5L, "Omicron CMC 356", "CMC-HB411", "05/01/2027"),
    val instrumentosDisponiblesRele: List<InstrumentoItem> = listOf(
        InstrumentoItem(5L, "Omicron CMC 356", "CMC-HB411", "05/01/2027")
    ),

    // 3. Mediciones por Tipo de Equipo (RN-05)
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

    // 7. Recepción Cliente (RF-08 / HU-08 / RN-03)
    val clienteRecibeNombre: String = "Marcela Fuentes Rojas",
    val clienteRecibeRut: String = "14.226.791-8",
    val clienteRecibeCargo: String = "Jefa de Mantenimiento",
    val errorRutClienteRecibe: Boolean = false,

    // Comparación Histórica y Resultados (RN-09 / RN-10)
    val lecturaAnterior1Min: Double? = 9800.0,
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