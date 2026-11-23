package cl.duocuc.Induztek.viewmodel

import android.app.Application
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duocuc.Induztek.data.remote.ApiRequestException
import cl.duocuc.Induztek.data.remote.InduztekRepository
import cl.duocuc.Induztek.data.remote.MedicionRequest
import cl.duocuc.Induztek.data.remote.ProtocoloPendienteRequest
import cl.duocuc.Induztek.data.remote.ProtocoloRequest
import cl.duocuc.Induztek.data.InduztekDbHelper
import cl.duocuc.Induztek.model.FormularioProtocoloUiState
import cl.duocuc.Induztek.model.InstrumentoItem
import cl.duocuc.Induztek.model.ClienteCatalogoLocal
import cl.duocuc.Induztek.model.CuentaEmpresaLocal
import cl.duocuc.Induztek.model.InstalacionCatalogoLocal
import cl.duocuc.Induztek.model.InstalacionAsignacionLocal
import cl.duocuc.Induztek.model.EquipoCatalogoLocal
import cl.duocuc.Induztek.model.TipoEquipoCatalogoLocal
import cl.duocuc.Induztek.model.CrearEquipoResultado
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
import cl.duocuc.Induztek.model.ValidarRut
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.google.gson.JsonObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

class ProtocoloViewModel(application: Application) : AndroidViewModel(application) {

    private val db = InduztekDbHelper(application)
    private val repositorioRemoto = InduztekRepository(application)

    private val _uiState = MutableStateFlow(FormularioProtocoloUiState())
    val uiState: StateFlow<FormularioProtocoloUiState> = _uiState.asStateFlow()

    private val _historial = MutableStateFlow<List<ProtocoloTerreno>>(emptyList())
    val historial: StateFlow<List<ProtocoloTerreno>> = _historial.asStateFlow()
    private val _errorLecturaRemota = MutableStateFlow<String?>(null)
    val errorLecturaRemota: StateFlow<String?> = _errorLecturaRemota.asStateFlow()
    private val protocolosPendientesVisita = mutableListOf<RegistroProtocolo>()
    private var usuarioVisitaPendiente: Long? = null
    private var usuarioCatalogoActual: Usuario? = null
    private var empresasRemotas = emptyList<ClienteCatalogoLocal>()
    private var instalacionesRemotas = emptyList<InstalacionAsignacionLocal>()
    private var equiposRemotos = emptyList<EquipoCatalogoLocal>()

    fun limpiarProtocoloRegistrado() {
        _uiState.update { it.copy(idProtocoloRegistrado = null) }
    }

    fun inicializarParaUsuario(usuario: Usuario) {
        usuarioCatalogoActual = usuario
        _historial.value = emptyList()
        protocolosRemotos = emptyList()
        _errorLecturaRemota.value = null
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
                tecnicoNombre = if (usuario.rolCodigo.equals(Rol.CODIGO_TECNICO, ignoreCase = true)) {
                    usuario.nombre
                } else {
                    ""
                },
                ingenieroNombre = "",
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
                    clienteRecibeNombre = "",
                    clienteRecibeRut = "",
                    clienteRecibeCargo = "",
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
        cargarCatalogosRemotos(usuario)
    }

    private fun cargarCatalogosRemotos(usuario: Usuario) {
        viewModelScope.launch {
            empresasRemotas = emptyList()
            instalacionesRemotas = emptyList()
            equiposRemotos = emptyList()
            var errorLectura: String? = null

            try {
                val empresasApi = repositorioRemoto.empresas()
                empresasRemotas = empresasApi.mapNotNull { empresa ->
                    val id = valorLong(empresa, "idEmpresa", "id") ?: return@mapNotNull null
                    val nombre = valorTexto(empresa, "razonSocial", "nombre", "empresa")
                    if (nombre.isBlank()) return@mapNotNull null
                    ClienteCatalogoLocal(id, nombre)
                }
            } catch (error: ApiRequestException) {
                errorLectura = "No se pudieron actualizar las empresas: ${error.message}"
            } catch (error: java.io.IOException) {
                errorLectura = "No se pudieron actualizar las empresas: ${error.message}"
            } catch (error: RuntimeException) {
                errorLectura = "No se pudieron interpretar las empresas recibidas."
            }

            try {
                val instalacionesApi = when {
                    usuario.rolCodigo.equals(Rol.CODIGO_CLIENTE, ignoreCase = true) ->
                        repositorioRemoto.instalacionesCliente(usuario.idUsuario)
                    usuario.rolCodigo.equals(Rol.CODIGO_TECNICO, ignoreCase = true) ->
                        repositorioRemoto.instalacionesTecnico(usuario.idUsuario)
                    else -> repositorioRemoto.instalaciones()
                }
                instalacionesRemotas = instalacionesApi.mapNotNull { mapearInstalacionAsignacion(it) }
                actualizarCatalogosDeProtocolo(usuario)
            } catch (error: ApiRequestException) {
                errorLectura = unirErrores(errorLectura, "No se pudieron actualizar las instalaciones: ${error.message}")
            } catch (error: java.io.IOException) {
                errorLectura = unirErrores(errorLectura, "No se pudieron actualizar las instalaciones: ${error.message}")
            } catch (error: RuntimeException) {
                errorLectura = unirErrores(errorLectura, "No se pudieron interpretar las instalaciones recibidas.")
            }

            try {
                cargarInstrumentosRemotos()
            } catch (error: ApiRequestException) {
                errorLectura = unirErrores(errorLectura, "No se pudieron actualizar los instrumentos: ${error.message}")
            } catch (error: java.io.IOException) {
                errorLectura = unirErrores(errorLectura, "No se pudieron actualizar los instrumentos: ${error.message}")
            } catch (error: RuntimeException) {
                errorLectura = unirErrores(errorLectura, "No se pudieron interpretar los instrumentos recibidos.")
            }

            if (errorLectura == null) {
                _errorLecturaRemota.value = null
            } else {
                mostrarErrorLectura(errorLectura)
            }
        }
    }

    private fun unirErrores(actual: String?, nuevo: String): String {
        if (actual.isNullOrBlank()) return nuevo
        return "$actual\n$nuevo"
    }

    private fun actualizarCatalogosDeProtocolo(usuario: Usuario) {
        var empresas = empresasRemotas
        if (usuario.rolCodigo.equals(Rol.CODIGO_CLIENTE, ignoreCase = true)) {
            val idEmpresa = usuario.idEmpresaCliente
            empresas = empresas.filter { it.idEmpresa == idEmpresa }
        } else if (usuario.rolCodigo.equals(Rol.CODIGO_TECNICO, ignoreCase = true)) {
            val nombres = instalacionesRemotas.map { it.empresa }.filter { it.isNotBlank() }.toSet()
            empresas = empresas.filter { empresa -> nombres.contains(empresa.razonSocial) }
            if (empresas.isEmpty()) {
                empresas = instalacionesRemotas.mapNotNull { instalacion ->
                    val empresa = empresasRemotas.firstOrNull {
                        it.razonSocial.equals(instalacion.empresa, ignoreCase = true)
                    }
                    empresa
                }.distinctBy { it.idEmpresa }
            }
        }

        _uiState.update { actual ->
            actual.copy(
                clientesDisponibles = empresas.map { it.razonSocial },
                instalacionesDisponibles = instalacionesRemotas.map { it.nombre },
                idEmpresaSeleccionada = empresas.firstOrNull()?.idEmpresa ?: actual.idEmpresaSeleccionada,
                idInstalacionSeleccionada = instalacionesRemotas.firstOrNull()?.idInstalacion
                    ?: actual.idInstalacionSeleccionada
            )
        }
        if (empresas.isNotEmpty()) {
            onClienteSeleccionado(empresas.first().razonSocial)
        } else if (instalacionesRemotas.isNotEmpty()) {
            val instalacion = instalacionesRemotas.first()
            _uiState.update {
                it.copy(
                    instalacionNombre = instalacion.nombre,
                    idInstalacionSeleccionada = instalacion.idInstalacion
                )
            }
            cargarEquiposRemotos(instalacion.idInstalacion, usuario)
        }
    }

    private suspend fun cargarInstrumentosRemotos() {
        val tipos = listOf("AISLACION", "TTR", "DEVANADOS", "RELE")
        for (tipo in tipos) {
            val respuesta = repositorioRemoto.instrumentos(tipo)
            val instrumentos = respuesta.mapNotNull { instrumento ->
                val id = valorLong(instrumento, "idInstrumento", "id") ?: return@mapNotNull null
                val modelo = valorTexto(instrumento, "modelo", "nombre")
                val serie = valorTexto(instrumento, "numeroSerie", "serie")
                if (modelo.isBlank() && serie.isBlank()) return@mapNotNull null
                InstrumentoItem(
                    idInstrumento = id,
                    modelo = modelo.ifBlank { "Instrumento $id" },
                    numeroSerie = serie,
                    fechaVencimiento = valorTexto(
                        instrumento,
                        "fechaVencimiento",
                        "calibracionVencimiento",
                        "fechaProximaCalibracion"
                    ),
                    nombre = valorTexto(instrumento, "nombre").ifBlank { modelo }
                )
            }
            _uiState.update { actual ->
                when (tipo) {
                    "AISLACION" -> actual.copy(instrumentosDisponiblesAislacion = instrumentos)
                    "TTR" -> actual.copy(instrumentosDisponiblesTtr = instrumentos)
                    "DEVANADOS" -> actual.copy(instrumentosDisponiblesDevanados = instrumentos)
                    else -> actual.copy(instrumentosDisponiblesRele = instrumentos)
                }
            }
        }
    }

