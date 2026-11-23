package cl.duocuc.Induztek.viewmodel

import android.app.Application
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duocuc.Induztek.data.remote.ApiRequestException
import cl.duocuc.Induztek.data.remote.RetrofitClient
import cl.duocuc.Induztek.data.remote.SolicitudAsignacion
import cl.duocuc.Induztek.data.remote.SolicitudEstadoProtocolo
import cl.duocuc.Induztek.data.remote.exigirCuerpo
import cl.duocuc.Induztek.data.remote.exigirExito
import cl.duocuc.Induztek.data.remote.MedicionRequest
import cl.duocuc.Induztek.data.remote.ProtocoloPendienteRequest
import cl.duocuc.Induztek.data.remote.ProtocoloRequest
import cl.duocuc.Induztek.model.FormularioProtocoloUiState
import cl.duocuc.Induztek.model.InstrumentoItem
import cl.duocuc.Induztek.model.ClienteCatalogoLocal
import cl.duocuc.Induztek.model.CuentaEmpresaLocal
import cl.duocuc.Induztek.model.InstalacionCatalogoLocal
import cl.duocuc.Induztek.model.InstalacionAsignacionLocal
import cl.duocuc.Induztek.model.EquipoCatalogoLocal
import cl.duocuc.Induztek.model.TipoEquipoCatalogoLocal
import cl.duocuc.Induztek.model.TiposEquipoCatalogo
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
import cl.duocuc.Induztek.model.HistorialEstadoProtocolo
import cl.duocuc.Induztek.notificaciones.NotificacionesLocales
import cl.duocuc.Induztek.model.ValidarRut
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.google.gson.JsonObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

class ProtocoloViewModel(application: Application) : AndroidViewModel(application) {

