package cl.duocuc.Induztek.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duocuc.Induztek.data.ConvertidorImagen
import cl.duocuc.Induztek.data.InduztekDbHelper
import cl.duocuc.Induztek.data.SesionPreferencias
import cl.duocuc.Induztek.data.remote.ApiRequestException
import cl.duocuc.Induztek.data.remote.InduztekRepository
import com.google.gson.JsonObject
import cl.duocuc.Induztek.model.CrearCuentaResultado
import cl.duocuc.Induztek.model.Rol
import cl.duocuc.Induztek.model.Usuario
import cl.duocuc.Induztek.model.ValidarContrasena
import cl.duocuc.Induztek.model.ValidarTelefonoChile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.SecureRandom

data class RecuperacionUiState(
    val codigoSolicitado: Boolean = false,
    val cargando: Boolean = false,
    val mensaje: String? = null,
    val error: String? = null,
    val claveActualizada: Boolean = false
)

data class AltaRemotaUiState(
    val cargando: Boolean = false,
    val pendiente: Boolean = false,
    val mensaje: String? = null,
    val error: String? = null
)

class UsuariosViewModel(application: Application) : AndroidViewModel(application) {

    private val db = InduztekDbHelper(application)
    private val sesionPreferencias = SesionPreferencias(application)
    private val repositorio = InduztekRepository(application)
    private val secureRandom = SecureRandom()

    private val _usuarioActual = MutableStateFlow<Usuario?>(null)
    val usuarioActual: StateFlow<Usuario?> = _usuarioActual.asStateFlow()

    private val _autenticando = MutableStateFlow(false)
    val autenticando = _autenticando.asStateFlow()

    private val _errorAuth = MutableStateFlow<String?>(null)
    val errorAuth: StateFlow<String?> = _errorAuth.asStateFlow()

    private val _recuperacion = MutableStateFlow(RecuperacionUiState())
    val recuperacion = _recuperacion.asStateFlow()

    private val _altaRemota = MutableStateFlow(AltaRemotaUiState())
    val altaRemota = _altaRemota.asStateFlow()
    private var accionReintentarAlta: (() -> Unit)? = null

    init {
        val idUsuario = sesionPreferencias.leer()
        val token = sesionPreferencias.leerToken()
        val usuarioGuardado = sesionPreferencias.leerUsuario()
        if (idUsuario > 0 && !token.isNullOrBlank()) {
            _usuarioActual.value = usuarioGuardado
            viewModelScope.launch {
                try {
                    val usuarioRemoto = repositorio.usuario(idUsuario)
                    val usuario = convertirUsuario(usuarioRemoto)
                    if (usuario.activo) {
                        _usuarioActual.value = usuario
                        sesionPreferencias.guardarUsuario(usuario)
                    } else {
                        cerrarSesion()
                    }
                } catch (error: ApiRequestException) {
                    if (error.statusCode == 401) {
                        cerrarSesion()
                    } else if (usuarioGuardado == null) {
                        _errorAuth.value = error.message
                    }
                } catch (error: java.io.IOException) {
                    if (usuarioGuardado == null) {
                        _errorAuth.value = "No se pudo validar la sesión sin conexión."
                    }
                }
            }
        } else {
            sesionPreferencias.borrar()
        }
    }

    fun iniciarSesion(correo: String, clave: String) {
        if (correo.isBlank()) {
            _errorAuth.value = "Ingresa el correo electrónico de tu cuenta."
            return
        }
        if (clave.isBlank()) {
            _errorAuth.value = "Ingresa la contraseña de tu cuenta."
            return
        }

        _autenticando.value = true
        _errorAuth.value = null
        viewModelScope.launch {
            try {
                val respuesta = repositorio.iniciarSesion(correo, clave)
                sesionPreferencias.guardar(respuesta.usuarioId)
                sesionPreferencias.guardarToken(respuesta.token)
                val perfil = convertirUsuario(repositorio.usuario(respuesta.usuarioId))
                if (!perfil.activo) {
                    sesionPreferencias.borrar()
                    _errorAuth.value = "La cuenta está inactiva. Contacta al administrador."
                    return@launch
                }
                val usuario = perfil.copy(
                    debeCambiarContrasena = respuesta.primerIngreso || perfil.debeCambiarContrasena
                )
                sesionPreferencias.guardarUsuario(usuario)
                _usuarioActual.value = usuario
            } catch (error: ApiRequestException) {
                sesionPreferencias.borrar()
                _errorAuth.value = error.message
            } catch (_: java.io.IOException) {
                sesionPreferencias.borrar()
                _errorAuth.value = "No se pudo conectar con el servicio. Verifica tu conexión e inténtalo nuevamente."
            } finally {
                _autenticando.value = false
            }
        }
    }