    private fun cargarEquiposRemotos(idInstalacion: Long, usuario: Usuario?) {
        viewModelScope.launch {
            try {
                val equiposApi = when {
                    usuario?.rolCodigo.equals(Rol.CODIGO_CLIENTE, ignoreCase = true) ->
                        repositorioRemoto.equiposCliente(usuario?.idUsuario ?: return@launch)
                    usuario?.rolCodigo.equals(Rol.CODIGO_TECNICO, ignoreCase = true) ->
                        repositorioRemoto.equiposTecnico(usuario?.idUsuario ?: return@launch)
                    else -> repositorioRemoto.equiposInstalacion(idInstalacion)
                }
                equiposRemotos = equiposApi.mapNotNull { mapearEquipo(it, idInstalacion) }
                if (_uiState.value.idInstalacionSeleccionada == idInstalacion) {
                    val tags = equiposRemotos.map { it.tag }
                    _uiState.update { it.copy(tagsDisponibles = tags) }
                    tags.firstOrNull()?.let { onTagEquipoSeleccionado(it) }
                }
                _errorLecturaRemota.value = null
            } catch (error: ApiRequestException) {
                mostrarErrorLectura("No se pudieron actualizar los equipos: ${error.message}")
            } catch (error: java.io.IOException) {
                mostrarErrorLectura("No se pudieron actualizar los equipos: ${error.message}")
            } catch (error: RuntimeException) {
                mostrarErrorLectura("No se pudieron interpretar los equipos recibidos.")
            }
        }
    }

    private fun mostrarErrorLectura(mensaje: String) {
        _errorLecturaRemota.value = mensaje
        _uiState.update { it.copy(mensajeErrorFormulario = mensaje) }
    }

    private fun mapearInstalacionAsignacion(registro: JsonObject): InstalacionAsignacionLocal? {
        val id = valorLong(registro, "idInstalacion", "id") ?: return null
        val nombre = valorTexto(registro, "nombre", "nombreInstalacion")
        if (nombre.isBlank()) return null
        val empresaJson = registro.get("empresa")
        val empresaAnidada = if (empresaJson != null && empresaJson.isJsonObject) {
            valorTexto(empresaJson.asJsonObject, "razonSocial", "nombre")
        } else {
            ""
        }
        var empresaId = valorLong(registro, "idEmpresa", "empresaId")
        if (empresaId == null && empresaJson != null && empresaJson.isJsonObject) {
            empresaId = valorLong(empresaJson.asJsonObject, "idEmpresa", "id")
        }
        val empresaPorId = empresasRemotas.firstOrNull { it.idEmpresa == empresaId }
        var nombreEmpresa = valorTexto(registro, "empresaNombre", "nombreEmpresa", "razonSocial")
        if (nombreEmpresa.isBlank()) {
            nombreEmpresa = empresaAnidada
        }
        if (nombreEmpresa.isBlank()) {
            nombreEmpresa = empresaPorId?.razonSocial.orEmpty()
        }
        return InstalacionAsignacionLocal(id, nombre, nombreEmpresa)
    }

    private fun idInstalacionDeAsignacion(registro: JsonObject): Long? {
        val directo = valorLong(registro, "idInstalacion")
        if (directo != null) return directo
        val instalacionJson = registro.get("instalacion")
        if (instalacionJson == null || !instalacionJson.isJsonObject) return null
        return valorLong(instalacionJson.asJsonObject, "idInstalacion", "id")
    }

    private fun mapearEquipo(registro: JsonObject, idInstalacion: Long): EquipoCatalogoLocal? {
        val id = valorLong(registro, "idEquipo", "id") ?: return null
        val tipoJson = registro.get("tipoEquipo")
        val tipoAnidado = if (tipoJson != null && tipoJson.isJsonObject) {
            valorTexto(tipoJson.asJsonObject, "nombre", "tipo")
        } else {
            ""
        }
        return EquipoCatalogoLocal(
            idEquipo = id,
            idInstalacion = valorLong(registro, "idInstalacion") ?: idInstalacion,
            tag = valorTexto(registro, "tag", "codigo"),
            tipo = valorTexto(registro, "tipo", "nombreTipo").ifBlank { tipoAnidado },
            clasificacion = valorTexto(registro, "clasificacion"),
            marca = valorTexto(registro, "marca"),
            modelo = valorTexto(registro, "modelo"),
            numeroSerie = valorTexto(registro, "numeroSerie", "serie"),
            potenciaKva = valorDouble(registro, "potenciaKva"),
            tensiones = valorTexto(registro, "tensiones"),
            anoFabricacion = valorTexto(registro, "anoFabricacion"),
            posicionTap = valorTexto(registro, "posicionTap"),
            grupoConexion = valorTexto(registro, "grupoConexion"),
            resistenciaContactoMaxUohm = valorDouble(registro, "resistenciaContactoMaxUohm")
        )
    }

    private fun valorTexto(registro: JsonObject, vararg claves: String): String {
        for (clave in claves) {
            val valor = registro.get(clave)
            if (valor != null && !valor.isJsonNull && valor.isJsonPrimitive) {
                return valor.asString
            }
        }
        return ""
    }

    private fun valorLong(registro: JsonObject, vararg claves: String): Long? {
        for (clave in claves) {
            val valor = registro.get(clave)
            if (valor == null || valor.isJsonNull) continue
            val numero = valor.asString.toLongOrNull()
            if (numero != null) return numero
        }
        return null
    }

    private fun valorDouble(registro: JsonObject, vararg claves: String): Double? {
        for (clave in claves) {
            val valor = registro.get(clave)
            if (valor == null || valor.isJsonNull) continue
            val numero = valor.asString.toDoubleOrNull()
            if (numero != null) return numero
        }
        return null
    }

    fun tecnicosParaAsignacion(usuario: Usuario?): List<Usuario> =
        if (puedeAdministrarAsignaciones(usuario)) {
            db.listarTecnicosActivos()
        } else {
            emptyList()
        }

    fun clientesConEmpresaParaInstalaciones(usuario: Usuario?): List<CuentaEmpresaLocal> =
        if (puedeAdministrarAsignaciones(usuario) && usuario != null) {
            db.listarCuentasClienteConEmpresa(usuario.idUsuario)
        } else {
            emptyList()
        }

    fun instalacionesDeClienteParaGestion(
        idCliente: Long,
        usuario: Usuario?
    ): List<InstalacionCatalogoLocal> =
        if (puedeAdministrarAsignaciones(usuario)) {
            db.listarInstalacionesDeCuentaCliente(idCliente)
        } else {
            emptyList()
        }

    suspend fun leerInstalacionesDeClienteParaGestion(
        idCliente: Long,
        usuario: Usuario?
    ): List<InstalacionCatalogoLocal> {
        val locales = instalacionesDeClienteParaGestion(idCliente, usuario)
        if (locales.isEmpty() && !puedeAdministrarAsignaciones(usuario)) return emptyList()
        try {
            val remoto = repositorioRemoto.instalacionesCliente(idCliente)
            val instalaciones = remoto.mapNotNull { registro ->
                val id = valorLong(registro, "idInstalacion", "id") ?: return@mapNotNull null
                val nombre = valorTexto(registro, "nombre", "nombreInstalacion")
                if (nombre.isBlank()) return@mapNotNull null
                InstalacionCatalogoLocal(id, nombre)
            }
            _errorLecturaRemota.value = null
            return instalaciones
        } catch (error: ApiRequestException) {
            mostrarErrorLectura("No se pudieron cargar las instalaciones: ${error.message}")
            return locales
        } catch (error: java.io.IOException) {
            mostrarErrorLectura("No se pudieron cargar las instalaciones: ${error.message}")
            return locales
        } catch (error: RuntimeException) {
            mostrarErrorLectura("No se pudieron interpretar las instalaciones recibidas.")
            return locales
        }
    }