    private val apis = RetrofitClient.crear(application)

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
    private var tecnicosRemotos = emptyList<Usuario>()
    private var cuentasClienteRemotas = emptyList<CuentaEmpresaLocal>()

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
        _uiState.update {
            it.copy(
                clientesDisponibles = emptyList(),
                instalacionesDisponibles = emptyList(),
                tagsDisponibles = emptyList(),
                instrumentosDisponiblesAislacion = emptyList(),
                instrumentosDisponiblesTtr = emptyList(),
                instrumentosDisponiblesDevanados = emptyList(),
                instrumentosDisponiblesRele = emptyList(),
                instrumentoAislacionSeleccionado = InstrumentoItem(0L, "", "", ""),
                instrumentoTtrSeleccionado = InstrumentoItem(0L, "", "", ""),
                instrumentoDevanadosSeleccionado = InstrumentoItem(0L, "", "", ""),
                instrumentoReleSeleccionado = InstrumentoItem(0L, "", "", ""),
                tecnicoNombre = if (usuario.rolCodigo.equals(Rol.CODIGO_TECNICO, ignoreCase = true)) {
                    usuario.nombre
                } else {
                    ""
                },
                ingenieroNombre = ""
            )
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
                val empresasApi = apis.empresas.empresas().exigirCuerpo()
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
                        apis.equipos.instalacionesCliente(usuario.idUsuario).exigirCuerpo()
                    usuario.rolCodigo.equals(Rol.CODIGO_TECNICO, ignoreCase = true) ->
                        apis.equipos.instalacionesTecnico(usuario.idUsuario).exigirCuerpo()
                    else -> apis.equipos.instalaciones().exigirCuerpo()
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
            val respuesta = apis.instrumentos.instrumentos(tipoPrueba = tipo, activo = true)
                .exigirCuerpo()
            val instrumentos = respuesta.mapNotNull { instrumento ->
                val id = valorLong(instrumento, "idInstrumento", "id") ?: return@mapNotNull null
                val modelo = valorTexto(instrumento, "modelo", "nombre")
                val serie = valorTexto(instrumento, "numeroSerie", "serie")
                if (modelo.isBlank() && serie.isBlank()) return@mapNotNull null
                val calibraciones = apis.instrumentos.calibraciones(id).exigirCuerpo()
                val fechaVencimiento = calibraciones
                    .mapNotNull { valorTexto(it, "fechaVencimiento").takeIf(String::isNotBlank) }
                    .maxOrNull()
                InstrumentoItem(
                    idInstrumento = id,
                    modelo = modelo.ifBlank { "Instrumento $id" },
                    numeroSerie = serie,
                    fechaVencimiento = normalizarFechaInstrumento(
                        fechaVencimiento ?: valorTexto(
                            instrumento,
                            "fechaVencimiento",
                            "calibracionVencimiento",
                            "fechaProximaCalibracion"
                        )
                    ),
                    nombre = valorTexto(instrumento, "nombre").ifBlank { modelo }
                )
            }
            _uiState.update { actual ->
                when (tipo) {
                    "AISLACION" -> actual.copy(
                        instrumentosDisponiblesAislacion = instrumentos,
                        instrumentoAislacionSeleccionado = instrumentos.firstOrNull() ?: actual.instrumentoAislacionSeleccionado
                    )
                    "TTR" -> actual.copy(
                        instrumentosDisponiblesTtr = instrumentos,
                        instrumentoTtrSeleccionado = instrumentos.firstOrNull() ?: actual.instrumentoTtrSeleccionado
                    )
                    "DEVANADOS" -> actual.copy(
                        instrumentosDisponiblesDevanados = instrumentos,
                        instrumentoDevanadosSeleccionado = instrumentos.firstOrNull() ?: actual.instrumentoDevanadosSeleccionado
                    )
                    else -> actual.copy(
                        instrumentosDisponiblesRele = instrumentos,
                        instrumentoReleSeleccionado = instrumentos.firstOrNull() ?: actual.instrumentoReleSeleccionado
                    )
                }
            }

            private fun normalizarFechaInstrumento(fecha: String): String {
                val fechaIso = fecha.take(10)
                return runCatching {
                    LocalDate.parse(fechaIso).format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT))
                }.getOrDefault(fecha)
            }
        }
    }

    private fun cargarEquiposRemotos(idInstalacion: Long, usuario: Usuario?) {
        viewModelScope.launch {
            try {
                val equiposApi = when {
                    usuario?.rolCodigo.equals(Rol.CODIGO_CLIENTE, ignoreCase = true) ->
                        apis.equipos.equiposCliente(usuario?.idUsuario ?: return@launch).exigirCuerpo()
                    usuario?.rolCodigo.equals(Rol.CODIGO_TECNICO, ignoreCase = true) ->
                        apis.equipos.equiposTecnico(usuario?.idUsuario ?: return@launch).exigirCuerpo()
                    else -> apis.equipos.equiposInstalacion(idInstalacion).exigirCuerpo()
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

    suspend fun leerTecnicosParaAsignacion(usuario: Usuario?): List<Usuario> {
        if (!puedeAdministrarAsignaciones(usuario)) return emptyList()
        return try {
            tecnicosRemotos = apis.usuarios.usuarios().exigirCuerpo()
                .map(::convertirUsuario)
                .filter { it.activo && it.rolCodigo == Rol.CODIGO_TECNICO }
            _errorLecturaRemota.value = null
            tecnicosRemotos
        } catch (error: ApiRequestException) {
            mostrarErrorLectura("No se pudieron cargar los técnicos: ${error.message}")
            emptyList()
        } catch (error: IOException) {
            mostrarErrorLectura("No se pudieron cargar los técnicos: ${error.message}")
            emptyList()
        } catch (error: RuntimeException) {
            mostrarErrorLectura("No se pudieron interpretar los técnicos recibidos.")
            emptyList()
        }
    }

    private fun convertirUsuario(respuesta: cl.duocuc.Induztek.data.remote.UsuarioResponse): Usuario {
        val rolTexto = respuesta.rolNombre.uppercase().replace(" ", "_").replace("-", "_")
        val rol = when {
            rolTexto.contains("JEFE") && rolTexto.contains("TECN") -> Rol.CODIGO_JEFE_TECNICO
            rolTexto.contains("INGEN") -> Rol.CODIGO_INGENIERO
            rolTexto.contains("TECN") -> Rol.CODIGO_TECNICO
            rolTexto.contains("CLIENT") -> Rol.CODIGO_CLIENTE
            else -> rolTexto
        }
        val nombre = listOf(respuesta.nombre, respuesta.apellido.orEmpty())
            .filter(String::isNotBlank).joinToString(" ")
        return Usuario(
            idUsuario = respuesta.id,
            rut = respuesta.rut,
            nombre = nombre,
            correo = respuesta.correo,
            telefono = respuesta.telefono.orEmpty(),
            rolCodigo = rol,
            idEmpresaCliente = respuesta.empresaId,
            activo = respuesta.activo,
            debeCambiarContrasena = respuesta.debeCambiarClave
        )
    }

    suspend fun leerClientesConEmpresaParaInstalaciones(usuario: Usuario?): List<CuentaEmpresaLocal> {
        if (!puedeAdministrarAsignaciones(usuario)) return emptyList()
        return try {
            cuentasClienteRemotas = apis.usuarios.usuarios().exigirCuerpo()
                .filter { it.activo && it.rolNombre.contains("CLIENT", ignoreCase = true) }
                .mapNotNull { respuesta ->
                    val idEmpresa = respuesta.empresaId ?: return@mapNotNull null
                    CuentaEmpresaLocal(
                        idUsuario = respuesta.id,
                        nombreUsuario = listOf(respuesta.nombre, respuesta.apellido.orEmpty())
                            .filter(String::isNotBlank).joinToString(" "),
                        idEmpresa = idEmpresa,
                        razonSocial = respuesta.empresaNombre.orEmpty()
                    )
                }
            _errorLecturaRemota.value = null
            cuentasClienteRemotas
        } catch (error: ApiRequestException) {
            mostrarErrorLectura("No se pudieron cargar las cuentas de clientes: ${error.message}")
            emptyList()
        } catch (error: IOException) {
            mostrarErrorLectura("No se pudieron cargar las cuentas de clientes: ${error.message}")
            emptyList()
        } catch (error: RuntimeException) {
            mostrarErrorLectura("No se pudieron interpretar las cuentas de clientes recibidas.")
            emptyList()
        }
    }

    suspend fun leerInstalacionesDeClienteParaGestion(
        idCliente: Long,
        usuario: Usuario?
    ): List<InstalacionCatalogoLocal> {
        if (!puedeAdministrarAsignaciones(usuario)) return emptyList()
        try {
            val remoto = apis.equipos.instalacionesCliente(idCliente).exigirCuerpo()
            val instalaciones = remoto.mapNotNull { registro ->
                val id = valorLong(registro, "idInstalacion", "id") ?: return@mapNotNull null
                val nombre = valorTexto(registro, "nombre", "nombreInstalacion")
                if (nombre.isBlank()) return@mapNotNull null
                InstalacionCatalogoLocal(id, nombre)
            }
            val instalacionesAsignacion = remoto.mapNotNull(::mapearInstalacionAsignacion)
            instalacionesRemotas = (instalacionesRemotas.filterNot { existente ->
                instalacionesAsignacion.any { it.idInstalacion == existente.idInstalacion }
            } + instalacionesAsignacion)
            _errorLecturaRemota.value = null
            return instalaciones
        } catch (error: ApiRequestException) {
            mostrarErrorLectura("No se pudieron cargar las instalaciones: ${error.message}")
            return emptyList()
        } catch (error: java.io.IOException) {
            mostrarErrorLectura("No se pudieron cargar las instalaciones: ${error.message}")
            return emptyList()
        } catch (error: RuntimeException) {
            mostrarErrorLectura("No se pudieron interpretar las instalaciones recibidas.")
            return emptyList()
        }
    }

    suspend fun leerEquiposDeInstalacionParaGestion(
        idInstalacion: Long,
        usuario: Usuario?
    ): List<EquipoCatalogoLocal> {
        if (!puedeAdministrarAsignaciones(usuario)) return emptyList()
        try {
            val remoto = apis.equipos.equiposInstalacion(idInstalacion).exigirCuerpo()
            val equipos = remoto.mapNotNull { mapearEquipo(it, idInstalacion) }
            equiposRemotos = (equiposRemotos.filterNot { it.idInstalacion == idInstalacion } + equipos)
            _errorLecturaRemota.value = null
            return equipos
        } catch (error: ApiRequestException) {
            mostrarErrorLectura("No se pudieron cargar los equipos: ${error.message}")
            return emptyList()
        } catch (error: java.io.IOException) {
            mostrarErrorLectura("No se pudieron cargar los equipos: ${error.message}")
            return emptyList()
        } catch (error: RuntimeException) {
            mostrarErrorLectura("No se pudieron interpretar los equipos recibidos.")
            return emptyList()
        }
    }

    suspend fun crearInstalacionParaCliente(
        idCliente: Long,
        nombre: String,
        usuario: Usuario?
    ): Boolean {
        val responsable = usuario ?: return false
        if (!puedeAdministrarAsignaciones(responsable)) return false
        val cuenta = cuentasClienteRemotas.firstOrNull { it.idUsuario == idCliente } ?: return false
        val solicitud = JsonObject().apply {
            addProperty("idEmpresa", cuenta.idEmpresa)
            addProperty("idUsuarioCliente", idCliente)
            addProperty("nombre", nombre)
            addProperty("activa", true)
        }
        return try {
            apis.equipos.crearInstalacion(solicitud).exigirExito()
            true
        } catch (error: ApiRequestException) {
            mostrarErrorLectura("El servidor rechazó la instalación: ${error.message}")
            false
        } catch (error: IOException) {
            mostrarErrorLectura("No se pudo crear la instalación en el servidor: ${error.message}")
            false
        } catch (error: RuntimeException) {
            mostrarErrorLectura("No se pudo interpretar la respuesta al crear la instalación.")
            false
        }
    }

    fun tiposEquipoParaGestion(usuario: Usuario?): List<TipoEquipoCatalogoLocal> =
        if (puedeAdministrarAsignaciones(usuario)) TiposEquipoCatalogo.todos else emptyList()

    suspend fun crearEquipoParaInstalacion(
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
        val instalacion = instalacionesRemotas.firstOrNull { it.idInstalacion == idInstalacion }
            ?: return CrearEquipoResultado(campoError = "general", mensajeError = "La instalación ya no está disponible.")
        val tipoEquipo = TiposEquipoCatalogo.todos.firstOrNull { it.idTipoEquipo == idTipoEquipo }
            ?: return CrearEquipoResultado(campoError = "general", mensajeError = "Selecciona un tipo de equipo válido.")
        val solicitud = JsonObject()
        solicitud.addProperty("codigo", tag)
        solicitud.addProperty("tag", tag)
        solicitud.addProperty("tipo", tipoEquipo.nombre)
        solicitud.addProperty("clasificacion", tipoEquipo.clasificacion)
        solicitud.addProperty("marca", marca)
        solicitud.addProperty("modelo", modelo)
        solicitud.addProperty("numeroSerie", numeroSerie)
        solicitud.addProperty("potenciaKva", potenciaKva)
        solicitud.addProperty("tensiones", tensiones)
        solicitud.addProperty("anoFabricacion", anoFabricacion)
        solicitud.addProperty("posicionTap", posicionTap)
        solicitud.addProperty("grupoConexion", grupoConexion)
        solicitud.addProperty("resistenciaContactoMaxUohm", resistenciaContactoMaxUohm)
        solicitud.addProperty("instalacion", instalacion.nombre)
        solicitud.addProperty("idInstalacion", idInstalacion)
        solicitud.addProperty("idCliente", idCliente)
        return try {
            val creado = apis.equipos.crearEquipo(solicitud).exigirCuerpo()
            val id = if (creado.isJsonObject) {
                valorLong(creado.asJsonObject, "idEquipo", "id")
            } else null
            if (id == null) {
                CrearEquipoResultado(campoError = "general", mensajeError = "El servidor no devolvió el identificador del equipo.")
            } else {
                CrearEquipoResultado(idEquipo = id)
            }
        } catch (error: ApiRequestException) {
            CrearEquipoResultado(campoError = "general", mensajeError = "El servidor rechazó el equipo: ${error.message}")
        } catch (error: IOException) {
            CrearEquipoResultado(campoError = "general", mensajeError = "No se pudo crear el equipo en el servidor: ${error.message}")
        } catch (error: RuntimeException) {
            CrearEquipoResultado(campoError = "general", mensajeError = "No se pudo interpretar la respuesta al crear el equipo.")
        }
    }

    suspend fun leerInstalacionesParaAsignacion(usuario: Usuario?): List<InstalacionAsignacionLocal> {
        if (!puedeAdministrarAsignaciones(usuario)) return emptyList()
        try {
            if (empresasRemotas.isEmpty()) {
                empresasRemotas = apis.empresas.empresas().exigirCuerpo().mapNotNull { empresa ->
                    val id = valorLong(empresa, "idEmpresa", "id") ?: return@mapNotNull null
                    val nombre = valorTexto(empresa, "razonSocial", "nombre", "empresa")
                    if (nombre.isBlank()) return@mapNotNull null
                    ClienteCatalogoLocal(id, nombre)
                }
            }
            val respuesta = apis.equipos.instalaciones().exigirCuerpo()
            val instalaciones = respuesta.mapNotNull { mapearInstalacionAsignacion(it) }
            instalacionesRemotas = instalaciones
            _errorLecturaRemota.value = null
            return instalaciones
        } catch (error: ApiRequestException) {
            mostrarErrorLectura("No se pudieron cargar las instalaciones para asignar: ${error.message}")
            return emptyList()
        } catch (error: java.io.IOException) {
            mostrarErrorLectura("No se pudieron cargar las instalaciones para asignar: ${error.message}")
            return emptyList()
        } catch (error: RuntimeException) {
            mostrarErrorLectura("No se pudieron interpretar las instalaciones recibidas.")
            return emptyList()
        }
    }

    suspend fun leerIdsAsignadosATecnico(idTecnico: Long, usuario: Usuario?): Set<Long> {
        if (!puedeAdministrarAsignaciones(usuario)) return emptySet()
        try {
            val ids = apis.equipos.idsAsignacionesTecnico(idTecnico).exigirCuerpo().toSet()
            _errorLecturaRemota.value = null
            return ids
        } catch (error: ApiRequestException) {
            mostrarErrorLectura("No se pudieron cargar las asignaciones: ${error.message}")
            return emptySet()
        } catch (error: java.io.IOException) {
            mostrarErrorLectura("No se pudieron cargar las asignaciones: ${error.message}")
            return emptySet()
        } catch (error: RuntimeException) {
            mostrarErrorLectura("No se pudieron interpretar las asignaciones recibidas.")
            return emptySet()
        }
    }

    suspend fun leerInstalacionesAsignadasAOtrosTecnicos(
        idTecnico: Long,
        usuario: Usuario?
    ): Map<Long, String> {
        if (!puedeAdministrarAsignaciones(usuario)) return emptyMap()
        try {
            val asignaciones = mutableMapOf<Long, String>()
            val tecnicos = if (tecnicosRemotos.isEmpty()) {
                leerTecnicosParaAsignacion(usuario)
            } else {
                tecnicosRemotos
            }
            for (tecnico in tecnicos) {
                if (tecnico.idUsuario == idTecnico) continue
                val respuesta = apis.equipos.asignacionesTecnico(tecnico.idUsuario).exigirCuerpo()
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
            return emptyMap()
        } catch (error: java.io.IOException) {
            mostrarErrorLectura("No se pudieron cargar las asignaciones existentes: ${error.message}")
            return emptyMap()
        } catch (error: RuntimeException) {
            mostrarErrorLectura("No se pudieron interpretar las asignaciones recibidas.")
            return emptyMap()
        }
    }

    private fun puedeAdministrarAsignaciones(usuario: Usuario?): Boolean =
        usuario?.rolCodigo.equals(Rol.CODIGO_JEFE_TECNICO, ignoreCase = true) ||
                usuario?.rolCodigo.equals(Rol.CODIGO_INGENIERO, ignoreCase = true)

    suspend fun guardarAsignacionesTecnico(
        idTecnico: Long,
        idsInstalaciones: Set<Long>,
        usuario: Usuario?
    ): Boolean {
        val responsable = usuario ?: return false
        if (!puedeAdministrarAsignaciones(responsable)) return false
        return try {
            apis.equipos.asignarInstalaciones(
                idTecnico,
                SolicitudAsignacion(responsable.idUsuario, idsInstalaciones.toList())
            ).exigirExito()
            true
        } catch (error: ApiRequestException) {
            mostrarErrorLectura("El servidor rechazó las asignaciones: ${error.message}")
            false
        } catch (error: IOException) {
            mostrarErrorLectura("No se pudieron guardar las asignaciones: ${error.message}")
            false
        } catch (error: RuntimeException) {
            mostrarErrorLectura("No se pudo interpretar la respuesta al guardar las asignaciones.")
            false
        }
    }

    private fun obtenerHoraActual(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    fun onClienteSeleccionado(cliente: String) {
        val empresa = empresasRemotas.firstOrNull {
            it.razonSocial.equals(cliente, ignoreCase = true)
        } ?: return
        val usuario = usuarioCatalogoActual
        if (
            usuario?.rolCodigo.equals(Rol.CODIGO_CLIENTE, ignoreCase = true) &&
            empresa.idEmpresa != usuario?.idEmpresaCliente
        ) return
        val instalaciones = if (usuario?.rolCodigo.equals(Rol.CODIGO_CLIENTE, ignoreCase = true)) {
            instalacionesRemotas
        } else {
            instalacionesRemotas.filter { instalacion ->
                instalacion.empresa.equals(cliente, ignoreCase = true)
            }
        }

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
                clienteRecibeNombre = "",
                clienteRecibeRut = "",
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
        val instalacionRemota = instalacionesRemotas.firstOrNull { it.nombre == instalacion }
        val idInstalacion = instalacionRemota?.idInstalacion ?: return
        val equiposDisponibles = equiposRemotos.filter { it.idInstalacion == idInstalacion }
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
        val equipo = equiposRemotos.firstOrNull {
            it.idInstalacion == estado.idInstalacionSeleccionada && it.tag == tag
        }
        if (equipo == null) return

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
                lecturaAnterior1Min = null,
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
            _historial.value = emptyList()
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
        _historial.value = emptyList()
        _errorLecturaRemota.value = null

        viewModelScope.launch {
            try {
                var respuesta = emptyList<JsonObject>()
                val codigoRol = usuarioActual.rolCodigo
                if (codigoRol.equals(Rol.CODIGO_CLIENTE, ignoreCase = true)) {
                    respuesta = apis.protocolos.protocolosCliente(usuarioActual.idUsuario).exigirCuerpo()
                    if (idEquipo != 0L) {
                        respuesta = respuesta.filter { protocolo ->
                            idEquipoDeProtocolo(protocolo) == idEquipo
                        }
                    }
                } else if (codigoRol.equals(Rol.CODIGO_TECNICO, ignoreCase = true)) {
                    if (idEquipo != 0L) {
                        val equiposTecnico = apis.equipos.equiposTecnico(usuarioActual.idUsuario)
                            .exigirCuerpo()
                        val puedeVer = equiposTecnico.any {
                            valorLong(it, "idEquipo", "id") == idEquipo
                        }
                        if (!puedeVer) {
                            _historial.value = emptyList()
                            _errorLecturaRemota.value = null
                            return@launch
                        }
                        respuesta = apis.protocolos.protocolosEquipo(idEquipo).exigirCuerpo()
                    } else {
                        val visitasTecnico = apis.equipos.visitas(tecnico = usuarioActual.idUsuario)
                            .exigirCuerpo()
                        val protocolosVisita = mutableListOf<JsonObject>()
                        for (visita in visitasTecnico) {
                            val idVisita = valorLong(visita, "idVisita", "id")
                            if (idVisita != null) {
                                protocolosVisita.addAll(
                                    apis.protocolos.protocolosVisita(idVisita).exigirCuerpo()
                                )
                            }
                        }
                        respuesta = protocolosVisita
                    }
                } else if (
                    codigoRol.equals(Rol.CODIGO_INGENIERO, ignoreCase = true) ||
                    codigoRol.equals(Rol.CODIGO_JEFE_TECNICO, ignoreCase = true)
                ) {
                    respuesta = if (idEquipo == 0L) {
                        apis.protocolos.protocolos().exigirCuerpo()
                    } else {
                        apis.protocolos.protocolosEquipo(idEquipo).exigirCuerpo()
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
                _historial.value = emptyList()
                mostrarErrorLectura("No se pudo actualizar el historial: ${error.message}")
            } catch (error: java.io.IOException) {
                _historial.value = emptyList()
                mostrarErrorLectura("No se pudo actualizar el historial: ${error.message}")
            } catch (error: RuntimeException) {
                _historial.value = emptyList()
                mostrarErrorLectura("No se pudo interpretar el historial recibido.")
            }
        }
    }

    fun cargarProtocolosCliente(usuario: Usuario) {
        _historial.value = emptyList()
        _errorLecturaRemota.value = null
        viewModelScope.launch {
            try {
                val respuesta = apis.protocolos.protocolosCliente(usuario.idUsuario).exigirCuerpo()
                val protocolos = respuesta.mapNotNull { mapearProtocoloRemoto(it) }
                protocolosRemotos = protocolos
                _historial.value = protocolos
                _errorLecturaRemota.value = null
            } catch (error: ApiRequestException) {
                _historial.value = emptyList()
                mostrarErrorLectura("No se pudo actualizar el portal de protocolos: ${error.message}")
            } catch (error: java.io.IOException) {
                _historial.value = emptyList()
                mostrarErrorLectura("No se pudo actualizar el portal de protocolos: ${error.message}")
            } catch (error: RuntimeException) {
                _historial.value = emptyList()
                mostrarErrorLectura("No se pudo interpretar el historial recibido.")
            }
        }
    }

    private var protocolosRemotos = emptyList<ProtocoloTerreno>()

    private fun mapearProtocoloRemoto(registro: JsonObject): ProtocoloTerreno? {
        val protocoloJson = registro.get("protocolo")
        val datosProtocolo = if (protocoloJson != null && protocoloJson.isJsonObject) {
            protocoloJson.asJsonObject
        } else {
            registro
        }
        val id = valorLong(datosProtocolo, "idProtocolo", "id") ?: return null
        val equipoJson = registro.get("equipo") ?: datosProtocolo.get("equipo")
        val equipo = if (equipoJson != null && equipoJson.isJsonObject) equipoJson.asJsonObject else null
        val instalacionJson = registro.get("instalacion") ?: datosProtocolo.get("instalacion")
        val instalacion = if (instalacionJson != null && instalacionJson.isJsonObject) {
            instalacionJson.asJsonObject
        } else {
            null
        }
        val tecnicoJson = registro.get("tecnico") ?: datosProtocolo.get("tecnico")
        val tecnico = if (tecnicoJson != null && tecnicoJson.isJsonObject) tecnicoJson.asJsonObject else null
        val clienteJson = registro.get("cliente") ?: datosProtocolo.get("cliente")
        val cliente = if (clienteJson != null && clienteJson.isJsonObject) clienteJson.asJsonObject else null
        val fecha = valorFecha(datosProtocolo, "fechaHora", "fechaHoraPrueba", "fechaRegistro", "createdAt")
        var idEquipo = valorLong(datosProtocolo, "idEquipo")
        if (idEquipo == null && equipo != null) {
            idEquipo = valorLong(equipo, "idEquipo", "id")
        }
        if (idEquipo == null) {
            idEquipo = 0L
        }
        return ProtocoloTerreno(
            idProtocolo = id,
            numeroProtocolo = valorTexto(datosProtocolo, "numeroProtocolo", "numero").ifBlank { "PT-$id" },
            idEquipo = idEquipo,
            tagEquipo = valorTextoAnidado(
                registro,
                "tagEquipo",
                equipo,
                "tag",
                "codigo",
                clavesDirectasAlternativas = arrayOf("codigoEquipo")
            ),
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
            instalacionNombre = valorTextoAnidado(
                registro,
                "instalacionNombre",
                instalacion,
                "nombre",
                clavesDirectasAlternativas = arrayOf("instalacion")
            ),
            tecnicoNombre = valorTextoAnidado(
                registro,
                "tecnicoNombre",
                tecnico,
                "nombre",
                "nombreCompleto",
                clavesDirectasAlternativas = arrayOf("nombreTecnico")
            ),
            ingenieroNombre = valorTexto(registro, "ingenieroNombre"),
            fechaHora = fecha,
            fechaRegistro = valorFecha(registro, "fechaRegistro", "createdAt").ifBlank { fecha },
            temperaturaC = valorDouble(datosProtocolo, "temperaturaC") ?: 0.0,
            humedadPct = valorDouble(datosProtocolo, "humedadPct") ?: 0.0,
            estado = valorTexto(datosProtocolo, "estado"),
            resultadoGlobal = valorTexto(datosProtocolo, "resultadoGlobal", "resultado"),
            resultadoEvaluado = valorTexto(datosProtocolo, "resultadoEvaluado", "resultadoGlobal", "resultado"),
            observaciones = valorTexto(datosProtocolo, "observaciones").ifBlank { null },
            tipoVisita = valorTexto(datosProtocolo, "tipoVisita").ifBlank { "Solo pruebas" },
            descripcionMantenimiento = valorTexto(datosProtocolo, "descripcionMantenimiento").ifBlank { null },
            receptorNombre = valorTexto(datosProtocolo, "receptorNombre"),
            receptorRut = valorTexto(datosProtocolo, "receptorRut"),
            receptorCargo = valorTexto(datosProtocolo, "receptorCargo"),
            numeroOt = valorTexto(datosProtocolo, "numeroOt"),
            fechaRecepcion = valorTexto(datosProtocolo, "fechaRecepcion"),
            idVisita = valorLong(datosProtocolo, "idVisita") ?: 0L,
            fechaFormalizacion = valorFecha(datosProtocolo, "fechaFormalizacion").ifBlank { null },
            motivoObservacion = valorTexto(datosProtocolo, "motivoObservacion").ifBlank { null },
            historialEstados = registro.get("historialEstados")
                ?.takeIf { it.isJsonArray }
                ?.asJsonArray
                ?.mapNotNull { evento ->
                    if (evento.isJsonObject) mapearHistorialEstado(evento.asJsonObject) else null
                }
                .orEmpty()
        )
    }

    private fun mapearHistorialEstado(registro: JsonObject): HistorialEstadoProtocolo? {
        val estadoNuevo = valorTexto(registro, "estadoNuevo")
        if (estadoNuevo.isBlank()) return null
        val idUsuario = valorLong(registro, "idUsuario")
        return HistorialEstadoProtocolo(
            estadoAnterior = valorTexto(registro, "estadoAnterior").ifBlank { null },
            estadoNuevo = estadoNuevo,
            usuarioNombre = valorTexto(registro, "usuarioNombre")
                .ifBlank { idUsuario?.let { "Usuario $it" }.orEmpty() },
            fechaHora = valorFecha(registro, "fechaHora"),
            motivo = valorTexto(registro, "motivo").ifBlank { null }
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
        return protocolosRemotos.firstOrNull { it.idProtocolo == idProtocolo }
    }

    suspend fun cargarDetalleProtocoloVisible(
        idProtocolo: Long,
        usuario: Usuario?
    ): ProtocoloTerreno? {
        val protocoloBase = obtenerProtocoloVisible(idProtocolo, usuario)
        if (usuario == null) return null
        try {
            val detalleApi = apis.protocolos.protocolo(idProtocolo).exigirCuerpo()
            val detalle = mapearProtocoloRemoto(detalleApi) ?: protocoloBase ?: return null
            val respaldo = protocoloBase ?: detalle
            val medicionesApi = apis.protocolos.mediciones(idProtocolo).exigirCuerpo()
            val mediciones = medicionesApi.mapNotNull { mapearMedicionRemota(it) }
            _errorLecturaRemota.value = null
            return detalle.copy(
                numeroProtocolo = elegirTexto(detalle.numeroProtocolo, respaldo.numeroProtocolo),
                tagEquipo = elegirTexto(detalle.tagEquipo, respaldo.tagEquipo),
                clienteNombre = elegirTexto(detalle.clienteNombre, respaldo.clienteNombre),
                instalacionNombre = elegirTexto(detalle.instalacionNombre, respaldo.instalacionNombre),
                tecnicoNombre = elegirTexto(detalle.tecnicoNombre, respaldo.tecnicoNombre),
                fechaHora = elegirTexto(detalle.fechaHora, respaldo.fechaHora),
                estado = elegirTexto(detalle.estado, respaldo.estado),
                resultadoGlobal = elegirTexto(detalle.resultadoGlobal, respaldo.resultadoGlobal),
                resultadoEvaluado = elegirTexto(detalle.resultadoEvaluado, respaldo.resultadoEvaluado),
                observaciones = elegirTextoNullable(detalle.observaciones, respaldo.observaciones),
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

    suspend fun cambiarEstadoProtocolo(
        idProtocolo: Long,
        nuevoEstado: String,
        usuario: Usuario,
        motivo: String? = null
    ): ProtocoloTerreno? {
        if (!puedeAdministrarAsignaciones(usuario)) return null
        if (nuevoEstado in setOf("OBSERVADO", "ANULADO") && motivo.isNullOrBlank()) return null
        return try {
            apis.protocolos.cambiarEstado(
                idProtocolo,
                SolicitudEstadoProtocolo(nuevoEstado, motivo)
            ).exigirCuerpo()
            val base = protocolosRemotos.firstOrNull { it.idProtocolo == idProtocolo }
                ?: apis.protocolos.protocolo(idProtocolo).exigirCuerpo()
                    .let(::mapearProtocoloRemoto)
                ?: return null
            val actualizado = base.copy(
                estado = nuevoEstado,
                motivoObservacion = if (nuevoEstado == "OBSERVADO") motivo?.trim() else base.motivoObservacion
            )
            protocolosRemotos = protocolosRemotos.map {
                if (it.idProtocolo == idProtocolo) actualizado else it
            }
            _historial.update { actuales ->
                actuales.map { if (it.idProtocolo == idProtocolo) actualizado else it }
            }
            NotificacionesLocales.notificarEstadoProtocolo(
                getApplication(),
                actualizado.numeroProtocolo,
                actualizado.estado
            )
            actualizado
        } catch (error: ApiRequestException) {
            mostrarErrorLectura("No se pudo cambiar el estado del protocolo: ${error.message}")
            null
        } catch (error: IOException) {
            mostrarErrorLectura("No se pudo cambiar el estado del protocolo: ${error.message}")
            null
        } catch (error: RuntimeException) {
            mostrarErrorLectura("No se pudo interpretar la respuesta al cambiar el estado.")
            null
        }
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

        val instrumentosDisponibles = estado.instrumentosDisponiblesAislacion +
            estado.instrumentosDisponiblesTtr +
            estado.instrumentosDisponiblesDevanados +
            estado.instrumentosDisponiblesRele
        val instrumentoSinCatalogo = mediciones.firstOrNull { medicion ->
            val idInstrumento = medicion.idInstrumento
            idInstrumento == null || idInstrumento <= 0 ||
                instrumentosDisponibles.none { it.idInstrumento == idInstrumento }
        }
        if (instrumentoSinCatalogo != null) {
            _uiState.update {
                it.copy(mensajeErrorFormulario = "Selecciona un instrumento registrado en el servidor para cada medición.")
            }
            return false
        }
        val instrumentosUsados = mediciones.mapNotNull { medicion ->
            val id = medicion.idInstrumento ?: return@mapNotNull null
            instrumentosDisponibles.firstOrNull { it.idInstrumento == id }
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
            val equipos = equiposRemotos.filter {
                it.idInstalacion == estado.idInstalacionSeleccionada
            }
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

        enviarVisitaYProtocolos(listOf(registro), estado)
        return true
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
        val registrosVisita = protocolosPendientesVisita.toList()
        val estadoVisita = _uiState.value
        enviarVisitaYProtocolos(registrosVisita, estadoVisita)
        return true
    }

    private fun enviarVisitaYProtocolos(
        registros: List<RegistroProtocolo>,
        estado: FormularioProtocoloUiState
    ) {
        if (registros.isEmpty()) {
            return
        }
        val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ROOT)
        val fechaServidor = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT)
        val fechaParseada = fecha.parse("${estado.fechaVisita} ${estado.horaInicio}")
        if (fechaParseada == null) {
            _uiState.update {
                it.copy(mensajeErrorFormulario = "No se pudo preparar la fecha de la visita.")
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
                val visitaRemota = apis.equipos.crearVisita(solicitudVisita).exigirCuerpo()
                val idVisitaRemota = valorLong(visitaRemota, "idVisita", "id")
                    ?: throw IOException("El servidor no devolvió el identificador de la visita.")
                val idsProtocolos = mutableListOf<Long>()
                for ((indice, registro) in registros.withIndex()) {
                    val solicitudEquipoVisita = JsonObject().apply {
                        addProperty("idEquipo", registro.idEquipo)
                        addProperty("orden", indice + 1)
                    }
                    apis.equipos.agregarEquipoVisita(
                        idVisitaRemota,
                        solicitudEquipoVisita
                    ).exigirExito()
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
                            idVisita = idVisitaRemota,
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
                    val protocoloCreado = apis.protocolos.crearProtocolo(solicitudProtocolo).exigirCuerpo()
                    idsProtocolos.add(
                        valorLong(protocoloCreado, "idProtocolo", "id")
                            ?: throw IOException("El servidor no devolvió el identificador del protocolo.")
                    )
                }
                apis.equipos.cerrarVisitaRemota(idVisitaRemota).exigirExito()
                val numeroOt = registros.first().numeroOt
                val idEquipo = registros.first().idEquipo
                protocolosPendientesVisita.clear()
                usuarioVisitaPendiente = null
                onLimpiar()
                _uiState.update {
                    it.copy(
                        idProtocoloRegistrado = idsProtocolos.lastOrNull(),
                        protocolosPendientesVisita = 0,
                        tagsPendientesVisita = emptyList(),
                        mensajeNotificacionExito = if (registros.size == 1) {
                            "¡Protocolo de la OT-$numeroOt registrado exitosamente!"
                        } else {
                            "Visita OT-$numeroOt registrada con ${idsProtocolos.size} protocolo(s)."
                        }
                    )
                }
                recargarHistorial(idEquipo)
            } catch (error: ApiRequestException) {
                _uiState.update {
                    it.copy(mensajeErrorFormulario = "El servidor rechazó el protocolo: ${error.message}")
                }
            } catch (error: java.io.IOException) {
                _uiState.update {
                    it.copy(
                        mensajeErrorFormulario = "No se pudo enviar el protocolo al servidor: ${error.message}"
                    )
                }
            } catch (error: IllegalArgumentException) {
                _uiState.update {
                    it.copy(mensajeErrorFormulario = "No se pudo preparar el protocolo: ${error.message}")
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
            _uiState.update { it.copy(clientesDisponibles = emptyList()) }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}