    fun cerrarSesion() {
        _usuarioActual.value = null
        sesionPreferencias.borrar()
        _errorAuth.value = null
    }

    fun limpiarErrorAuth() {
        _errorAuth.value = null
    }

    fun solicitarCodigoRecuperacion(correo: String) {
        if (correo.isBlank()) {
            _recuperacion.value = RecuperacionUiState(error = "Ingresa tu correo electrónico.")
            return
        }
        _recuperacion.value = RecuperacionUiState(cargando = true)
        viewModelScope.launch {
            try {
                repositorio.solicitarCodigo(correo)
                _recuperacion.value = RecuperacionUiState(
                    codigoSolicitado = true,
                    mensaje = "Si el correo está registrado, recibirás un código de recuperación válido por 15 minutos."
                )
            } catch (error: ApiRequestException) {
                _recuperacion.value = RecuperacionUiState(error = error.message)
            } catch (_: java.io.IOException) {
                _recuperacion.value = RecuperacionUiState(
                    error = "No se pudo conectar con el servicio. Revisa tu conexión e inténtalo nuevamente."
                )
            }
        }
    }

    fun restablecerClaveConCodigo(correo: String, codigo: String, claveNueva: String) {
        if (codigo.isBlank()) {
            _recuperacion.value = _recuperacion.value.copy(error = "Ingresa el código enviado a tu correo.")
            return
        }
        _recuperacion.value = _recuperacion.value.copy(cargando = true, error = null)
        viewModelScope.launch {
            try {
                repositorio.restablecerClave(correo, codigo, claveNueva)
                _recuperacion.value = RecuperacionUiState(
                    codigoSolicitado = true,
                    claveActualizada = true,
                    mensaje = "Contraseña actualizada. Ya puedes iniciar sesión."
                )
            } catch (error: ApiRequestException) {
                _recuperacion.value = _recuperacion.value.copy(
                    cargando = false,
                    error = error.message
                )
            } catch (_: java.io.IOException) {
                _recuperacion.value = _recuperacion.value.copy(
                    cargando = false,
                    error = "No se pudo conectar con el servicio. Revisa tu conexión e inténtalo nuevamente."
                )
            }
        }
    }

    fun limpiarRecuperacion() {
        _recuperacion.value = RecuperacionUiState()
    }