    suspend fun leerEquiposDeInstalacionParaGestion(
        idInstalacion: Long,
        usuario: Usuario?
    ): List<EquipoCatalogoLocal> {
        val locales = equiposDeInstalacionParaGestion(idInstalacion, usuario)
        if (locales.isEmpty() && !puedeAdministrarAsignaciones(usuario)) return emptyList()
        try {
            val remoto = repositorioRemoto.equiposInstalacion(idInstalacion)
            val equipos = remoto.mapNotNull { mapearEquipo(it, idInstalacion) }
            _errorLecturaRemota.value = null
            return equipos
        } catch (error: ApiRequestException) {
            mostrarErrorLectura("No se pudieron cargar los equipos: ${error.message}")
            return locales
        } catch (error: java.io.IOException) {
            mostrarErrorLectura("No se pudieron cargar los equipos: ${error.message}")
            return locales
        } catch (error: RuntimeException) {
            mostrarErrorLectura("No se pudieron interpretar los equipos recibidos.")
            return locales
        }
    }

    fun crearInstalacionParaCliente(
        idCliente: Long,
        nombre: String,
        usuario: Usuario?
    ): Boolean {
        val responsable = usuario ?: return false
        if (!puedeAdministrarAsignaciones(responsable)) return false
        val creada = db.crearInstalacionAdministrativa(responsable.idUsuario, idCliente, nombre)
        if (!creada) {
            return false
        }
        val cuenta = db.listarCuentasClienteConEmpresa(responsable.idUsuario)
            .firstOrNull { it.idUsuario == idCliente }
        if (cuenta != null) {
            val instalacionLocal = db.listarInstalacionesDeCuentaCliente(idCliente)
                .firstOrNull { it.nombre.equals(nombre, ignoreCase = true) }
            val solicitud = JsonObject()
            solicitud.addProperty("idEmpresa", cuenta.idEmpresa)
            solicitud.addProperty("idUsuarioCliente", idCliente)
            solicitud.addProperty("nombre", nombre)
            solicitud.addProperty("activa", true)
            viewModelScope.launch {
                try {
                    repositorioRemoto.crearInstalacion(solicitud, instalacionLocal?.idInstalacion)
                } catch (error: ApiRequestException) {
                    _uiState.update {
                        it.copy(mensajeErrorFormulario = "Sincronización: ${error.message}")
                    }
                } catch (_: java.io.IOException) {
                    _uiState.update {
                        it.copy(
                            mensajeErrorFormulario = "Sincronización: no se pudo enviar la instalación al servidor."
                        )
                    }
                }
            }
        }
        return true
    }

    fun tiposEquipoParaGestion(usuario: Usuario?): List<TipoEquipoCatalogoLocal> =
        if (puedeAdministrarAsignaciones(usuario)) db.listarTiposEquipo() else emptyList()

    fun equiposDeInstalacionParaGestion(
        idInstalacion: Long,
        usuario: Usuario?
    ): List<EquipoCatalogoLocal> =
        if (puedeAdministrarAsignaciones(usuario)) {
            db.listarEquiposPorInstalacion(idInstalacion)
        } else {
            emptyList()
        }

    fun crearEquipoParaInstalacion(
        idInstalacion: Long,
        idTipoEquipo: Long,
        tag: String,
        marca: String,
        modelo: String,
        numeroSerie: String,
        potenciaKva: Double?,
        tensiones: String,
        anoFabricacion: String,
        posicionTap: String,
        grupoConexion: String,
        resistenciaContactoMaxUohm: Double?,
        idCliente: Long,
        usuario: Usuario?
    ): CrearEquipoResultado {
        val responsable = usuario ?: return CrearEquipoResultado(
            campoError = "general",
            mensajeError = "Inicia sesión con un perfil autorizado para registrar equipos."
        )
        if (!puedeAdministrarAsignaciones(responsable)) {
            return CrearEquipoResultado(
                campoError = "general",
                mensajeError = "Solo Ingeniería o Jefatura Técnica pueden registrar equipos."
            )
        }
        val resultado = db.crearEquipoAdministrativo(
            actorId = responsable.idUsuario,
            idInstalacion = idInstalacion,
            idTipoEquipo = idTipoEquipo,
            tag = tag,
            marca = marca,
            modelo = modelo,
            numeroSerie = numeroSerie,
            potenciaKva = potenciaKva,
            tensiones = tensiones,
            anoFabricacion = anoFabricacion,
            posicionTap = posicionTap,
            grupoConexion = grupoConexion,
            resistenciaContactoMaxUohm = resistenciaContactoMaxUohm
        )
        if (!resultado.exitosa) {
            return resultado
        }

        val instalacionLocal = db.listarInstalacionesParaAsignacion()
            .firstOrNull { it.idInstalacion == idInstalacion }
        val tipoEquipo = db.listarTiposEquipo().firstOrNull { it.idTipoEquipo == idTipoEquipo }
        val instalacionEmpresa = db.listarClientes()
            .firstOrNull { it.razonSocial == instalacionLocal?.empresa }
        val solicitud = JsonObject()
        solicitud.addProperty("codigo", tag)
        solicitud.addProperty("tag", tag)
        solicitud.addProperty("tipo", tipoEquipo?.nombre.orEmpty())
        solicitud.addProperty("clasificacion", tipoEquipo?.clasificacion.orEmpty())
        solicitud.addProperty("marca", marca)
        solicitud.addProperty("modelo", modelo)
        solicitud.addProperty("numeroSerie", numeroSerie)
        solicitud.addProperty("potenciaKva", potenciaKva)
        solicitud.addProperty("tensiones", tensiones)
        solicitud.addProperty("anoFabricacion", anoFabricacion)
        solicitud.addProperty("posicionTap", posicionTap)
        solicitud.addProperty("grupoConexion", grupoConexion)
        solicitud.addProperty("resistenciaContactoMaxUohm", resistenciaContactoMaxUohm)
        solicitud.addProperty("instalacion", instalacionLocal?.nombre.orEmpty())
        solicitud.addProperty("idInstalacion", idInstalacion)
        solicitud.addProperty("idCliente", idCliente)
        if (instalacionEmpresa == null) {
            _uiState.update {
                it.copy(
                    mensajeErrorFormulario = "Sincronización: el equipo quedó guardado localmente, pero no se pudo resolver su empresa."
                )
            }
        } else {
            viewModelScope.launch {
                try {
                    repositorioRemoto.crearEquipo(solicitud, resultado.idEquipo)
                } catch (error: ApiRequestException) {
                    _uiState.update {
                        it.copy(mensajeErrorFormulario = "Sincronización: ${error.message}")
                    }
                } catch (_: java.io.IOException) {
                    _uiState.update {
                        it.copy(
                            mensajeErrorFormulario = "Sincronización: no se pudo enviar el equipo al servidor."
                        )
                    }
                }
            }
        }
        return resultado
    }

    fun instalacionesParaAsignacion(usuario: Usuario?) =
        if (puedeAdministrarAsignaciones(usuario)) {
            db.listarInstalacionesParaAsignacion()
        } else {
            emptyList()
        }

    suspend fun leerInstalacionesParaAsignacion(usuario: Usuario?): List<InstalacionAsignacionLocal> {
        val locales = instalacionesParaAsignacion(usuario)
        if (!puedeAdministrarAsignaciones(usuario)) return emptyList()
        try {
            val respuesta = repositorioRemoto.instalaciones()
            val instalaciones = respuesta.mapNotNull { mapearInstalacionAsignacion(it) }
            _errorLecturaRemota.value = null
            return instalaciones
        } catch (error: ApiRequestException) {
            mostrarErrorLectura("No se pudieron cargar las instalaciones para asignar: ${error.message}")
            return locales
        } catch (error: java.io.IOException) {
            mostrarErrorLectura("No se pudieron cargar las instalaciones para asignar: ${error.message}")
            return locales
        } catch (error: RuntimeException) {
            mostrarErrorLectura("No se pudieron interpretar las instalaciones recibidas.")
            return locales
        }
    }

    fun idsAsignadosATecnico(idTecnico: Long, usuario: Usuario?): Set<Long> =
        if (puedeAdministrarAsignaciones(usuario)) {
            db.listarIdsInstalacionesTecnico(idTecnico)
        } else {
            emptySet()
        }

    suspend fun leerIdsAsignadosATecnico(idTecnico: Long, usuario: Usuario?): Set<Long> {
        val locales = idsAsignadosATecnico(idTecnico, usuario)
        if (!puedeAdministrarAsignaciones(usuario)) return emptySet()
        try {
            val respuesta = repositorioRemoto.asignacionesTecnico(idTecnico)
            val ids = respuesta.mapNotNull { registro ->
                idInstalacionDeAsignacion(registro)
            }.toSet()
            _errorLecturaRemota.value = null
            return ids
        } catch (error: ApiRequestException) {
            mostrarErrorLectura("No se pudieron cargar las asignaciones: ${error.message}")
            return locales
        } catch (error: java.io.IOException) {
            mostrarErrorLectura("No se pudieron cargar las asignaciones: ${error.message}")
            return locales
        } catch (error: RuntimeException) {
            mostrarErrorLectura("No se pudieron interpretar las asignaciones recibidas.")
            return locales
        }
    }

