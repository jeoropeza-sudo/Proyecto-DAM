package cl.duocuc.Induztek.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import cl.duocuc.Induztek.data.InduztekDbHelper
import cl.duocuc.Induztek.model.FormularioProtocoloUiState
import cl.duocuc.Induztek.model.InstrumentoItem
import cl.duocuc.Induztek.model.MedicionAislacionUiState
import cl.duocuc.Induztek.model.MedicionContactosUiState
import cl.duocuc.Induztek.model.MedicionReleUiState
import cl.duocuc.Induztek.model.MedicionTtrUiState
import cl.duocuc.Induztek.model.MedicionProtocolo
import cl.duocuc.Induztek.model.ProtocoloTerreno
import cl.duocuc.Induztek.model.RegistroProtocolo
import cl.duocuc.Induztek.model.Rol
import cl.duocuc.Induztek.model.Usuario
import cl.duocuc.Induztek.notificaciones.NotificacionesLocales
import cl.duocuc.Induztek.util.ValidarRut
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class ProtocoloViewModel(application: Application) : AndroidViewModel(application) {

    private val db = InduztekDbHelper(application)

    private val _uiState = MutableStateFlow(FormularioProtocoloUiState())
    val uiState: StateFlow<FormularioProtocoloUiState> = _uiState.asStateFlow()

    private val _historial = MutableStateFlow<List<ProtocoloTerreno>>(emptyList())
    val historial: StateFlow<List<ProtocoloTerreno>> = _historial.asStateFlow()
    private val protocolosPendientesVisita = mutableListOf<RegistroProtocolo>()
    private var usuarioVisitaPendiente: Long? = null
    private var usuarioCatalogoActual: Usuario? = null

    fun limpiarProtocoloRegistrado() {
        _uiState.update { it.copy(idProtocoloRegistrado = null) }
    }

    fun inicializarParaUsuario(usuario: Usuario) {
        usuarioCatalogoActual = usuario
        _historial.value = emptyList()
        if (usuarioVisitaPendiente != null && usuarioVisitaPendiente != usuario.idUsuario) {
            protocolosPendientesVisita.clear()
            usuarioVisitaPendiente = null
            _uiState.update {
                it.copy(protocolosPendientesVisita = 0, tagsPendientesVisita = emptyList())
            }
        }
        val empresas = db.listarClientes()
        val empresasPermitidas = when (usuario.rolCodigo.uppercase()) {
            Rol.CODIGO_CLIENTE -> empresas.filter { it.idEmpresa == usuario.idEmpresaCliente }
            Rol.CODIGO_TECNICO -> db.listarClientesTecnico(usuario.idUsuario)
            else -> empresas
        }
        val instrumentosAislacion = db.listarInstrumentos("AISLACION")
        val instrumentosTtr = db.listarInstrumentos("TTR")
        val instrumentosDevanados = db.listarInstrumentos("DEVANADOS")
        val instrumentosRele = db.listarInstrumentos("RELE")
        _uiState.update {
            it.copy(
                clientesDisponibles = empresasPermitidas.map { empresa -> empresa.razonSocial },
                instrumentosDisponiblesAislacion = instrumentosAislacion,
                instrumentosDisponiblesTtr = instrumentosTtr,
                instrumentosDisponiblesDevanados = instrumentosDevanados,
                instrumentosDisponiblesRele = instrumentosRele,
                instrumentoAislacionSeleccionado = primeroVigente(instrumentosAislacion) ?: it.instrumentoAislacionSeleccionado,
                instrumentoTtrSeleccionado = primeroVigente(instrumentosTtr) ?: it.instrumentoTtrSeleccionado,
                instrumentoDevanadosSeleccionado = primeroVigente(instrumentosDevanados) ?: it.instrumentoDevanadosSeleccionado,
                instrumentoReleSeleccionado = primeroVigente(instrumentosRele) ?: it.instrumentoReleSeleccionado
            )
        }
        if (empresasPermitidas.isNotEmpty()) {
            onClienteSeleccionado(empresasPermitidas.first().razonSocial)
        } else {
            _uiState.update {
                it.copy(
                    clienteNombre = if (usuario.rolCodigo.equals(Rol.CODIGO_TECNICO, ignoreCase = true)) {
                        "Sin instalaciones asignadas"
                    } else {
                        "Sin empresa asignada"
                    },
                    idEmpresaSeleccionada = 0L,
                    instalacionesDisponibles = emptyList(),
                    instalacionNombre = "",
                    idInstalacionSeleccionada = 0L,
                    tagsDisponibles = emptyList(),
                    idEquipo = 0L,
                    tagEquipo = "",
                    tipoEquipo = "",
                    clasificacionEquipo = "",
                    marcaEquipo = "",
                    modeloEquipo = "",
                    numeroSerieEquipo = "",
                    potenciaKva = "",
                    anoFabricacion = "",
                    tensiones = "",
                    tapPosition = "",
                    conexionGrupo = "",
                    aislacionFilas = emptyList(),
                    ttrFilas = emptyList(),
                    devanadoFilas = emptyList(),
                    contactosFilas = emptyList(),
                    releFilas = emptyList()
                )
            }
        }
    }

    fun tecnicosParaAsignacion(usuario: Usuario?): List<Usuario> =
        if (usuario?.rolCodigo.equals(Rol.CODIGO_JEFE_TECNICO, ignoreCase = true)) {
            db.listarTecnicosActivos()
        } else {
            emptyList()
        }

    fun instalacionesParaAsignacion(usuario: Usuario?) =
        if (usuario?.rolCodigo.equals(Rol.CODIGO_JEFE_TECNICO, ignoreCase = true)) {
            db.listarInstalacionesParaAsignacion()
        } else {
            emptyList()
        }

    fun idsAsignadosATecnico(idTecnico: Long, usuario: Usuario?): Set<Long> =
        if (usuario?.rolCodigo.equals(Rol.CODIGO_JEFE_TECNICO, ignoreCase = true)) {
            db.listarIdsInstalacionesTecnico(idTecnico)
        } else {
            emptySet()
        }

    fun instalacionesAsignadasAOtrosTecnicos(
        idTecnico: Long,
        usuario: Usuario?
    ): Map<Long, String> =
        if (usuario?.rolCodigo.equals(Rol.CODIGO_JEFE_TECNICO, ignoreCase = true)) {
            db.listarTecnicosPropietariosDeInstalaciones(idTecnico)
        } else {
            emptyMap()
        }

    fun guardarAsignacionesTecnico(
        idTecnico: Long,
        idsInstalaciones: Set<Long>,
        usuario: Usuario?
    ): Boolean {
        val jefeTecnico = usuario ?: return false
        if (!jefeTecnico.rolCodigo.equals(Rol.CODIGO_JEFE_TECNICO, ignoreCase = true)) return false
        return db.guardarAsignacionesTecnico(idTecnico, idsInstalaciones, jefeTecnico.idUsuario)
    }

    private fun obtenerHoraActual(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    fun onClienteSeleccionado(cliente: String) {
        val empresa = db.listarClientes().firstOrNull { it.razonSocial == cliente } ?: return
        val usuario = usuarioCatalogoActual
        if (
            usuario?.rolCodigo.equals(Rol.CODIGO_CLIENTE, ignoreCase = true) &&
            empresa.idEmpresa != usuario?.idEmpresaCliente
        ) return
        val instalaciones = if (
            usuario != null && usuario.rolCodigo.equals(Rol.CODIGO_TECNICO, ignoreCase = true)
        ) {
            db.listarInstalacionesTecnico(empresa.idEmpresa, usuario.idUsuario)
        } else {
            db.listarInstalacionesCliente(empresa.idEmpresa)
        }
        val contacto = db.buscarContactoEmpresa(empresa.idEmpresa)

        _uiState.update {
            it.copy(
                idEmpresaSeleccionada = empresa.idEmpresa,
                clienteNombre = cliente,
                instalacionesDisponibles = instalaciones.map { instalacion -> instalacion.nombre },
                instalacionNombre = instalaciones.firstOrNull()?.nombre.orEmpty(),
                idInstalacionSeleccionada = instalaciones.firstOrNull()?.idInstalacion ?: 0L,
                tagsDisponibles = emptyList(),
                idEquipo = 0L,
                tagEquipo = "",
                tipoEquipo = "",
                clasificacionEquipo = "",
                marcaEquipo = "",
                modeloEquipo = "",
                numeroSerieEquipo = "",
                potenciaKva = "",
                anoFabricacion = "",
                tensiones = "",
                tapPosition = "",
                conexionGrupo = "",
                aislacionFilas = emptyList(),
                ttrFilas = emptyList(),
                devanadoFilas = emptyList(),
                contactosFilas = emptyList(),
                releFilas = emptyList(),
                clienteRecibeNombre = contacto?.nombre.orEmpty(),
                clienteRecibeRut = contacto?.rut.orEmpty(),
                clienteRecibeCargo = "",
                errorRutClienteRecibe = false
            )
        }
        instalaciones.firstOrNull()?.let { onInstalacionSeleccionada(it.nombre) }
    }

    fun onInstalacionSeleccionada(instalacion: String) {
        val estado = _uiState.value
        val usuario = usuarioCatalogoActual
        if (
            usuario?.rolCodigo.equals(Rol.CODIGO_CLIENTE, ignoreCase = true) &&
            estado.idEmpresaSeleccionada != usuario?.idEmpresaCliente
        ) return
        val instalacionesPermitidas = if (
            usuario != null && usuario.rolCodigo.equals(Rol.CODIGO_TECNICO, ignoreCase = true)
        ) {
            db.listarInstalacionesTecnico(estado.idEmpresaSeleccionada, usuario.idUsuario)
        } else {
            db.listarInstalacionesCliente(estado.idEmpresaSeleccionada)
        }
        val instalacionLocal = instalacionesPermitidas
            .firstOrNull { it.nombre == instalacion } ?: return
        val tagsList = db.listarEquiposPorInstalacion(instalacionLocal.idInstalacion).map { it.tag }

        _uiState.update {
            it.copy(
                idInstalacionSeleccionada = instalacionLocal.idInstalacion,
                instalacionNombre = instalacion,
                tagsDisponibles = tagsList,
                idEquipo = 0L,
                tagEquipo = "",
                tipoEquipo = "",
                clasificacionEquipo = "",
                marcaEquipo = "",
                modeloEquipo = "",
                numeroSerieEquipo = "",
                potenciaKva = "",
                anoFabricacion = "",
                tensiones = "",
                tapPosition = "",
                conexionGrupo = "",
                aislacionFilas = emptyList(),
                ttrFilas = emptyList(),
                devanadoFilas = emptyList(),
                contactosFilas = emptyList(),
                releFilas = emptyList()
            )
        }
        if (tagsList.isNotEmpty()) {
            onTagEquipoSeleccionado(tagsList.first())
        }
    }

    fun onTagEquipoSeleccionado(tag: String) {
        val estado = _uiState.value
        val equipo = db.listarEquiposPorInstalacion(estado.idInstalacionSeleccionada)
            .firstOrNull { it.tag == tag } ?: return
        val lecturaAnt = db.obtenerUltimaLecturaAnterior(equipo.idEquipo, "LECTURA_1MIN")

        val aislacionesNuevas = when (equipo.tipo) {
            "Transformador" -> listOf(
                MedicionAislacionUiState(devanado = "AT – BT", tensionPrueba = "5 kV"),
                MedicionAislacionUiState(devanado = "AT – T", tensionPrueba = "5 kV"),
                MedicionAislacionUiState(devanado = "BT – T", tensionPrueba = "1 kV")
            )
            "Interruptor", "Celda MT" -> listOf(
                MedicionAislacionUiState(devanado = "Entrada – Salida", tensionPrueba = "5 kV"),
                MedicionAislacionUiState(devanado = "Fase – Tierra", tensionPrueba = "5 kV")
            )
            "Cable de poder" -> listOf(
                MedicionAislacionUiState(devanado = "Fase A – Tierra", tensionPrueba = "5 kV"),
                MedicionAislacionUiState(devanado = "Fase B – Tierra", tensionPrueba = "5 kV"),
                MedicionAislacionUiState(devanado = "Fase C – Tierra", tensionPrueba = "5 kV")
            )
            else -> emptyList()
        }

        val relacionNominal = calcularRelacionNominal(equipo.tensiones)
        val ttrNuevas = if (equipo.tipo == "Transformador") {
            listOf(
                MedicionTtrUiState(fase = "H1 – X1", relacionTeorica = relacionNominal),
                MedicionTtrUiState(fase = "H2 – X2", relacionTeorica = relacionNominal),
                MedicionTtrUiState(fase = "H3 – X3", relacionTeorica = relacionNominal)
            )
        } else emptyList()

        val contactosNuevos = if (equipo.tipo == "Interruptor" || equipo.tipo == "Celda MT") {
            listOf(
                MedicionContactosUiState(polo = "Polo A", valorMaximoPlacaUohm = equipo.resistenciaContactoMaxUohm ?: 0.0),
                MedicionContactosUiState(polo = "Polo B", valorMaximoPlacaUohm = equipo.resistenciaContactoMaxUohm ?: 0.0),
                MedicionContactosUiState(polo = "Polo C", valorMaximoPlacaUohm = equipo.resistenciaContactoMaxUohm ?: 0.0)
            )
        } else emptyList()

        val releNuevos = if (equipo.tipo == "Relé de protección") {
            listOf(MedicionReleUiState())
        } else emptyList()

        _uiState.update {
            it.copy(
                idEquipo = equipo.idEquipo,
                tagEquipo = equipo.tag,
                tipoEquipo = equipo.tipo,
                clasificacionEquipo = equipo.clasificacion,
                marcaEquipo = equipo.marca,
                modeloEquipo = equipo.modelo,
                numeroSerieEquipo = equipo.numeroSerie,
                potenciaKva = equipo.potenciaKva?.let {
                    String.format(Locale.ROOT, "%.0f kVA", it)
                }.orEmpty(),
                anoFabricacion = equipo.anoFabricacion,
                tensiones = equipo.tensiones,
                tapPosition = equipo.posicionTap,
                conexionGrupo = equipo.grupoConexion,
                lecturaAnterior1Min = lecturaAnt,
                aislacionFilas = aislacionesNuevas,
                ttrFilas = ttrNuevas,
                devanadoFilas = if (equipo.tipo == "Transformador") listOf(
                    cl.duocuc.Induztek.model.MedicionDevanadoUiState("AT", unidad = "mΩ"),
                    cl.duocuc.Induztek.model.MedicionDevanadoUiState("BT", unidad = "mΩ")
                ) else emptyList(),
                contactosFilas = contactosNuevos,
                releFilas = releNuevos,
                mensajeErrorFormulario = null
            )
        }

        recargarHistorial(equipo.idEquipo)
        recalcularGlobal()
    }

    private fun calcularRelacionNominal(tensiones: String): Double {
        val coincidencia = Regex("""(\d+(?:[.,]\d+)?)\s*/\s*(\d+(?:[.,]\d+)?)""")
            .find(tensiones) ?: return 0.0
        val primario = coincidencia.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return 0.0
        val secundario = coincidencia.groupValues[2].replace(',', '.').toDoubleOrNull() ?: return 0.0
        return if (primario > 0 && secundario > 0) primario / secundario else 0.0
    }

    fun recargarHistorial(idEquipo: Long) {
        _historial.value = db.listarProtocolosPorEquipo(idEquipo)
    }

    fun cargarHistorialEquipo(idEquipo: Long, usuario: Usuario?) {
        val protocolos = if (
            usuario != null && usuario.rolCodigo.equals(Rol.CODIGO_TECNICO, ignoreCase = true)
        ) {
            if (!db.tecnicoPuedeVerEquipo(usuario.idUsuario, idEquipo)) {
                _historial.value = emptyList()
                return
            }
            db.listarProtocolosPorEquipo(idEquipo)
        } else if (usuario?.rolCodigo.equals(Rol.CODIGO_CLIENTE, ignoreCase = true)) {
            val idEmpresa = usuario?.idEmpresaCliente
            if (idEmpresa == null) {
                _historial.value = emptyList()
                return
            }
            db.listarProtocolosEquipoCliente(idEquipo, idEmpresa)
        } else {
            db.listarProtocolosPorEquipo(idEquipo)
        }
        _historial.value = protocolos
        protocolos.firstOrNull()?.let { protocolo ->
            _uiState.update {
                it.copy(tagEquipo = protocolo.tagEquipo, instalacionNombre = protocolo.instalacionNombre)
            }
        }
    }

    fun cargarProtocolosCliente(usuario: Usuario) {
        val idEmpresa = usuario.idEmpresaCliente ?: return
        _historial.value = db.listarProtocolosCliente(idEmpresa)
    }

    fun obtenerProtocoloVisible(idProtocolo: Long, usuario: Usuario?): ProtocoloTerreno? {
        usuario ?: return null
        if (usuario.rolCodigo.equals(Rol.CODIGO_TECNICO, ignoreCase = true) &&
            !db.tecnicoPuedeVerProtocolo(usuario.idUsuario, idProtocolo)
        ) return null
        if (usuario.rolCodigo.equals(Rol.CODIGO_CLIENTE, ignoreCase = true)) {
            val idEmpresa = usuario.idEmpresaCliente ?: return null
            if (!db.clientePuedeVerProtocolo(idProtocolo, idEmpresa)) return null
        }
        return db.obtenerProtocoloPorId(idProtocolo)
    }

    fun cambiarEstadoProtocolo(
        idProtocolo: Long,
        nuevoEstado: String,
        usuario: Usuario,
        motivo: String? = null
    ): ProtocoloTerreno? {
        if (!usuario.rolCodigo.equals("INGENIERO", ignoreCase = true) &&
            !usuario.rolCodigo.equals("JEFE_TECNICO", ignoreCase = true)
        ) return null
        if (!db.cambiarEstadoProtocolo(idProtocolo, nuevoEstado, usuario.idUsuario, motivo)) return null
        val protocolo = db.obtenerProtocoloPorId(idProtocolo) ?: return null
        NotificacionesLocales.notificarEstadoProtocolo(
            getApplication(),
            protocolo.numeroProtocolo,
            protocolo.estado
        )
        return protocolo
    }

    private fun validarInstrumentoVigente(inst: InstrumentoItem): Boolean {
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).apply { isLenient = false }
        val fechaEnsayo = runCatching { formato.parse(_uiState.value.fechaVisita) }.getOrNull() ?: return false
        val fechaVencimiento = runCatching { formato.parse(inst.fechaVencimiento) }.getOrNull() ?: return false
        val comparable = SimpleDateFormat("yyyyMMdd", Locale.ROOT)
        return comparable.format(fechaVencimiento) >= comparable.format(fechaEnsayo)
    }

    private fun primeroVigente(instrumentos: List<InstrumentoItem>): InstrumentoItem? =
        instrumentos.firstOrNull(::validarInstrumentoVigente) ?: instrumentos.firstOrNull()

    fun onInstrumentoTtrSeleccionado(seleccionTexto: String) {
        _uiState.update { actual ->
            val instrumento = actual.instrumentosDisponiblesTtr.firstOrNull {
                "${it.modelo} (${it.numeroSerie})" == seleccionTexto
            } ?: return@update actual
            if (!validarInstrumentoVigente(instrumento)) {
                actual.copy(mensajeErrorFormulario = "La calibración de ${instrumento.modelo} venció el ${instrumento.fechaVencimiento}.")
            } else actual.copy(instrumentoTtrSeleccionado = instrumento, mensajeErrorFormulario = null)
        }
    }

    fun onInstrumentoDevanadosSeleccionado(seleccionTexto: String) {
        _uiState.update { actual ->
            val instrumento = actual.instrumentosDisponiblesDevanados.firstOrNull {
                "${it.modelo} (${it.numeroSerie})" == seleccionTexto
            } ?: return@update actual
            if (!validarInstrumentoVigente(instrumento)) {
                actual.copy(mensajeErrorFormulario = "La calibración de ${instrumento.modelo} venció el ${instrumento.fechaVencimiento}.")
            } else actual.copy(instrumentoDevanadosSeleccionado = instrumento, mensajeErrorFormulario = null)
        }
    }

    fun onInstrumentoReleSeleccionado(seleccionTexto: String) {
        _uiState.update { actual ->
            val instrumento = actual.instrumentosDisponiblesRele.firstOrNull {
                "${it.modelo} (${it.numeroSerie})" == seleccionTexto
            } ?: return@update actual
            if (!validarInstrumentoVigente(instrumento)) {
                actual.copy(mensajeErrorFormulario = "La calibración de ${instrumento.modelo} venció el ${instrumento.fechaVencimiento}.")
            } else actual.copy(instrumentoReleSeleccionado = instrumento, mensajeErrorFormulario = null)
        }
    }

    fun onInstrumentoAislacionSeleccionado(seleccionTexto: String) {
        _uiState.update { estadoActual ->
            val instrumentoEncontrado = estadoActual.instrumentosDisponiblesAislacion.find {
                "${it.modelo} (${it.numeroSerie})" == seleccionTexto
            } ?: estadoActual.instrumentoAislacionSeleccionado
            if (!validarInstrumentoVigente(instrumentoEncontrado)) {
                estadoActual.copy(
                    mensajeErrorFormulario = "La calibración de ${instrumentoEncontrado.modelo} vence el ${instrumentoEncontrado.fechaVencimiento}."
                )
            } else {
                estadoActual.copy(instrumentoAislacionSeleccionado = instrumentoEncontrado, mensajeErrorFormulario = null)
            }
        }
    }

    fun onTempAmbienteCambia(valor: String) = _uiState.update { it.copy(tempAmbiente = valor) }
    fun onHumedadPctCambia(valor: String) = _uiState.update { it.copy(humedadPct = valor) }
    fun onHoraInicioCambia(valor: String) = _uiState.update { it.copy(horaInicio = valor) }
    fun onTipoVisitaCambia(tipo: String) = _uiState.update { it.copy(tipoVisita = tipo) }
    fun onDescripcionMantenimientoCambia(valTexto: String) = _uiState.update { it.copy(descripcionMantenimiento = valTexto) }
    fun onEsMedicionPosteriorCambia(esPosterior: Boolean) = _uiState.update { it.copy(esMedicionPosterior = esPosterior) }

    fun onPruebaRealizadaCambia(realizada: Boolean) {
        _uiState.update { it.copy(pruebaRealizada = realizada, errorMotivoNoRealizada = null, mensajeErrorFormulario = null) }
        recalcularGlobal()
    }

    fun onMotivoNoRealizadaCambia(motivo: String) {
        _uiState.update { it.copy(motivoNoRealizada = motivo, errorMotivoNoRealizada = null) }
    }

    fun onAislacionCambia(index: Int, l1Str: String, l10Str: String) {
        val lista = _uiState.value.aislacionFilas.toMutableList()
        if (index !in lista.indices) return
        val l1 = l1Str.replace(',', '.').toDoubleOrNull() ?: 0.0
        val l10 = l10Str.replace(',', '.').toDoubleOrNull() ?: 0.0
        val ip = if (l1 > 0) l10 / l1 else 0.0

        val res = when {
            l1Str.isBlank() && l10Str.isBlank() -> "PENDIENTE"
            l1Str.replace(',', '.').toDoubleOrNull() == null ||
                    l10Str.replace(',', '.').toDoubleOrNull() == null ||
                    l1 <= 0 || l10 <= 0 -> "PENDIENTE"
            l1 < 100.0 || ip < 1.0 -> "RECHAZADO"
            ip >= 1.0 && ip < 2.0 -> "OBSERVADO"
            else -> "APROBADO"
        }

        lista[index] = lista[index].copy(
            lectura1Min = l1Str,
            lectura10Min = l10Str,
            ipCalculado = ip,
            resultado = res
        )

        _uiState.update { it.copy(aislacionFilas = lista, mensajeErrorFormulario = null) }
        evaluarVariacionHistorica(l1)
        recalcularGlobal()
    }

    private fun evaluarVariacionHistorica(l1Actual: Double) {
        val ant = _uiState.value.lecturaAnterior1Min
        if (ant != null && ant > 0 && l1Actual > 0) {
            val pct = abs(l1Actual - ant) / ant * 100.0
            _uiState.update {
                it.copy(
                    variacionPctCalculada = pct,
                    variacionDetectada = pct > 25.0
                )
            }
        } else {
            _uiState.update { it.copy(variacionDetectada = false, variacionPctCalculada = 0.0) }
        }
    }

    fun onTtrMedidaCambia(index: Int, medidaStr: String) {
        val lista = _uiState.value.ttrFilas.toMutableList()
        if (index !in lista.indices) return
        val teorica = lista[index].relacionTeorica
        val medidaParseada = medidaStr.replace(',', '.').toDoubleOrNull()
        val medida = medidaParseada ?: 0.0
        val err = if (teorica > 0 && medida > 0) abs(medida - teorica) / teorica * 100.0 else 0.0

        val res = when {
            medidaStr.isBlank() -> "PENDIENTE"
            medidaParseada == null || medida <= 0 || teorica <= 0 -> "PENDIENTE"
            err <= 0.5 -> "APROBADO"
            err <= 1.0 -> "OBSERVADO"
            else -> "RECHAZADO"
        }

        lista[index] = lista[index].copy(
            relacionMedida = medidaStr,
            errorPct = err,
            resultado = res
        )

        _uiState.update { it.copy(ttrFilas = lista, mensajeErrorFormulario = null) }
        recalcularGlobal()
    }

    fun onContactoMedidoCambia(index: Int, medidoStr: String) {
        val lista = _uiState.value.contactosFilas.toMutableList()
        if (index !in lista.indices) return
        val maxPlaca = lista[index].valorMaximoPlacaUohm
        val medidoParseado = medidoStr.replace(',', '.').toDoubleOrNull()
        val medido = medidoParseado ?: 0.0
        val pct = if (maxPlaca > 0 && medido > 0) (medido / maxPlaca) * 100.0 else 0.0

        val res = when {
            medidoStr.isBlank() -> "PENDIENTE"
            medidoParseado == null || medido <= 0 || maxPlaca <= 0 -> "PENDIENTE"
            pct <= 100.0 -> "APROBADO"
            pct <= 120.0 -> "OBSERVADO"
            else -> "RECHAZADO"
        }

        lista[index] = lista[index].copy(
            valorMedidoUohm = medidoStr,
            errorPctSobrePlaca = pct,
            resultado = res
        )

        _uiState.update { it.copy(contactosFilas = lista, mensajeErrorFormulario = null) }
        recalcularGlobal()
    }

    fun onReleMedicionCambia(index: Int, tiempoMedidoMsStr: String, opera: Boolean?) {
        val lista = _uiState.value.releFilas.toMutableList()
        if (index !in lista.indices) return
        val fila = lista[index]
        val esperado = fila.tiempoEsperadoMs.replace(',', '.').toDoubleOrNull() ?: 100.0
        val medido = tiempoMedidoMsStr.replace(',', '.').toDoubleOrNull() ?: 0.0
        val ajuste = fila.corrienteAjusteA.replace(',', '.').toDoubleOrNull() ?: 0.0
        val corriente = fila.corrienteMedidaA.replace(',', '.').toDoubleOrNull() ?: 0.0
        val desviacionTiempo = if (esperado > 0 && medido > 0) abs(medido - esperado) / esperado * 100.0 else 0.0
        val desviacionCorriente = if (ajuste > 0 && corriente > 0) {
            abs(corriente - ajuste) / ajuste * 100.0
        } else 0.0
        val difPct = maxOf(desviacionTiempo, desviacionCorriente)

        val res = when {
            opera == false -> "RECHAZADO"
            opera == null -> "PENDIENTE"
            tiempoMedidoMsStr.isBlank() || ajuste <= 0 || corriente <= 0 ||
                    (opera == true && (medido <= 0 || esperado <= 0)) -> "PENDIENTE"
            difPct <= 5.0 -> "APROBADO"
            difPct <= 10.0 -> "OBSERVADO"
            else -> "RECHAZADO"
        }

        lista[index] = lista[index].copy(
            tiempoMedidoMs = tiempoMedidoMsStr,
            diferenciaTiempoPct = difPct,
            operaCorrectamente = opera,
            resultado = res
        )

        _uiState.update { it.copy(releFilas = lista, mensajeErrorFormulario = null) }
        recalcularGlobal()
    }

    fun onReleCorrienteMedidaCambia(index: Int, corriente: String) {
        val lista = _uiState.value.releFilas.toMutableList()
        if (index !in lista.indices) return
        lista[index] = lista[index].copy(corrienteMedidaA = corriente)
        _uiState.update { it.copy(releFilas = lista, mensajeErrorFormulario = null) }
        onReleMedicionCambia(index, lista[index].tiempoMedidoMs, lista[index].operaCorrectamente)
    }

    fun onReleOperacionCambia(index: Int, opera: Boolean) {
        val lista = _uiState.value.releFilas.toMutableList()
        if (index !in lista.indices) return
        val fila = lista[index]
        lista[index] = fila.copy(
            operaCorrectamente = opera,
            resultado = if (!opera) "RECHAZADO" else if (fila.tiempoMedidoMs.isBlank()) "PENDIENTE" else fila.resultado
        )
        _uiState.update { it.copy(releFilas = lista, mensajeErrorFormulario = null) }
        onReleMedicionCambia(index, fila.tiempoMedidoMs, opera)
    }

    fun onClienteRecibeNombreCambia(valor: String) = _uiState.update { it.copy(clienteRecibeNombre = valor) }

    fun onClienteRecibeRutCambia(valor: String) {
        val esValido = ValidarRut.validarRut(valor)
        _uiState.update {
            it.copy(
                clienteRecibeRut = valor,
                errorRutClienteRecibe = !esValido && valor.isNotBlank()
            )
        }
    }

    fun onClienteRecibeCargoCambia(valor: String) = _uiState.update { it.copy(clienteRecibeCargo = valor) }
    fun onObservacionesCambia(valor: String) = _uiState.update { it.copy(observaciones = valor) }

    private fun recalcularGlobal() {
        val estado = _uiState.value
        if (!estado.pruebaRealizada) {
            _uiState.update { it.copy(resultadoGlobal = "NO REALIZADA") }
            return
        }

        val lista = mutableListOf<String>()
        estado.aislacionFilas.forEach { lista.add(it.resultado) }
        estado.ttrFilas.forEach { lista.add(it.resultado) }
        estado.devanadoFilas.forEach { lista.add(it.resultadoManual) }
        estado.contactosFilas.forEach { lista.add(it.resultado) }
        estado.releFilas.forEach { lista.add(it.resultado) }

        val peor = when {
            lista.contains("RECHAZADO") -> "RECHAZADO"
            lista.contains("OBSERVADO") -> "OBSERVADO"
            lista.all { it == "APROBADO" } -> "APROBADO"
            else -> "PENDIENTE"
        }

        _uiState.update { it.copy(resultadoGlobal = peor) }
    }

    fun onRegistrarProtocolo(idUsuarioActivo: Long, agregarAVisita: Boolean = false): Boolean {
        val estado = _uiState.value

        val temperatura = estado.tempAmbiente.replace(',', '.').toDoubleOrNull()
        val humedad = estado.humedadPct.replace(',', '.').toDoubleOrNull()
        if (temperatura == null || !temperatura.isFinite() ||
            humedad == null || !humedad.isFinite() || humedad !in 0.0..100.0
        ) {
            _uiState.update {
                it.copy(mensajeErrorFormulario = "Ingrese temperatura y humedad válidas (humedad entre 0 y 100 %).")
            }
            return false
        }

        if (!ValidarRut.validarRut(estado.clienteRecibeRut)) {
            _uiState.update {
                it.copy(
                    errorRutClienteRecibe = true,
                    mensajeErrorFormulario = "El RUT del receptor del cliente es inválido (RN-03)."
                )
            }
            return false
        }

        if (estado.clienteRecibeNombre.isBlank() || estado.clienteRecibeCargo.isBlank()) {
            _uiState.update {
                it.copy(mensajeErrorFormulario = "Complete nombre y cargo de quien recibe el acta.")
            }
            return false
        }

        if (
            estado.tipoVisita == "PRUEBAS_Y_MANTENIMIENTO" &&
            estado.descripcionMantenimiento.isBlank()
        ) {
            _uiState.update {
                it.copy(mensajeErrorFormulario = "Describa el mantenimiento realizado para cerrar esta visita.")
            }
            return false
        }

        if (!estado.pruebaRealizada) {
            if (estado.motivoNoRealizada.isBlank()) {
                _uiState.update {
                    it.copy(
                        errorMotivoNoRealizada = "Debe ingresar la justificación obligatoria.",
                        mensajeErrorFormulario = "Complete los campos obligatorios antes de registrar el protocolo."
                    )
                }
                return false
            }
        } else {
            fun esPositivo(valor: String): Boolean =
                valor.replace(',', '.').toDoubleOrNull()?.let { it > 0 } == true

            val aislamientoCompleto = estado.aislacionFilas.isNotEmpty() &&
                    estado.aislacionFilas.all { esPositivo(it.lectura1Min) && esPositivo(it.lectura10Min) }
            val medicionesCompletas = when (estado.tipoEquipo) {
                "Transformador" -> aislamientoCompleto &&
                        estado.ttrFilas.isNotEmpty() && estado.ttrFilas.all {
                            it.relacionTeorica > 0 && esPositivo(it.relacionMedida)
                        } &&
                        estado.devanadoFilas.isNotEmpty() && estado.devanadoFilas.all {
                            esPositivo(it.faseU) && esPositivo(it.faseV) && esPositivo(it.faseW) &&
                                    it.resultadoManual in setOf("APROBADO", "OBSERVADO", "RECHAZADO")
                        }
                "Interruptor", "Celda MT" -> aislamientoCompleto &&
                        estado.contactosFilas.isNotEmpty() && estado.contactosFilas.all {
                            it.valorMaximoPlacaUohm > 0 && esPositivo(it.valorMedidoUohm)
                        }
                "Cable de poder" -> aislamientoCompleto
                "Relé de protección" -> estado.releFilas.isNotEmpty() && estado.releFilas.all {
                    esPositivo(it.corrienteMedidaA) && it.operaCorrectamente != null &&
                            (esPositivo(it.tiempoMedidoMs) || !it.operaCorrectamente)
                }
                else -> false
            }

            if (!medicionesCompletas) {
                _uiState.update {
                    it.copy(mensajeErrorFormulario = "Complete todas las mediciones obligatorias para el tipo de equipo seleccionado.")
                }
                return false
            }
        }

        val mediciones = buildList {
            estado.aislacionFilas.filter {
                it.lectura1Min.isNotBlank() || it.lectura10Min.isNotBlank()
            }.forEach { fila ->
                add(
                    MedicionProtocolo(
                        tipoPrueba = "AISLACION",
                        nombre = fila.devanado,
                        valor1 = fila.lectura1Min,
                        valor2 = fila.lectura10Min,
                        valor3 = String.format(Locale.ROOT, "%.2f", fila.ipCalculado),
                        referencia = fila.tensionPrueba,
                        unidad = "MΩ",
                        resultado = fila.resultado,
                        idInstrumento = estado.instrumentoAislacionSeleccionado.idInstrumento
                    )
                )
            }
            estado.ttrFilas.filter { it.relacionMedida.isNotBlank() }.forEach { fila ->
                add(
                    MedicionProtocolo(
                        tipoPrueba = "TTR",
                        nombre = fila.fase,
                        valor1 = fila.relacionMedida,
                        valor2 = String.format(Locale.ROOT, "%.2f", fila.errorPct),
                        referencia = fila.relacionTeorica.toString(),
                        unidad = "relación",
                        resultado = fila.resultado,
                        idInstrumento = estado.instrumentoTtrSeleccionado.idInstrumento
                    )
                )
            }
            estado.devanadoFilas.filter {
                it.faseU.isNotBlank() || it.faseV.isNotBlank() || it.faseW.isNotBlank()
            }.forEach { fila ->
                add(
                    MedicionProtocolo(
                        tipoPrueba = "DEVANADOS",
                        nombre = fila.devanado,
                        valor1 = fila.faseU,
                        valor2 = fila.faseV,
                        valor3 = fila.faseW,
                        unidad = fila.unidad,
                        resultado = fila.resultadoManual,
                        idInstrumento = estado.instrumentoDevanadosSeleccionado.idInstrumento
                    )
                )
            }
            estado.contactosFilas.filter { it.valorMedidoUohm.isNotBlank() }.forEach { fila ->
                add(
                    MedicionProtocolo(
                        tipoPrueba = "CONTACTOS",
                        nombre = fila.polo,
                        valor1 = fila.valorMedidoUohm,
                        valor2 = String.format(Locale.ROOT, "%.2f", fila.errorPctSobrePlaca),
                        referencia = fila.valorMaximoPlacaUohm.toString(),
                        unidad = "µΩ",
                        resultado = fila.resultado,
                        idInstrumento = estado.instrumentoDevanadosSeleccionado.idInstrumento
                    )
                )
            }
            estado.releFilas.filter {
                it.corrienteMedidaA.isNotBlank() || it.tiempoMedidoMs.isNotBlank() ||
                        it.operaCorrectamente != null
            }.forEach { fila ->
                add(
                    MedicionProtocolo(
                        tipoPrueba = "RELE",
                        nombre = fila.funcion,
                        valor1 = fila.corrienteAjusteA,
                        valor2 = fila.corrienteMedidaA,
                        valor3 = fila.tiempoMedidoMs,
                        valor4 = when (fila.operaCorrectamente) {
                            true -> "SI"
                            false -> "NO"
                            null -> ""
                        },
                        referencia = fila.tiempoEsperadoMs,
                        unidad = "A / ms",
                        resultado = fila.resultado,
                        idInstrumento = estado.instrumentoReleSeleccionado.idInstrumento
                    )
                )
            }
        }

        val instrumentosUsados = mediciones.mapNotNull { medicion ->
            val id = medicion.idInstrumento ?: return@mapNotNull null
            (estado.instrumentosDisponiblesAislacion +
                    estado.instrumentosDisponiblesTtr +
                    estado.instrumentosDisponiblesDevanados +
                    estado.instrumentosDisponiblesRele)
                .firstOrNull { it.idInstrumento == id }
        }
        val fechaHoraPrueba = runCatching {
            val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ROOT).apply { isLenient = false }
            val fecha = formato.parse("${estado.fechaVisita} ${estado.horaInicio}") ?: return false
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(fecha)
        }.getOrElse {
            _uiState.update { current ->
                current.copy(mensajeErrorFormulario = "La fecha u hora de la prueba no tienen un formato válido.")
            }
            return false
        }
        val instrumentoVencido = instrumentosUsados.firstOrNull { !validarInstrumentoVigente(it) }
        if (instrumentoVencido != null) {
            _uiState.update {
                it.copy(
                    mensajeErrorFormulario = "La calibración del instrumento ${instrumentoVencido.modelo} " +
                            "(S/N: ${instrumentoVencido.numeroSerie}) venció el ${instrumentoVencido.fechaVencimiento}."
                )
            }
            return false
        }

        val fechaRecepcion = protocolosPendientesVisita.firstOrNull()?.fechaRecepcion
            ?: SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(Date())
        val registro = RegistroProtocolo(
                idEquipo = estado.idEquipo,
                numeroOt = estado.otNumero,
                fechaHora = fechaHoraPrueba,
                temperaturaC = temperatura,
                humedadPct = humedad,
                observaciones = if (estado.pruebaRealizada) estado.observaciones else
                    "NO REALIZADA: ${estado.motivoNoRealizada}",
                tipoVisita = estado.tipoVisita,
                descripcionMantenimiento = estado.descripcionMantenimiento,
                receptorNombre = estado.clienteRecibeNombre.trim(),
                receptorRut = estado.clienteRecibeRut.trim(),
                receptorCargo = estado.clienteRecibeCargo.trim(),
                fechaRecepcion = fechaRecepcion,
                idUsuarioRegistra = idUsuarioActivo,
                resultadoGlobal = if (estado.pruebaRealizada) estado.resultadoGlobal else "NO REALIZADA",
                estado = "REGISTRADO_TERRENO",
                mediciones = mediciones
            )

        if (agregarAVisita) {
            if (protocolosPendientesVisita.any { it.idEquipo == registro.idEquipo }) {
                _uiState.update {
                    it.copy(mensajeErrorFormulario = "Este equipo ya fue agregado a la visita.")
                }
                return false
            }
            if (usuarioVisitaPendiente != null && usuarioVisitaPendiente != idUsuarioActivo) {
                _uiState.update { it.copy(mensajeErrorFormulario = "La visita fue iniciada por otro usuario.") }
                return false
            }
            protocolosPendientesVisita.add(registro)
            usuarioVisitaPendiente = idUsuarioActivo
            val equipos = db.listarEquiposPorInstalacion(estado.idInstalacionSeleccionada)
            val tagsPendientes = protocolosPendientesVisita.map { protocolo ->
                equipos.firstOrNull { it.idEquipo == protocolo.idEquipo }?.tag ?: protocolo.idEquipo.toString()
            }
            val filasAislacion = when (estado.tipoEquipo) {
                "Transformador" -> listOf(
                    MedicionAislacionUiState("AT – BT", "5 kV"),
                    MedicionAislacionUiState("AT – T", "5 kV"),
                    MedicionAislacionUiState("BT – T", "1 kV")
                )
                "Interruptor", "Celda MT" -> listOf(
                    MedicionAislacionUiState("Entrada – Salida", "5 kV"),
                    MedicionAislacionUiState("Fase – Tierra", "5 kV")
                )
                "Cable de poder" -> listOf(
                    MedicionAislacionUiState("Fase A – Tierra", "5 kV"),
                    MedicionAislacionUiState("Fase B – Tierra", "5 kV"),
                    MedicionAislacionUiState("Fase C – Tierra", "5 kV")
                )
                else -> emptyList()
            }
            _uiState.update {
                it.copy(
                    aislacionFilas = filasAislacion,
                    ttrFilas = if (estado.tipoEquipo == "Transformador") listOf(
                        MedicionTtrUiState("H1 – X1"),
                        MedicionTtrUiState("H2 – X2"),
                        MedicionTtrUiState("H3 – X3")
                    ) else emptyList(),
                    devanadoFilas = if (estado.tipoEquipo == "Transformador") listOf(
                        cl.duocuc.Induztek.model.MedicionDevanadoUiState("AT", unidad = "mΩ"),
                        cl.duocuc.Induztek.model.MedicionDevanadoUiState("BT", unidad = "mΩ")
                    ) else emptyList(),
                    contactosFilas = if (estado.tipoEquipo in listOf("Interruptor", "Celda MT")) listOf(
                        MedicionContactosUiState("Polo A"),
                        MedicionContactosUiState("Polo B"),
                        MedicionContactosUiState("Polo C")
                    ) else emptyList(),
                    releFilas = if (estado.tipoEquipo == "Relé de protección") {
                        listOf(MedicionReleUiState())
                    } else emptyList(),
                    observaciones = "",
                    motivoNoRealizada = "",
                    pruebaRealizada = true,
                    protocolosPendientesVisita = protocolosPendientesVisita.size,
                    tagsPendientesVisita = tagsPendientes,
                    idProtocoloRegistrado = null,
                    mensajeNotificacionExito = "${tagsPendientes.last()} agregado a la visita."
                )
            }
            recalcularGlobal()
            return true
        }

        if (protocolosPendientesVisita.isNotEmpty()) {
            _uiState.update {
                it.copy(mensajeErrorFormulario = "Cierre la visita pendiente o agregue este equipo antes de iniciar otra visita.")
            }
            return false
        }

        val idInsertado = db.registrarProtocoloLocal(registro)

        val exito = idInsertado > 0
        if (exito) {
            recargarHistorial(estado.idEquipo)
            val otNotif = estado.otNumero
            onLimpiar()
            _uiState.update {
                it.copy(
                    mensajeNotificacionExito = "¡Protocolo de la OT-$otNotif registrado exitosamente!",
                    idProtocoloRegistrado = idInsertado
                )
            }
        } else {
            _uiState.update {
                it.copy(mensajeErrorFormulario = "Error al guardar el protocolo en la base de datos.")
            }
        }
        return exito
    }

    fun cerrarVisita(idUsuarioActivo: Long): Boolean {
        if (protocolosPendientesVisita.isEmpty()) {
            _uiState.update { it.copy(mensajeErrorFormulario = "Agregue al menos un protocolo antes de cerrar la visita.") }
            return false
        }
        if (protocolosPendientesVisita.any { it.idUsuarioRegistra != idUsuarioActivo }) {
            _uiState.update { it.copy(mensajeErrorFormulario = "El usuario activo no coincide con quien inició esta visita.") }
            return false
        }
        val ids = db.registrarProtocolosVisitaLocal(protocolosPendientesVisita.toList())
        if (ids.size != protocolosPendientesVisita.size) {
            _uiState.update {
                it.copy(mensajeErrorFormulario = "No se pudo guardar la visita completa. Revise instalaciones e instrumentos.")
            }
            return false
        }
        ids.forEach { id ->
            db.obtenerProtocoloPorId(id)?.let { protocolo ->
                NotificacionesLocales.notificarEstadoProtocolo(
                    getApplication(),
                    protocolo.numeroProtocolo,
                    protocolo.estado
                )
            }
        }
        val idEquipo = protocolosPendientesVisita.first().idEquipo
        val numeroOt = protocolosPendientesVisita.first().numeroOt
        protocolosPendientesVisita.clear()
        usuarioVisitaPendiente = null
        recargarHistorial(idEquipo)
        onLimpiar()
        _uiState.update {
            it.copy(
                idProtocoloRegistrado = ids.last(),
                protocolosPendientesVisita = 0,
                tagsPendientesVisita = emptyList(),
                mensajeNotificacionExito = "Visita OT-$numeroOt registrada con ${ids.size} protocolo(s)."
            )
        }
        return true
    }

    fun onDevanadoCambia(index: Int, uStr: String, vStr: String, wStr: String) {
        val lista = _uiState.value.devanadoFilas.toMutableList()
        if (index in lista.indices) {
            lista[index] = lista[index].copy(
                faseU = uStr,
                faseV = vStr,
                faseW = wStr
            )
            _uiState.update { it.copy(devanadoFilas = lista, mensajeErrorFormulario = null) }
            recalcularGlobal()
        }
    }

    fun onDevanadoResultadoCambia(index: Int, resultado: String) {
        val lista = _uiState.value.devanadoFilas.toMutableList()
        if (index !in lista.indices || resultado !in setOf("APROBADO", "OBSERVADO", "RECHAZADO")) return
        lista[index] = lista[index].copy(resultadoManual = resultado)
        _uiState.update { it.copy(devanadoFilas = lista, mensajeErrorFormulario = null) }
        recalcularGlobal()
    }

    fun onFormalizarProtocoloDirecto(idProtocolo: Long, idUsuarioActivo: Long): Boolean {
        val exito = db.cambiarEstadoProtocolo(
            idProtocolo = idProtocolo,
            nuevoEstado = "FORMALIZADO",
            idUsuario = idUsuarioActivo,
            motivo = "Aprobado y formalizado oficialmente"
        )
        if (exito) {
            db.obtenerProtocoloPorId(idProtocolo)?.let { protocolo ->
                NotificacionesLocales.notificarEstadoProtocolo(
                    getApplication(),
                    protocolo.numeroProtocolo,
                    protocolo.estado
                )
            }
            _uiState.update { it.copy(mensajeNotificacionExito = "¡Protocolo formalizado exitosamente en BD!") }
        }
        return exito
    }

    fun descartarNotificacionExito() {
        _uiState.update { it.copy(mensajeNotificacionExito = null) }
    }

    fun onLimpiar() {
        if (protocolosPendientesVisita.isNotEmpty()) return
        val usuario = usuarioCatalogoActual
        val horaActual = obtenerHoraActual()
        val fechaActual = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        _uiState.value = FormularioProtocoloUiState(
            horaInicio = horaActual,
            fechaVisita = fechaActual
        )
        if (usuario != null) {
            inicializarParaUsuario(usuario)
        } else {
            _uiState.update {
                it.copy(
                    clientesDisponibles = db.listarClientes().map { cliente -> cliente.razonSocial }
                )
            }
        }
    }

    override fun onCleared() {
        db.close()
        super.onCleared()
    }
}