    private fun convertirUsuario(respuesta: cl.duocuc.Induztek.data.remote.UsuarioResponse): Usuario {
        val rolTexto = respuesta.rolNombre.uppercase()
            .replace(" ", "_")
            .replace("-", "_")
        val rol = when {
            rolTexto.contains("JEFE") && rolTexto.contains("TECN") -> Rol.CODIGO_JEFE_TECNICO
            rolTexto.contains("INGEN") -> Rol.CODIGO_INGENIERO
            rolTexto.contains("TECN") -> Rol.CODIGO_TECNICO
            rolTexto.contains("CLIENT") -> Rol.CODIGO_CLIENTE
            else -> rolTexto
        }
        val nombreCompleto = respuesta.nombre.trim()
        val apellido = respuesta.apellido.orEmpty().trim()
        val nombre = if (apellido.isBlank()) nombreCompleto else "$nombreCompleto $apellido"
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

    fun registrar(
        rut: String,
        nombre: String,
        correo: String,
        tel: String,
        clave: String,
        rol: String,
        preg: String,
        resp: String
    ): Boolean {
        val telefonoNormalizado = if (tel.isBlank()) "" else ValidarTelefonoChile.normalizar(tel) ?: return false
        val id = db.registrarNuevoUsuario(
            rut = rut,
            nombre = nombre,
            correo = correo,
            telefono = telefonoNormalizado,
            clavePlana = clave,
            rolCodigo = rol,
            pregunta = preg,
            respuestaPlana = resp
        )
        if (id > 0) {
            _usuarioActual.value = null
            sesionPreferencias.borrar()
            return true
        }
        return false
    }

    fun crearCuentaAdministrativa(
        actor: Usuario?,
        rut: String,
        nombre: String,
        correo: String,
        telefono: String,
        rol: String,
        rutEmpresa: String? = null,
        razonSocial: String? = null,
        giro: String? = null,
        direccion: String? = null,
        contactoNombre: String? = null,
        contactoCorreo: String? = null,
        contactoTelefono: String? = null,
        nombreInstalacion: String? = null
    ): CrearCuentaResultado {
        if (actor == null ||
            actor.rolCodigo !in setOf(Rol.CODIGO_INGENIERO, Rol.CODIGO_JEFE_TECNICO)
        ) {
            return CrearCuentaResultado(
                campoError = "general",
                mensajeError = "Solo Ingeniería o Jefatura Técnica pueden crear cuentas."
            )
        }

        val passwordTemporal = generarContrasenaTemporal()
        val resultado = db.crearCuentaAdministrativa(
            actorId = actor.idUsuario,
            rut = rut,
            nombre = nombre,
            correo = correo,
            telefono = telefono,
            rolCodigo = rol,
            contrasenaTemporal = passwordTemporal,
            rutEmpresa = rutEmpresa,
            razonSocial = razonSocial,
            giro = giro,
            direccion = direccion,
            contactoNombre = contactoNombre,
            contactoCorreo = contactoCorreo,
            contactoTelefono = contactoTelefono,
            nombreInstalacion = nombreInstalacion
        )
        if (resultado.exitosa &&
            !ValidarContrasena.esSegura(resultado.contrasenaTemporal.orEmpty())
        ) {
            error("La contraseña temporal generada no cumple la política de seguridad.")
        }
        if (resultado.exitosa) {
            sincronizarCuentaAdministrativa(
                actor,
                resultado.idUsuario ?: 0L,
                rut,
                nombre,
                correo,
                telefono,
                rol,
                rutEmpresa,
                razonSocial,
                giro,
                direccion,
                contactoNombre,
                contactoCorreo,
                contactoTelefono,
                nombreInstalacion
            )
        }
        return resultado
    }

    fun limpiarEstadoAltaRemota() {
        accionReintentarAlta = null
        _altaRemota.value = AltaRemotaUiState()
    }

    fun reintentarAltaRemota() {
        val accion = accionReintentarAlta
        if (accion == null) {
            return
        }
        accion()
    }

    private fun sincronizarCuentaAdministrativa(
        actor: Usuario,
        idUsuarioLocal: Long,
        rut: String,
        nombreCompleto: String,
        correo: String,
        telefono: String,
        rol: String,
        rutEmpresa: String?,
        razonSocial: String?,
        giro: String?,
        direccion: String?,
        contactoNombre: String?,
        contactoCorreo: String?,
        contactoTelefono: String?,
        nombreInstalacion: String?
    ) {
        accionReintentarAlta = {
            sincronizarCuentaAdministrativa(
                actor,
                idUsuarioLocal,
                rut,
                nombreCompleto,
                correo,
                telefono,
                rol,
                rutEmpresa,
                razonSocial,
                giro,
                direccion,
                contactoNombre,
                contactoCorreo,
                contactoTelefono,
                nombreInstalacion
            )
        }
        _altaRemota.value = AltaRemotaUiState(cargando = true)
        viewModelScope.launch {
            try {
                val usuarioLocal = db.buscarPorIdUsuario(idUsuarioLocal)
                var idEmpresaLocal = usuarioLocal?.idEmpresaCliente
                if (idEmpresaLocal == null) {
                    idEmpresaLocal = actor.idEmpresaCliente
                }
                if (idEmpresaLocal == null) {
                    throw ApiRequestException(
                        400,
                        "El microservicio requiere una empresa asociada para crear esta cuenta."
                    )
                }

                var empresaRemota = repositorio.leerEmpresaCacheada(idEmpresaLocal)
                if (empresaRemota == null) {
                    val empresasRemotas = repositorio.empresas()
                    for (empresa in empresasRemotas) {
                        val rutRemoto = leerTextoJson(empresa, "rut")
                        var idRemoto = leerLongJson(empresa, "idEmpresa")
                        if (idRemoto < 0) {
                            idRemoto = leerLongJson(empresa, "id")
                        }
                        if ((!rutEmpresa.isNullOrBlank() && rutRemoto == rutEmpresa) ||
                            (rutEmpresa.isNullOrBlank() && idRemoto == idEmpresaLocal)
                        ) {
                            empresaRemota = empresa
                            break
                        }
                    }
                }

                if (empresaRemota != null) {
                    repositorio.vincularEmpresaCacheada(idEmpresaLocal, empresaRemota)
                } else if (rol.equals(Rol.CODIGO_CLIENTE, ignoreCase = true)) {
                    if (razonSocial.isNullOrBlank() || rutEmpresa.isNullOrBlank()) {
                        throw ApiRequestException(
                            400,
                            "Completa la razón social y el RUT de la empresa para sincronizar la cuenta."
                        )
                    }
                    val solicitudEmpresa = JsonObject()
                    solicitudEmpresa.addProperty("nombre", razonSocial)
                    solicitudEmpresa.addProperty("rut", rutEmpresa)
                    solicitudEmpresa.addProperty("giro", giro)
                    solicitudEmpresa.addProperty("direccion", direccion)
                    solicitudEmpresa.addProperty("contactoNombre", contactoNombre)
                    solicitudEmpresa.addProperty("contactoCorreo", contactoCorreo)
                    solicitudEmpresa.addProperty("contactoTelefono", contactoTelefono)
                    repositorio.crearEmpresa(solicitudEmpresa, idEmpresaLocal)
                } else {
                    throw ApiRequestException(
                        404,
                        "La empresa de la cuenta no existe en el servidor. Registra primero la empresa del usuario."
                    )
                }

                val nombres = nombreCompleto.trim().split(Regex("\\s+"), limit = 2)
                if (nombres.size < 2 || nombres[1].isBlank()) {
                    throw ApiRequestException(
                        400,
                        "Ingresa nombre y apellido para cumplir el formato del microservicio."
                    )
                }
                val rolRemoto = when (rol.uppercase()) {
                    Rol.CODIGO_TECNICO -> 1L
                    Rol.CODIGO_CLIENTE -> 2L
                    Rol.CODIGO_INGENIERO -> 3L
                    Rol.CODIGO_JEFE_TECNICO -> 4L
                    else -> throw ApiRequestException(400, "El rol seleccionado no está soportado.")
                }
                val solicitudUsuario = JsonObject()
                solicitudUsuario.addProperty("actorId", actor.idUsuario)
                solicitudUsuario.addProperty("nombre", nombres[0])
                solicitudUsuario.addProperty("apellido", nombres[1])
                solicitudUsuario.addProperty("rut", rut)
                solicitudUsuario.addProperty("correo", correo.trim())
                solicitudUsuario.addProperty("telefono", telefono.trim())
                solicitudUsuario.addProperty("empresaId", idEmpresaLocal)
                solicitudUsuario.addProperty("rolId", rolRemoto)
                val resultadoUsuario = repositorio.crearUsuario(
                    solicitudUsuario,
                    idEmpresaLocal,
                    idUsuarioLocal
                )
                var altaPendiente = resultadoUsuario.enCola
                if (rol.equals(Rol.CODIGO_CLIENTE, ignoreCase = true) &&
                    !nombreInstalacion.isNullOrBlank()
                ) {
                    val solicitudInstalacion = JsonObject()
                    solicitudInstalacion.addProperty("idEmpresaLocal", idEmpresaLocal)
                    solicitudInstalacion.addProperty("idUsuarioClienteLocal", idUsuarioLocal)
                    solicitudInstalacion.addProperty("nombre", nombreInstalacion.trim())
                    solicitudInstalacion.addProperty("activa", true)
                    val instalacionLocal = db.listarInstalacionesDeCuentaCliente(idUsuarioLocal)
                        .firstOrNull {
                            it.nombre.equals(nombreInstalacion.trim(), ignoreCase = true)
                        }
                    val resultadoInstalacion = repositorio.crearInstalacion(
                        solicitudInstalacion,
                        instalacionLocal?.idInstalacion
                    )
                    if (resultadoInstalacion.enCola) {
                        altaPendiente = true
                    }
                }
                _altaRemota.value = if (altaPendiente) {
                    AltaRemotaUiState(
                        pendiente = true,
                        mensaje = "Alta guardada en la cola; la empresa, la cuenta y la instalación se enviarán al recuperar conexión."
                    )
                } else {
                    AltaRemotaUiState(
                        mensaje = if (rol.equals(Rol.CODIGO_CLIENTE, ignoreCase = true)) {
                            "Cuenta e instalación creadas en el servidor. El microservicio enviará las credenciales temporales al correo del usuario."
                        } else {
                            "Cuenta creada en el servidor. El microservicio enviará las credenciales temporales al correo del usuario."
                        }
                    )
                }
                accionReintentarAlta = null
            } catch (error: ApiRequestException) {
                _altaRemota.value = AltaRemotaUiState(error = error.message)
            } catch (_: java.io.IOException) {
                _altaRemota.value = AltaRemotaUiState(
                    error = "No se pudo sincronizar la cuenta. Verifica la conexión y vuelve a intentarlo."
                )
            }
        }
    }

    private fun leerTextoJson(json: JsonObject, clave: String): String {
        val valor = json.get(clave)
        if (valor == null || valor.isJsonNull) {
            return ""
        }
        return valor.asString
    }

    private fun leerLongJson(json: JsonObject, clave: String): Long {
        val valor = json.get(clave)
        if (valor == null || valor.isJsonNull) {
            return -1L
        }
        return valor.asLong
    }

    fun validarDisponibilidadCuenta(rut: String, correo: String): Map<String, String> =
        buildMap {
            if (db.existeRutUsuario(rut)) {
                put("rut", "Ya existe una cuenta con ese RUT.")
            }
            if (db.existeCorreoUsuario(correo)) {
                put("correo", "Ya existe una cuenta con ese correo.")
            }
        }

    fun validarDisponibilidadEmpresa(rut: String, razonSocial: String): Map<String, String> =
        buildMap {
            if (db.existeRutEmpresa(rut)) {
                put("rutEmpresa", "Ya existe una empresa con ese RUT.")
            }
            if (db.existeRazonSocialEmpresa(razonSocial)) {
                put("razonSocial", "Ya existe una empresa con esa razón social.")
            }
        }

    private fun generarContrasenaTemporal(): String {
        val mayusculas = "ABCDEFGHJKLMNPQRSTUVWXYZ"
        val minusculas = "abcdefghijkmnopqrstuvwxyz"
        val numeros = "23456789"
        val especiales = "!@#$%&*+-_"
        val todos = mayusculas + minusculas + numeros + especiales
        val caracteres = mutableListOf(
            mayusculas[secureRandom.nextInt(mayusculas.length)],
            minusculas[secureRandom.nextInt(minusculas.length)],
            numeros[secureRandom.nextInt(numeros.length)],
            especiales[secureRandom.nextInt(especiales.length)]
        )
        repeat(12) {
            caracteres += todos[secureRandom.nextInt(todos.length)]
        }
        for (i in caracteres.lastIndex downTo 1) {
            val j = secureRandom.nextInt(i + 1)
            val temporal = caracteres[i]
            caracteres[i] = caracteres[j]
            caracteres[j] = temporal
        }
        return caracteres.joinToString("")
    }

    fun cambiarAvatarDesdeCamara(foto: Bitmap) {
        guardarAvatar(ConvertidorImagen.aBytes(foto))
    }

    fun cambiarAvatarDesdeGaleria(uri: Uri) {
        val imagen = ConvertidorImagen.desdeUri(getApplication(), uri) ?: return
        guardarAvatar(ConvertidorImagen.aBytes(imagen))
    }

    private fun guardarAvatar(bytes: ByteArray) {
        val usuario = _usuarioActual.value ?: return
        if (db.actualizarFotoUsuario(usuario.idUsuario, bytes)) {
            _usuarioActual.value = db.buscarPorIdUsuario(usuario.idUsuario)
        }
    }

    fun cambiarClave(claveActual: String, claveNueva: String): Boolean {
        val usuario = _usuarioActual.value ?: return false
        val exito = db.cambiarClave(usuario.idUsuario, claveActual, claveNueva)
        if (exito) {
            _usuarioActual.value = db.buscarPorIdUsuario(usuario.idUsuario)
        }
        return exito
    }

    fun actualizarPerfil(nombre: String, telefono: String): Boolean {
        val usuario = _usuarioActual.value ?: return false
        val telefonoNormalizado = ValidarTelefonoChile.normalizar(telefono) ?: return false
        if (!db.actualizarPerfilUsuario(usuario.idUsuario, nombre, telefonoNormalizado)) return false
        val usuarioActualizado = db.buscarPorIdUsuario(usuario.idUsuario) ?: return false
        _usuarioActual.value = usuarioActualizado
        return true
    }

    fun existeUsuarioActivo(correo: String): Boolean {
        return db.existeUsuarioActivo(correo)
    }

    fun recuperarClaveConRespuesta(
        correo: String,
        pregunta: String,
        respuesta: String,
        nuevaClave: String
    ): Boolean {
        return db.recuperarClaveConRespuesta(correo, pregunta, respuesta, nuevaClave)
    }

    override fun onCleared() {
        repositorio.cerrar()
        db.close()
        super.onCleared()
    }
}