    fun instalacionesAsignadasAOtrosTecnicos(
        idTecnico: Long,
        usuario: Usuario?
    ): Map<Long, String> =
        if (puedeAdministrarAsignaciones(usuario)) {
            db.listarTecnicosPropietariosDeInstalaciones(idTecnico)
        } else {
            emptyMap()
        }

    suspend fun leerInstalacionesAsignadasAOtrosTecnicos(
        idTecnico: Long,
        usuario: Usuario?
    ): Map<Long, String> {
        val locales = instalacionesAsignadasAOtrosTecnicos(idTecnico, usuario)
        if (!puedeAdministrarAsignaciones(usuario)) return emptyMap()
        try {
            val asignaciones = mutableMapOf<Long, String>()
            val tecnicos = db.listarTecnicosActivos()
            for (tecnico in tecnicos) {
                if (tecnico.idUsuario == idTecnico) continue
                val respuesta = repositorioRemoto.asignacionesTecnico(tecnico.idUsuario)
                for (registro in respuesta) {
                    val idInstalacion = idInstalacionDeAsignacion(registro)
                    if (idInstalacion != null) {
                        asignaciones[idInstalacion] = tecnico.nombre
                    }
                }
            }
            _errorLecturaRemota.value = null
            return asignaciones
        } catch (error: ApiRequestException) {
            mostrarErrorLectura("No se pudieron cargar las asignaciones existentes: ${error.message}")
            return locales
        } catch (error: java.io.IOException) {
            mostrarErrorLectura("No se pudieron cargar las asignaciones existentes: ${error.message}")
            return locales
        } catch (error: RuntimeException) {
            mostrarErrorLectura("No se pudieron interpretar las asignaciones recibidas.")
            return locales
        }
    }

    private fun puedeAdministrarAsignaciones(usuario: Usuario?): Boolean =
        usuario?.rolCodigo.equals(Rol.CODIGO_JEFE_TECNICO, ignoreCase = true) ||
                usuario?.rolCodigo.equals(Rol.CODIGO_INGENIERO, ignoreCase = true)

    fun guardarAsignacionesTecnico(
        idTecnico: Long,
        idsInstalaciones: Set<Long>,
        usuario: Usuario?
    ): Boolean {
        val responsable = usuario ?: return false
        if (!puedeAdministrarAsignaciones(responsable)) return false
        val guardadas = db.guardarAsignacionesTecnico(
            idTecnico,
            idsInstalaciones,
            responsable.idUsuario
        )
        if (guardadas) {
            viewModelScope.launch {
                try {
                    repositorioRemoto.actualizarAsignaciones(
                        idTecnico,
                        responsable.idUsuario,
                        idsInstalaciones.toList()
                    )
                } catch (error: ApiRequestException) {
                    _uiState.update {
                        it.copy(mensajeErrorFormulario = "Sincronización: ${error.message}")
                    }
                } catch (_: java.io.IOException) {
                    _uiState.update {
                        it.copy(
                            mensajeErrorFormulario = "Sincronización: no se pudieron enviar las asignaciones."
                        )
                    }
                }
            }
        }
        return guardadas
    }

    private fun obtenerHoraActual(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    fun onClienteSeleccionado(cliente: String) {
        val empresaLocal = db.listarClientes().firstOrNull { it.razonSocial == cliente }
        val empresa = empresaLocal ?: empresasRemotas.firstOrNull {
            it.razonSocial.equals(cliente, ignoreCase = true)
        } ?: return
        val usuario = usuarioCatalogoActual
        if (
            usuario?.rolCodigo.equals(Rol.CODIGO_CLIENTE, ignoreCase = true) &&
            empresa.idEmpresa != usuario?.idEmpresaCliente
        ) return
        val instalacionesLocales = if (
            usuario != null && usuario.rolCodigo.equals(Rol.CODIGO_TECNICO, ignoreCase = true)
        ) {
            db.listarInstalacionesTecnico(empresa.idEmpresa, usuario.idUsuario)
        } else {
            db.listarInstalacionesCliente(empresa.idEmpresa)
        }
        val instalacionesApi = if (usuario?.rolCodigo.equals(Rol.CODIGO_CLIENTE, ignoreCase = true)) {
            instalacionesRemotas
        } else {
            instalacionesRemotas.filter { instalacion ->
                instalacion.empresa.equals(cliente, ignoreCase = true) ||
                        (instalacionesRemotas.size == 1 && instalacion.empresa.isBlank())
            }
        }
        val instalaciones = if (instalacionesApi.isNotEmpty()) {
            instalacionesApi
        } else {
            instalacionesLocales.map {
                InstalacionAsignacionLocal(it.idInstalacion, it.nombre, cliente)
            }
        }
        val contacto = if (empresaLocal == null) null else db.buscarContactoEmpresa(empresa.idEmpresa)

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
        val instalacionesLocales = if (
            usuario != null && usuario.rolCodigo.equals(Rol.CODIGO_TECNICO, ignoreCase = true)
        ) {
            db.listarInstalacionesTecnico(estado.idEmpresaSeleccionada, usuario.idUsuario)
        } else {
            db.listarInstalacionesCliente(estado.idEmpresaSeleccionada)
        }
        val instalacionLocal = instalacionesLocales.firstOrNull { it.nombre == instalacion }
        val instalacionRemota = instalacionesRemotas.firstOrNull { it.nombre == instalacion }
        var idInstalacion = instalacionRemota?.idInstalacion
        if (idInstalacion == null) {
            idInstalacion = instalacionLocal?.idInstalacion
        }
        if (idInstalacion == null) return
        val equiposLocales = db.listarEquiposPorInstalacion(idInstalacion)
        val equiposDisponibles = if (equiposRemotos.any { it.idInstalacion == idInstalacion }) {
            equiposRemotos.filter { it.idInstalacion == idInstalacion }
        } else {
            equiposLocales
        }
        val tagsList = equiposDisponibles.map { it.tag }

        _uiState.update {
            it.copy(
                idInstalacionSeleccionada = idInstalacion,
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
        } else {
            cargarEquiposRemotos(idInstalacion, usuario)
        }
    }

    fun onTagEquipoSeleccionado(tag: String) {
        val estado = _uiState.value
        var equipo = db.listarEquiposPorInstalacion(estado.idInstalacionSeleccionada)
            .firstOrNull { it.tag == tag }
        if (equipo == null) {
            equipo = equiposRemotos.firstOrNull {
                it.idInstalacion == estado.idInstalacionSeleccionada && it.tag == tag
            }
        }
        if (equipo == null) return
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
        val usuario = usuarioCatalogoActual
        if (usuario == null) {
            _historial.value = db.listarProtocolosPorEquipo(idEquipo)
        } else {
            cargarHistorialEquipo(idEquipo, usuario)
        }
    }

    fun cargarHistorialEquipo(idEquipo: Long, usuario: Usuario?) {
        val usuarioActual = usuario
        if (usuarioActual == null) {
            _historial.value = emptyList()
            return
        }
        val protocolosLocales = protocolosLocalesVisibles(idEquipo, usuarioActual)
        _historial.value = protocolosLocales
        _errorLecturaRemota.value = null

        viewModelScope.launch {
            try {
                var respuesta = emptyList<JsonObject>()
                val codigoRol = usuarioActual.rolCodigo
                if (codigoRol.equals(Rol.CODIGO_CLIENTE, ignoreCase = true)) {
                    respuesta = repositorioRemoto.protocolosCliente(usuarioActual.idUsuario)
                    if (idEquipo != 0L) {
                        respuesta = respuesta.filter { protocolo ->
                            idEquipoDeProtocolo(protocolo) == idEquipo
                        }
                    }
                } else if (codigoRol.equals(Rol.CODIGO_TECNICO, ignoreCase = true)) {
                    if (idEquipo != 0L) {
                        val equiposTecnico = repositorioRemoto.equiposTecnico(usuarioActual.idUsuario)
                        val puedeVer = equiposTecnico.any {
                            valorLong(it, "idEquipo", "id") == idEquipo
                        }
                        if (!puedeVer && !db.tecnicoPuedeVerEquipo(usuarioActual.idUsuario, idEquipo)) {
                            _historial.value = emptyList()
                            _errorLecturaRemota.value = null
                            return@launch
                        }
                        respuesta = repositorioRemoto.protocolosEquipo(idEquipo)
                    } else {
                        val visitasTecnico = repositorioRemoto.visitas(null, usuarioActual.idUsuario)
                        val protocolosVisita = mutableListOf<JsonObject>()
                        for (visita in visitasTecnico) {
                            val idVisita = valorLong(visita, "idVisita", "id")
                            if (idVisita != null) {
                                protocolosVisita.addAll(repositorioRemoto.protocolosVisita(idVisita))
                            }
                        }
                        respuesta = protocolosVisita
                    }
                } else if (
                    codigoRol.equals(Rol.CODIGO_INGENIERO, ignoreCase = true) ||
                    codigoRol.equals(Rol.CODIGO_JEFE_TECNICO, ignoreCase = true)
                ) {
                    respuesta = if (idEquipo == 0L) {
                        repositorioRemoto.protocolos()
                    } else {
                        repositorioRemoto.protocolosEquipo(idEquipo)
                    }
                }

                val protocolos = respuesta.mapNotNull { mapearProtocoloRemoto(it) }
                protocolosRemotos = protocolos
                _historial.value = protocolos
                _errorLecturaRemota.value = null
                if (idEquipo != 0L) {
                    protocolos.firstOrNull()?.let { protocolo ->
                        _uiState.update {
                            it.copy(
                                tagEquipo = protocolo.tagEquipo,
                                instalacionNombre = protocolo.instalacionNombre
                            )
                        }
                    }
                }
            } catch (error: ApiRequestException) {
                _historial.value = protocolosLocales
                mostrarErrorLectura("No se pudo actualizar el historial: ${error.message}")
            } catch (error: java.io.IOException) {
                _historial.value = protocolosLocales
                mostrarErrorLectura("No se pudo actualizar el historial: ${error.message}")
            } catch (error: RuntimeException) {
                _historial.value = protocolosLocales
                mostrarErrorLectura("No se pudo interpretar el historial recibido.")
            }
        }
    }

    fun cargarProtocolosCliente(usuario: Usuario) {
        val idEmpresa = usuario.idEmpresaCliente
        val historialLocal = if (idEmpresa == null) emptyList() else db.listarProtocolosCliente(idEmpresa)
        _historial.value = historialLocal
        _errorLecturaRemota.value = null
        viewModelScope.launch {
            try {
                val respuesta = repositorioRemoto.protocolosCliente(usuario.idUsuario)
                val protocolos = respuesta.mapNotNull { mapearProtocoloRemoto(it) }
                protocolosRemotos = protocolos
                _historial.value = protocolos
                _errorLecturaRemota.value = null
            } catch (error: ApiRequestException) {
                _historial.value = historialLocal
                mostrarErrorLectura("No se pudo actualizar el portal de protocolos: ${error.message}")
            } catch (error: java.io.IOException) {
                _historial.value = historialLocal
                mostrarErrorLectura("No se pudo actualizar el portal de protocolos: ${error.message}")
            } catch (error: RuntimeException) {
                _historial.value = historialLocal
                mostrarErrorLectura("No se pudo interpretar el historial recibido.")
            }
        }
    }

    private var protocolosRemotos = emptyList<ProtocoloTerreno>()

    private fun protocolosLocalesVisibles(idEquipo: Long, usuario: Usuario): List<ProtocoloTerreno> {
        if (idEquipo != 0L) {
            if (usuario.rolCodigo.equals(Rol.CODIGO_TECNICO, ignoreCase = true)) {
                if (!db.tecnicoPuedeVerEquipo(usuario.idUsuario, idEquipo)) return emptyList()
                return db.listarProtocolosPorEquipo(idEquipo)
            }
            if (usuario.rolCodigo.equals(Rol.CODIGO_CLIENTE, ignoreCase = true)) {
                val idEmpresa = usuario.idEmpresaCliente ?: return emptyList()
                return db.listarProtocolosEquipoCliente(idEquipo, idEmpresa)
            }
            return db.listarProtocolosPorEquipo(idEquipo)
        }
        if (usuario.rolCodigo.equals(Rol.CODIGO_TECNICO, ignoreCase = true)) {
            return db.listarProtocolosTecnico(usuario.idUsuario)
        }
        if (usuario.rolCodigo.equals(Rol.CODIGO_CLIENTE, ignoreCase = true)) {
            val idEmpresa = usuario.idEmpresaCliente ?: return emptyList()
            return db.listarProtocolosCliente(idEmpresa)
        }
        if (
            usuario.rolCodigo.equals(Rol.CODIGO_INGENIERO, ignoreCase = true) ||
            usuario.rolCodigo.equals(Rol.CODIGO_JEFE_TECNICO, ignoreCase = true)
        ) {
            return db.listarProtocolosTodos()
        }
        return emptyList()
    }

    private fun mapearProtocoloRemoto(registro: JsonObject): ProtocoloTerreno? {
        val id = valorLong(registro, "idProtocolo", "id") ?: return null
        val equipoJson = registro.get("equipo")
        val equipo = if (equipoJson != null && equipoJson.isJsonObject) equipoJson.asJsonObject else null
        val instalacionJson = registro.get("instalacion")
        val instalacion = if (instalacionJson != null && instalacionJson.isJsonObject) {
            instalacionJson.asJsonObject
        } else {
            null
        }
        val tecnicoJson = registro.get("tecnico")
        val tecnico = if (tecnicoJson != null && tecnicoJson.isJsonObject) tecnicoJson.asJsonObject else null
        val clienteJson = registro.get("cliente")
        val cliente = if (clienteJson != null && clienteJson.isJsonObject) clienteJson.asJsonObject else null
        val fecha = valorFecha(registro, "fechaHora", "fechaHoraPrueba", "fechaRegistro", "createdAt")
        var idEquipo = valorLong(registro, "idEquipo")
        if (idEquipo == null && equipo != null) {
            idEquipo = valorLong(equipo, "idEquipo", "id")
        }
        if (idEquipo == null) {
            idEquipo = 0L
        }
        return ProtocoloTerreno(
            idProtocolo = id,
            numeroProtocolo = valorTexto(registro, "numeroProtocolo", "numero").ifBlank { "PT-$id" },
            idEquipo = idEquipo,
            tagEquipo = valorTextoAnidado(registro, "tagEquipo", equipo, "tag", "codigo"),
            tipoEquipo = valorTextoAnidado(registro, "tipoEquipo", equipo, "tipo", "tipoEquipo"),
            marcaEquipo = valorTextoAnidado(registro, "marcaEquipo", equipo, "marca"),
            modeloEquipo = valorTextoAnidado(registro, "modeloEquipo", equipo, "modelo"),
            numeroSerieEquipo = valorTextoAnidado(registro, "numeroSerieEquipo", equipo, "numeroSerie"),
            anoFabricacion = valorTextoAnidado(registro, "anoFabricacion", equipo, "anoFabricacion"),
            potenciaKva = valorTextoAnidado(registro, "potenciaKva", equipo, "potenciaKva"),
            tensiones = valorTextoAnidado(registro, "tensiones", equipo, "tensiones"),
            posicionTap = valorTextoAnidado(registro, "posicionTap", equipo, "posicionTap"),
            grupoConexion = valorTextoAnidado(registro, "grupoConexion", equipo, "grupoConexion"),
            clienteNombre = valorTextoAnidado(
                registro,
                "clienteNombre",
                cliente,
                "razonSocial",
                "nombre",
                clavesDirectasAlternativas = arrayOf("empresaNombre")
            ),
            instalacionNombre = valorTextoAnidado(registro, "instalacionNombre", instalacion, "nombre"),
            tecnicoNombre = valorTextoAnidado(registro, "tecnicoNombre", tecnico, "nombre", "nombreCompleto"),
            ingenieroNombre = valorTexto(registro, "ingenieroNombre"),
            fechaHora = fecha,
            fechaRegistro = valorFecha(registro, "fechaRegistro", "createdAt").ifBlank { fecha },
            temperaturaC = valorDouble(registro, "temperaturaC") ?: 0.0,
            humedadPct = valorDouble(registro, "humedadPct") ?: 0.0,
            estado = valorTexto(registro, "estado"),
            resultadoGlobal = valorTexto(registro, "resultadoGlobal", "resultado"),
            resultadoEvaluado = valorTexto(registro, "resultadoEvaluado", "resultadoGlobal", "resultado"),
            observaciones = valorTexto(registro, "observaciones").ifBlank { null },
            tipoVisita = valorTexto(registro, "tipoVisita").ifBlank { "Solo pruebas" },
            descripcionMantenimiento = valorTexto(registro, "descripcionMantenimiento").ifBlank { null },
            receptorNombre = valorTexto(registro, "receptorNombre"),
            receptorRut = valorTexto(registro, "receptorRut"),
            receptorCargo = valorTexto(registro, "receptorCargo"),
            numeroOt = valorTexto(registro, "numeroOt"),
            fechaRecepcion = valorTexto(registro, "fechaRecepcion"),
            idVisita = valorLong(registro, "idVisita") ?: 0L
        )
    }

    private fun idEquipoDeProtocolo(registro: JsonObject): Long? {
        val directo = valorLong(registro, "idEquipo")
        if (directo != null) return directo
        val equipoJson = registro.get("equipo")
        if (equipoJson == null || !equipoJson.isJsonObject) return null
        return valorLong(equipoJson.asJsonObject, "idEquipo", "id")
    }

    private fun valorTextoAnidado(
        registro: JsonObject,
        claveDirecta: String,
        anidado: JsonObject?,
        vararg clavesAnidadas: String,
        clavesDirectasAlternativas: Array<String> = emptyArray()
    ): String {
        val directo = valorTexto(registro, claveDirecta)
        if (directo.isNotBlank()) return directo

        for (clave in clavesDirectasAlternativas) {
            val alternativa = valorTexto(registro, clave)
            if (alternativa.isNotBlank()) return alternativa
        }

        if (anidado == null) return ""
        return valorTexto(anidado, *clavesAnidadas)
    }

    private fun valorFecha(registro: JsonObject, vararg claves: String): String {
        for (clave in claves) {
            val valor = registro.get(clave)
            if (valor == null || valor.isJsonNull) continue
            val texto = valor.asString
            val epoch = texto.toLongOrNull()
            if (epoch != null) {
                return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(epoch))
            }
            return texto.replace('T', ' ').take(16)
        }
        return ""
    }

    fun obtenerProtocoloVisible(idProtocolo: Long, usuario: Usuario?): ProtocoloTerreno? {
        usuario ?: return null
        val remoto = protocolosRemotos.firstOrNull { it.idProtocolo == idProtocolo }
        val local = db.obtenerProtocoloPorId(idProtocolo)
        if (usuario.rolCodigo.equals(Rol.CODIGO_TECNICO, ignoreCase = true) &&
            !db.tecnicoPuedeVerProtocolo(usuario.idUsuario, idProtocolo) && remoto == null
        ) return null
        if (usuario.rolCodigo.equals(Rol.CODIGO_CLIENTE, ignoreCase = true)) {
            val idEmpresa = usuario.idEmpresaCliente ?: return null
            if (!db.clientePuedeVerProtocolo(idProtocolo, idEmpresa) && remoto == null) return null
        }
        return local ?: remoto
    }

    suspend fun cargarDetalleProtocoloVisible(
        idProtocolo: Long,
        usuario: Usuario?
    ): ProtocoloTerreno? {
        val protocoloBase = obtenerProtocoloVisible(idProtocolo, usuario) ?: return null
        try {
            val detalleApi = repositorioRemoto.protocolo(idProtocolo)
            val detalle = mapearProtocoloRemoto(detalleApi) ?: protocoloBase
            val medicionesApi = repositorioRemoto.medicionesProtocolo(idProtocolo)
            val mediciones = medicionesApi.mapNotNull { mapearMedicionRemota(it) }
            _errorLecturaRemota.value = null
            return detalle.copy(
                numeroProtocolo = elegirTexto(detalle.numeroProtocolo, protocoloBase.numeroProtocolo),
                tagEquipo = elegirTexto(detalle.tagEquipo, protocoloBase.tagEquipo),
                clienteNombre = elegirTexto(detalle.clienteNombre, protocoloBase.clienteNombre),
                instalacionNombre = elegirTexto(detalle.instalacionNombre, protocoloBase.instalacionNombre),
                tecnicoNombre = elegirTexto(detalle.tecnicoNombre, protocoloBase.tecnicoNombre),
                fechaHora = elegirTexto(detalle.fechaHora, protocoloBase.fechaHora),
                estado = elegirTexto(detalle.estado, protocoloBase.estado),
                resultadoGlobal = elegirTexto(detalle.resultadoGlobal, protocoloBase.resultadoGlobal),
                resultadoEvaluado = elegirTexto(detalle.resultadoEvaluado, protocoloBase.resultadoEvaluado),
                observaciones = elegirTextoNullable(detalle.observaciones, protocoloBase.observaciones),
                mediciones = mediciones
            )
        } catch (error: ApiRequestException) {
            mostrarErrorLectura("No se pudo actualizar el detalle del protocolo: ${error.message}")
            return protocoloBase
        } catch (error: java.io.IOException) {
            mostrarErrorLectura("No se pudo actualizar el detalle del protocolo: ${error.message}")
            return protocoloBase
        } catch (error: RuntimeException) {
            mostrarErrorLectura("No se pudo interpretar el detalle del protocolo.")
            return protocoloBase
        }
    }

    private fun mapearMedicionRemota(registro: JsonObject): MedicionProtocolo? {
        val tipo = valorTexto(registro, "tipoPrueba")
        val nombre = valorTexto(registro, "nombre", "devanado", "fase")
        if (tipo.isBlank() && nombre.isBlank()) return null
        return MedicionProtocolo(
            tipoPrueba = tipo,
            nombre = nombre,
            valor1 = valorTexto(registro, "valor1"),
            valor2 = valorTexto(registro, "valor2"),
            valor3 = valorTexto(registro, "valor3"),
            valor4 = valorTexto(registro, "valor4"),
            referencia = valorTexto(registro, "referencia"),
            unidad = valorTexto(registro, "unidad"),
            resultado = valorTexto(registro, "resultado"),
            idInstrumento = valorLong(registro, "idInstrumento"),
            instrumento = valorTexto(registro, "instrumentoNombre", "instrumento")
        )
    }

    private fun elegirTexto(principal: String, respaldo: String): String {
        if (principal.isNotBlank()) return principal
        return respaldo
    }

    private fun elegirTextoNullable(principal: String?, respaldo: String?): String? {
        if (!principal.isNullOrBlank()) return principal
        return respaldo
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

    fun onTempAmbienteCambia(valor: String) =
        _uiState.update { it.copy(tempAmbiente = valor, mensajeErrorTemperatura = null) }
    fun onHumedadPctCambia(valor: String) =
        _uiState.update { it.copy(humedadPct = valor, mensajeErrorHumedad = null) }
    fun onHoraInicioCambia(valor: String) = _uiState.update { it.copy(horaInicio = valor) }
    fun onTipoVisitaCambia(tipo: String) = _uiState.update {
        it.copy(tipoVisita = tipo, mensajeErrorDescripcionMantenimiento = null)
    }
    fun onDescripcionMantenimientoCambia(valTexto: String) =
        _uiState.update { it.copy(descripcionMantenimiento = valTexto, mensajeErrorDescripcionMantenimiento = null) }
    fun onEsMedicionPosteriorCambia(esPosterior: Boolean) = _uiState.update { it.copy(esMedicionPosterior = esPosterior) }

    fun onPruebaRealizadaCambia(realizada: Boolean) {
        _uiState.update { it.copy(pruebaRealizada = realizada, errorMotivoNoRealizada = null, mensajeErrorFormulario = null) }
        recalcularGlobal()
    }

    fun onMotivoNoRealizadaCambia(motivo: String) {
        _uiState.update {
            it.copy(
                motivoNoRealizada = motivo,
                errorMotivoNoRealizada = null,
                mensajeErrorFormulario = null
            )
        }
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

        _uiState.update {
            it.copy(aislacionFilas = lista, mensajeErrorFormulario = null, mensajeErrorMediciones = null)
        }
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

        _uiState.update { it.copy(ttrFilas = lista, mensajeErrorFormulario = null, mensajeErrorMediciones = null) }
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

        _uiState.update { it.copy(contactosFilas = lista, mensajeErrorFormulario = null, mensajeErrorMediciones = null) }
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

        _uiState.update { it.copy(releFilas = lista, mensajeErrorFormulario = null, mensajeErrorMediciones = null) }
        recalcularGlobal()
    }

    fun onReleCorrienteMedidaCambia(index: Int, corriente: String) {
        val lista = _uiState.value.releFilas.toMutableList()
        if (index !in lista.indices) return
        lista[index] = lista[index].copy(corrienteMedidaA = corriente)
        _uiState.update { it.copy(releFilas = lista, mensajeErrorFormulario = null, mensajeErrorMediciones = null) }
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
        _uiState.update { it.copy(releFilas = lista, mensajeErrorFormulario = null, mensajeErrorMediciones = null) }
        onReleMedicionCambia(index, fila.tiempoMedidoMs, opera)
    }

    fun onClienteRecibeNombreCambia(valor: String) =
        _uiState.update { it.copy(clienteRecibeNombre = valor, mensajeErrorNombreClienteRecibe = null) }

    fun onClienteRecibeRutCambia(valor: String) {
        val esValido = ValidarRut.validarRut(valor)
        val errorRut = when {
            valor.isBlank() || esValido -> null
            else -> "El RUT debe incluir un dígito verificador válido, por ejemplo 12.345.678-5."
        }
        _uiState.update {
            it.copy(
                clienteRecibeRut = valor,
                errorRutClienteRecibe = errorRut != null,
                mensajeErrorRutClienteRecibe = errorRut
            )
        }
    }

    fun onClienteRecibeCargoCambia(valor: String) =
        _uiState.update { it.copy(clienteRecibeCargo = valor, mensajeErrorCargoClienteRecibe = null) }
    fun onIngenieroNombreCambia(valor: String) =
        _uiState.update { it.copy(ingenieroNombre = valor, mensajeErrorFormulario = null) }
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

    fun validarDatosVisita(): Boolean {
        val estado = _uiState.value
        val temperatura = estado.tempAmbiente.replace(',', '.').toDoubleOrNull()
        val humedad = estado.humedadPct.replace(',', '.').toDoubleOrNull()
        val errorTemperatura = if (temperatura == null || !temperatura.isFinite()) {
            "Ingresa una temperatura numérica, por ejemplo 20 o 20,5."
        } else null
        val errorHumedad = if (humedad == null || !humedad.isFinite() || humedad !in 0.0..100.0) {
            "Ingresa un valor de humedad entre 0 y 100 %."
        } else null
        val errorMantenimiento =
            if (
                estado.tipoVisita.replace('_', ' ') == "PRUEBAS Y MANTENIMIENTO" &&
                estado.descripcionMantenimiento.isBlank()
            ) {
                "Describe el mantenimiento realizado para cerrar esta visita."
            } else null

        _uiState.update {
            it.copy(
                mensajeErrorTemperatura = errorTemperatura,
                mensajeErrorHumedad = errorHumedad,
                mensajeErrorDescripcionMantenimiento = errorMantenimiento
            )
        }
        return errorTemperatura == null && errorHumedad == null && errorMantenimiento == null
    }

    fun validarMediciones(): Boolean {
        val estado = _uiState.value
        val errorMotivo = if (!estado.pruebaRealizada && estado.motivoNoRealizada.isBlank()) {
            "Explica por qué no se realizó la prueba."
        } else null
        val errorMediciones =
            if (estado.pruebaRealizada) obtenerErrorMediciones(estado) else null

        _uiState.update {
            it.copy(
                errorMotivoNoRealizada = errorMotivo,
                mensajeErrorMediciones = errorMediciones
            )
        }
        return errorMotivo == null && errorMediciones == null
    }

    fun validarRecepcion(): Boolean {
        val estado = _uiState.value
        val errorRut = if (!ValidarRut.validarRut(estado.clienteRecibeRut)) {
            if (estado.clienteRecibeRut.isBlank()) {
                "Ingresa el RUT de quien recibe."
            } else {
                "El RUT debe incluir un dígito verificador válido, por ejemplo 12.345.678-5."
            }
        } else null
        val errorNombre =
            if (estado.clienteRecibeNombre.isBlank()) "Ingresa el nombre de quien recibe." else null
        val errorCargo =
            if (estado.clienteRecibeCargo.isBlank()) "Ingresa el cargo de quien recibe." else null

        _uiState.update {
            it.copy(
                mensajeErrorRutClienteRecibe = errorRut,
                errorRutClienteRecibe = errorRut != null,
                mensajeErrorNombreClienteRecibe = errorNombre,
                mensajeErrorCargoClienteRecibe = errorCargo
            )
        }
        return errorRut == null && errorNombre == null && errorCargo == null
    }

    private fun obtenerErrorMediciones(estado: FormularioProtocoloUiState): String? {
        fun esPositivo(valor: String): Boolean =
            valor.replace(',', '.').toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true

        val aislamientoCompleto = estado.aislacionFilas.isNotEmpty() &&
            estado.aislacionFilas.all {
                esPositivo(it.lectura1Min) && esPositivo(it.lectura10Min)
            }
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
        if (medicionesCompletas) return null

        return when (estado.tipoEquipo) {
            "Transformador" -> {
                val aislacionFaltante = estado.aislacionFilas.firstOrNull {
                    !esPositivo(it.lectura1Min) || !esPositivo(it.lectura10Min)
                }?.let { "Aislación ${it.devanado}: completa las lecturas de 1 y 10 minutos." }
                val ttrFaltante = estado.ttrFilas.firstOrNull {
                    it.relacionTeorica <= 0 || !esPositivo(it.relacionMedida)
                }?.let { "TTR ${it.fase}: ingresa una relación medida válida." }
                val devanadoFaltante = estado.devanadoFilas.firstOrNull {
                    !esPositivo(it.faseU) || !esPositivo(it.faseV) || !esPositivo(it.faseW) ||
                        it.resultadoManual !in setOf("APROBADO", "OBSERVADO", "RECHAZADO")
                }?.let {
                    "Devanado ${it.devanado}: completa las fases U, V y W y selecciona su evaluación."
                }
                aislacionFaltante ?: ttrFaltante ?: devanadoFaltante
            }
            "Interruptor", "Celda MT" -> {
                val aislacionFaltante = estado.aislacionFilas.firstOrNull {
                    !esPositivo(it.lectura1Min) || !esPositivo(it.lectura10Min)
                }?.let { "Aislación ${it.devanado}: completa las lecturas de 1 y 10 minutos." }
                val contactoFaltante = estado.contactosFilas.firstOrNull {
                    it.valorMaximoPlacaUohm <= 0 || !esPositivo(it.valorMedidoUohm)
                }?.let { "${it.polo}: ingresa una resistencia de contacto mayor que cero." }
                aislacionFaltante ?: contactoFaltante
            }
            "Cable de poder" -> estado.aislacionFilas.firstOrNull {
                !esPositivo(it.lectura1Min) || !esPositivo(it.lectura10Min)
            }?.let { "Aislación ${it.devanado}: completa las lecturas de 1 y 10 minutos." }
            "Relé de protección" -> estado.releFilas.firstOrNull {
                !esPositivo(it.corrienteMedidaA) || it.operaCorrectamente == null ||
                    (it.operaCorrectamente && !esPositivo(it.tiempoMedidoMs))
            }?.let {
                "Relé ${it.funcion}: ingresa corriente y tiempo medidos, e indica si operó."
            }
            else -> null
        } ?: "Revisa las mediciones requeridas para este tipo de equipo."
    }

    fun onRegistrarProtocolo(
        idUsuarioActivo: Long,
        agregarAVisita: Boolean = false,
        evidenciaEquipo: ByteArray? = null
    ): Boolean {
        val estado = _uiState.value
        _uiState.update {
            it.copy(
                mensajeErrorFormulario = null,
                mensajeErrorTemperatura = null,
                mensajeErrorHumedad = null,
                mensajeErrorRutClienteRecibe = null,
                mensajeErrorNombreClienteRecibe = null,
                mensajeErrorCargoClienteRecibe = null,
                mensajeErrorDescripcionMantenimiento = null,
                mensajeErrorMediciones = null,
                errorRutClienteRecibe = false,
                errorMotivoNoRealizada = null
            )
        }

        val temperatura = estado.tempAmbiente.replace(',', '.').toDoubleOrNull()
        val humedad = estado.humedadPct.replace(',', '.').toDoubleOrNull()
        val errorTemperatura = if (temperatura == null || !temperatura.isFinite()) {
            "Ingresa una temperatura numérica, por ejemplo 20 o 20,5."
        } else null
        val errorHumedad = if (humedad == null || !humedad.isFinite() || humedad !in 0.0..100.0) {
            "Ingresa un valor de humedad entre 0 y 100 %."
        } else null
        val errorRutReceptor = if (!ValidarRut.validarRut(estado.clienteRecibeRut)) {
            if (estado.clienteRecibeRut.isBlank()) "Ingresa el RUT de quien recibe."
            else "El RUT debe incluir un dígito verificador válido, por ejemplo 12.345.678-5."
        } else null
        val errorNombreReceptor =
            if (estado.clienteRecibeNombre.isBlank()) "Ingresa el nombre de quien recibe." else null
        val errorCargoReceptor =
            if (estado.clienteRecibeCargo.isBlank()) "Ingresa el cargo de quien recibe." else null

        if (
            errorTemperatura != null || errorHumedad != null || errorRutReceptor != null ||
            errorNombreReceptor != null || errorCargoReceptor != null
        ) {
            _uiState.update {
                it.copy(
                    mensajeErrorTemperatura = errorTemperatura,
                    mensajeErrorHumedad = errorHumedad,
                    mensajeErrorRutClienteRecibe = errorRutReceptor,
                    errorRutClienteRecibe = errorRutReceptor != null,
                    mensajeErrorNombreClienteRecibe = errorNombreReceptor,
                    mensajeErrorCargoClienteRecibe = errorCargoReceptor
                )
            }
            return false
        }

        if (
            estado.tipoVisita.replace('_', ' ') == "PRUEBAS Y MANTENIMIENTO" &&
            estado.descripcionMantenimiento.isBlank()
        ) {
            _uiState.update {
                it.copy(mensajeErrorDescripcionMantenimiento = "Describe el mantenimiento realizado para cerrar esta visita.")
            }
            return false
        }

        val errorMotivo = if (!estado.pruebaRealizada && estado.motivoNoRealizada.isBlank()) {
            "Explica por qué no se realizó la prueba."
        } else null
        val errorMediciones =
            if (estado.pruebaRealizada) obtenerErrorMediciones(estado) else null
        if (errorMotivo != null || errorMediciones != null) {
            _uiState.update {
                it.copy(
                    errorMotivoNoRealizada = errorMotivo,
                    mensajeErrorMediciones = errorMediciones,
                    mensajeErrorFormulario = null
                )
            }
            return false
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
                temperaturaC = checkNotNull(temperatura),
                humedadPct = checkNotNull(humedad),
                observaciones = if (estado.pruebaRealizada) estado.observaciones else
                    "NO REALIZADA: ${estado.motivoNoRealizada}",
                tipoVisita = estado.tipoVisita.replace(' ', '_'),
                descripcionMantenimiento = estado.descripcionMantenimiento,
                receptorNombre = estado.clienteRecibeNombre.trim(),
                receptorRut = estado.clienteRecibeRut.trim(),
                receptorCargo = estado.clienteRecibeCargo.trim(),
                fechaRecepcion = fechaRecepcion,
                idUsuarioRegistra = idUsuarioActivo,
                resultadoGlobal = if (estado.pruebaRealizada) estado.resultadoGlobal else "NO REALIZADA",
                estado = "REGISTRADO_TERRENO",
                mediciones = mediciones,
                ingenieroNombre = estado.ingenieroNombre.trim(),
                evidenciaEquipo = evidenciaEquipo
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
            sincronizarVisitaYProtocolos(listOf(registro), estado)
            recargarHistorial(estado.idEquipo)
            val otNotif = estado.otNumero
            onLimpiar()
            _uiState.update {
                it.copy(
                    mensajeNotificacionExito = "¡Protocolo de la OT-$otNotif registrado exitosamente!",
                    idProtocoloRegistrado = idInsertado
                )
            }
            db.obtenerProtocoloPorId(idInsertado)?.let { protocolo ->
                NotificacionesLocales.notificarEstadoProtocolo(
                    getApplication(),
                    protocolo.numeroProtocolo,
                    protocolo.estado
                )
            }
        } else {
            _uiState.update {
                it.copy(mensajeErrorFormulario = "No se pudo guardar el protocolo. Comprueba que el equipo y los instrumentos seleccionados sigan disponibles e inténtalo otra vez.")
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
                it.copy(mensajeErrorFormulario = "No se guardaron todos los protocolos de la visita. Comprueba que las instalaciones e instrumentos sigan disponibles antes de volver a intentarlo.")
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
        val registrosVisita = protocolosPendientesVisita.toList()
        val estadoVisita = _uiState.value
        sincronizarVisitaYProtocolos(registrosVisita, estadoVisita)
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

    private fun sincronizarVisitaYProtocolos(
        registros: List<RegistroProtocolo>,
        estado: FormularioProtocoloUiState
    ) {
        if (registros.isEmpty()) {
            return
        }
        val idVisitaLocal = -System.currentTimeMillis()
        val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ROOT)
        val fechaServidor = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT)
        val fechaParseada = fecha.parse("${estado.fechaVisita} ${estado.horaInicio}")
        if (fechaParseada == null) {
            _uiState.update {
                it.copy(mensajeErrorFormulario = "No se pudo preparar la fecha de visita para sincronizar.")
            }
            return
        }
        val solicitudVisita = JsonObject()
        solicitudVisita.addProperty("uuidSync", UUID.randomUUID().toString())
        solicitudVisita.addProperty("numeroOt", registros.first().numeroOt)
        solicitudVisita.addProperty("fechaVisita", fechaServidor.format(fechaParseada))
        solicitudVisita.addProperty("idInstalacion", estado.idInstalacionSeleccionada)
        solicitudVisita.addProperty("idTecnico", registros.first().idUsuarioRegistra)
        solicitudVisita.addProperty("tipoVisita", registros.first().tipoVisita)
        solicitudVisita.addProperty("descripcionMantenimiento", registros.first().descripcionMantenimiento)
        solicitudVisita.addProperty("temperaturaC", registros.first().temperaturaC)
        solicitudVisita.addProperty("humedadPct", registros.first().humedadPct)
        solicitudVisita.addProperty("receptorNombre", registros.first().receptorNombre)
        solicitudVisita.addProperty("receptorRut", registros.first().receptorRut)
        solicitudVisita.addProperty("receptorCargo", registros.first().receptorCargo)
        solicitudVisita.addProperty("estado", "EN_PREPARACION")
        val instrumentos = estado.instrumentosDisponiblesAislacion +
            estado.instrumentosDisponiblesTtr +
            estado.instrumentosDisponiblesDevanados +
            estado.instrumentosDisponiblesRele

        viewModelScope.launch {
            try {
                repositorioRemoto.crearVisita(solicitudVisita, idVisitaLocal)
                for ((indice, registro) in registros.withIndex()) {
                    repositorioRemoto.agregarEquipoVisita(
                        idVisitaLocal,
                        registro.idEquipo,
                        indice + 1
                    )
                    val fechaProtocolo = SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.ROOT
                    ).parse(registro.fechaHora)
                    if (fechaProtocolo == null) {
                        throw IllegalArgumentException("La fecha del protocolo no es válida.")
                    }
                    val mediciones = mutableListOf<MedicionRequest>()
                    for (medicion in registro.mediciones) {
                        val instrumento = instrumentos.firstOrNull {
                            it.idInstrumento == medicion.idInstrumento
                        }
                        mediciones.add(
                            MedicionRequest(
                                idInstrumento = medicion.idInstrumento,
                                tipoPrueba = medicion.tipoPrueba,
                                nombre = medicion.nombre,
                                valor1 = medicion.valor1,
                                valor2 = medicion.valor2,
                                valor3 = medicion.valor3,
                                valor4 = medicion.valor4,
                                referencia = medicion.referencia,
                                unidad = medicion.unidad,
                                resultado = medicion.resultado,
                                instrumentoNombre = instrumento?.nombre,
                                instrumentoModelo = instrumento?.modelo,
                                instrumentoSerie = instrumento?.numeroSerie,
                                calibracionVencimiento = instrumento?.fechaVencimiento
                            )
                        )
                    }
                    val evidenciaBase64 = if (registro.evidenciaEquipo == null) {
                        null
                    } else {
                        Base64.encodeToString(registro.evidenciaEquipo, Base64.NO_WRAP)
                    }
                    val solicitudProtocolo = ProtocoloPendienteRequest(
                        protocolo = ProtocoloRequest(
                            idVisita = idVisitaLocal,
                            uuidSync = UUID.randomUUID().toString(),
                            idEquipo = registro.idEquipo,
                            idTecnico = registro.idUsuarioRegistra,
                            fechaHora = fechaProtocolo.time,
                            tipoPrueba = "PROTOCOLO_TERRENO",
                            atBt = null,
                            atTierra = null,
                            btTierra = null,
                            indicePolarizacion = null,
                            resultado = registro.resultadoGlobal,
                            observaciones = registro.observaciones,
                            estado = "REGISTRADO_EN_TERRENO",
                            evidenciaEquipo = evidenciaBase64,
                            resultadoGlobal = registro.resultadoGlobal
                        ),
                        mediciones = mediciones
                    )
                    repositorioRemoto.crearProtocolo(solicitudProtocolo)
                }
                repositorioRemoto.cerrarVisita(idVisitaLocal)
            } catch (error: ApiRequestException) {
                _uiState.update {
                    it.copy(mensajeErrorFormulario = "Sincronización: ${error.message}")
                }
            } catch (error: java.io.IOException) {
                _uiState.update {
                    it.copy(
                        mensajeErrorFormulario = "Sincronización: el envío quedó pendiente. ${error.message}"
                    )
                }
            } catch (error: IllegalArgumentException) {
                _uiState.update {
                    it.copy(mensajeErrorFormulario = "Sincronización: ${error.message}")
                }
            }
        }
    }

    fun onDevanadoCambia(index: Int, uStr: String, vStr: String, wStr: String) {
        val lista = _uiState.value.devanadoFilas.toMutableList()
        if (index in lista.indices) {
            lista[index] = lista[index].copy(
                faseU = uStr,
                faseV = vStr,
                faseW = wStr
            )
            _uiState.update {
                it.copy(devanadoFilas = lista, mensajeErrorFormulario = null, mensajeErrorMediciones = null)
            }
            recalcularGlobal()
        }
    }

    fun onDevanadoResultadoCambia(index: Int, resultado: String) {
        val lista = _uiState.value.devanadoFilas.toMutableList()
        if (index !in lista.indices || resultado !in setOf("APROBADO", "OBSERVADO", "RECHAZADO")) return
        lista[index] = lista[index].copy(resultadoManual = resultado)
        _uiState.update {
            it.copy(devanadoFilas = lista, mensajeErrorFormulario = null, mensajeErrorMediciones = null)
        }
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

    fun descartarErrorFormulario() {
        _uiState.update { it.copy(mensajeErrorFormulario = null) }
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
        repositorioRemoto.cerrar()
        db.close()
        super.onCleared()
    }
}