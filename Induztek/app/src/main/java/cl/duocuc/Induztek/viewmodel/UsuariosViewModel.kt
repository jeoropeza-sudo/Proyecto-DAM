package cl.duocuc.Induztek.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duocuc.Induztek.data.ConvertidorImagen
import cl.duocuc.Induztek.data.SesionPreferencias
import cl.duocuc.Induztek.data.repository.AvatarRepository
import cl.duocuc.Induztek.data.repository.UsuarioRepository
import cl.duocuc.Induztek.data.remote.ApiRequestException
import com.google.gson.JsonObject
import cl.duocuc.Induztek.model.CrearCuentaResultado
import cl.duocuc.Induztek.model.Rol
import cl.duocuc.Induztek.model.Usuario
import cl.duocuc.Induztek.model.ValidarTelefonoChile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecuperacionUiState(
    val codigoSolicitado: Boolean = false,
    val cargando: Boolean = false,
    val mensaje: String? = null,
    val error: String? = null,
    val claveActualizada: Boolean = false
)

data class AltaRemotaUiState(
    val cargando: Boolean = false,
    val mensaje: String? = null,
    val error: String? = null
)

class UsuariosViewModel(application: Application) : AndroidViewModel(application) {

    private val sesionPreferencias = SesionPreferencias(application)
    private val repositorioUsuarios = UsuarioRepository(application)
    private val repositorioAvatar = AvatarRepository(application)

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

    init {
        val idUsuario = sesionPreferencias.leer()
        val token = sesionPreferencias.leerToken()
        if (idUsuario > 0 && !token.isNullOrBlank()) {
            viewModelScope.launch {
                try {
                    val usuarioRemoto = repositorioUsuarios.usuario(idUsuario)
                    val usuario = convertirUsuario(usuarioRemoto).copy(
                        avatar = repositorioAvatar.obtener(idUsuario)
                    )
                    if (usuario.activo) {
                        _usuarioActual.value = usuario
                    } else {
                        cerrarSesion()
                    }
                } catch (error: ApiRequestException) {
                    if (error.statusCode == 401) {
                        cerrarSesion()
                    } else {
                        _errorAuth.value = error.message
                    }
                } catch (error: java.io.IOException) {
                    _errorAuth.value = "No se pudo validar la sesión con el servidor."
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
                val respuesta = repositorioUsuarios.iniciarSesion(correo, clave)
                sesionPreferencias.guardar(respuesta.usuarioId)
                sesionPreferencias.guardarToken(respuesta.token)
                val perfil = convertirUsuario(repositorioUsuarios.usuario(respuesta.usuarioId)).copy(
                    avatar = repositorioAvatar.obtener(respuesta.usuarioId)
                )
                if (!perfil.activo) {
                    sesionPreferencias.borrar()
                    _errorAuth.value = "La cuenta está inactiva. Contacta al administrador."
                    return@launch
                }
                val usuario = perfil.copy(
                    debeCambiarContrasena = respuesta.primerIngreso || perfil.debeCambiarContrasena
                )
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
                repositorioUsuarios.solicitarCodigo(correo)
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
                repositorioUsuarios.restablecerClave(correo, codigo, claveNueva)
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

        val telefonoNormalizado = ValidarTelefonoChile.normalizar(telefono)
            ?: return CrearCuentaResultado(
                campoError = "telefono",
                mensajeError = "Ingresa un teléfono móvil chileno válido."
            )
        if (rut.isBlank() || nombre.trim().split(Regex("\\s+")).size < 2 || correo.isBlank()) {
            return CrearCuentaResultado(
                campoError = "general",
                mensajeError = "Completa los datos obligatorios de la cuenta."
            )
        }

        _altaRemota.value = AltaRemotaUiState(cargando = true)
        crearCuentaEnServidor(
            actor,
            rut,
            nombre,
            correo,
            telefonoNormalizado,
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
        return CrearCuentaResultado(aceptada = true)
    }

    fun limpiarEstadoAltaRemota() {
        _altaRemota.value = AltaRemotaUiState()
    }

    private fun crearCuentaEnServidor(
        actor: Usuario,
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
        _altaRemota.value = AltaRemotaUiState(cargando = true)
        viewModelScope.launch {
            try {
                var empresaRemota = repositorioUsuarios.empresas().firstOrNull { empresa ->
                    val rutRemoto = leerTextoJson(empresa, "rut")
                    val idRemoto = leerLongJson(empresa, "idEmpresa", "id")
                    (!rutEmpresa.isNullOrBlank() && rutRemoto.equals(rutEmpresa, ignoreCase = true)) ||
                        (rutEmpresa.isNullOrBlank() && idRemoto == actor.idEmpresaCliente)
                }

                if (empresaRemota == null && rol.equals(Rol.CODIGO_CLIENTE, ignoreCase = true)) {
                    if (razonSocial.isNullOrBlank() || rutEmpresa.isNullOrBlank()) {
                        throw ApiRequestException(
                            400,
                            "Completa la razón social y el RUT de la empresa para registrarla."
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
                    empresaRemota = repositorioUsuarios.crearEmpresa(solicitudEmpresa)
                } else if (empresaRemota == null) {
                    throw ApiRequestException(
                        404,
                        "La empresa de la cuenta no existe en el servidor. Registra primero la empresa del usuario."
                    )
                }
                val empresaConfirmada = empresaRemota
                    ?: throw ApiRequestException(404, "La empresa no está registrada en el servidor.")
                val idEmpresaRemota = leerLongJson(empresaConfirmada, "idEmpresa", "id")
                if (idEmpresaRemota <= 0L) {
                    throw ApiRequestException(502, "El servidor no devolvió el identificador de la empresa.")
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
                solicitudUsuario.addProperty("empresaId", idEmpresaRemota)
                solicitudUsuario.addProperty("rolId", rolRemoto)
                val respuestaUsuario = repositorioUsuarios.crearUsuario(solicitudUsuario)
                if (rol.equals(Rol.CODIGO_CLIENTE, ignoreCase = true) &&
                    !nombreInstalacion.isNullOrBlank()
                ) {
                    val usuarioRemoto = respuestaUsuario.getAsJsonObject("usuario")
                    val idUsuarioRemoto = usuarioRemoto?.let { leerLongJson(it, "id") } ?: -1L
                    if (idUsuarioRemoto <= 0L) {
                        throw ApiRequestException(502, "El servidor no devolvió el identificador del usuario.")
                    }
                    val solicitudInstalacion = JsonObject()
                    solicitudInstalacion.addProperty("idEmpresa", idEmpresaRemota)
                    solicitudInstalacion.addProperty("idUsuarioCliente", idUsuarioRemoto)
                    solicitudInstalacion.addProperty("nombre", nombreInstalacion.trim())
                    solicitudInstalacion.addProperty("activa", true)
                    repositorioUsuarios.crearInstalacion(solicitudInstalacion)
                }
                _altaRemota.value = AltaRemotaUiState(
                    mensaje = if (rol.equals(Rol.CODIGO_CLIENTE, ignoreCase = true)) {
                        "Cuenta e instalación creadas en el servidor. El microservicio enviará las credenciales temporales al correo del usuario."
                    } else {
                        "Cuenta creada en el servidor. El microservicio enviará las credenciales temporales al correo del usuario."
                    }
                )
            } catch (error: ApiRequestException) {
                _altaRemota.value = AltaRemotaUiState(error = error.message)
            } catch (_: java.io.IOException) {
                _altaRemota.value = AltaRemotaUiState(
                    error = "No se pudo crear la cuenta en el servidor. Verifica la conexión e inténtalo nuevamente."
                )
            } catch (error: RuntimeException) {
                _altaRemota.value = AltaRemotaUiState(
                    error = error.message ?: "El servidor devolvió datos que no se pudieron interpretar."
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

    private fun leerLongJson(json: JsonObject, vararg claves: String): Long {
        for (clave in claves) {
            val valor = json.get(clave)
            if (valor != null && !valor.isJsonNull) {
                return valor.asLong
            }
        }
        return -1L
    }

    fun cambiarAvatarDesdeCamara(foto: Bitmap, alCompletar: (String?) -> Unit) {
        guardarAvatar(ConvertidorImagen.aBytes(foto), alCompletar)
    }

    fun cambiarAvatarDesdeGaleria(uri: Uri, alCompletar: (String?) -> Unit) {
        val imagen = ConvertidorImagen.desdeUri(getApplication(), uri)
        if (imagen == null) {
            alCompletar("No se pudo leer la imagen seleccionada.")
            return
        }
        guardarAvatar(ConvertidorImagen.aBytes(imagen), alCompletar)
    }

    private fun guardarAvatar(bytes: ByteArray, alCompletar: (String?) -> Unit) {
        val usuario = _usuarioActual.value ?: run {
            alCompletar("Inicia sesión nuevamente para cambiar el avatar.")
            return
        }
        viewModelScope.launch {
            try {
                val avatar = repositorioAvatar.subir(usuario.idUsuario, bytes)
                val usuarioActualizado = usuario.copy(avatar = avatar)
                _usuarioActual.value = usuarioActualizado
                alCompletar(null)
            } catch (error: ApiRequestException) {
                alCompletar(error.message)
            } catch (_: java.io.IOException) {
                alCompletar("No se pudo subir la foto de perfil.")
            }
        }
    }

    fun cambiarClave(
        claveActual: String,
        claveNueva: String,
        alCompletar: (String?) -> Unit
    ) {
        val usuario = _usuarioActual.value ?: run {
            alCompletar("Inicia sesión nuevamente para cambiar la contraseña.")
            return
        }
        viewModelScope.launch {
            try {
                val respuesta = repositorioUsuarios.cambiarContrasena(
                    usuario.correo,
                    claveActual,
                    claveNueva
                )
                sesionPreferencias.guardarToken(respuesta.token)
                _usuarioActual.value = usuario.copy(debeCambiarContrasena = false)
                alCompletar(null)
            } catch (error: ApiRequestException) {
                alCompletar(error.message)
            } catch (_: java.io.IOException) {
                alCompletar("No se pudo cambiar la contraseña. Verifica tu conexión.")
            }
        }
    }

    fun actualizarPerfil(
        nombre: String,
        telefono: String,
        alCompletar: (String?) -> Unit
    ) {
        val usuario = _usuarioActual.value ?: run {
            alCompletar("Inicia sesión nuevamente para actualizar el perfil.")
            return
        }
        val telefonoNormalizado = ValidarTelefonoChile.normalizar(telefono)
        if (telefonoNormalizado == null || nombre.isBlank()) {
            alCompletar("Ingresa un nombre y un teléfono válidos.")
            return
        }
        val partesNombre = nombre.trim().split(Regex("\\s+"), limit = 2)
        val nombrePila = partesNombre.firstOrNull().orEmpty()
        val apellido = partesNombre.getOrNull(1).orEmpty()
        if (apellido.isBlank()) {
            alCompletar("Ingresa nombre y apellido.")
            return
        }
        viewModelScope.launch {
            try {
                val respuesta = repositorioUsuarios.actualizarPerfil(
                    usuario.idUsuario,
                    nombrePila,
                    apellido,
                    telefonoNormalizado
                )
                val usuarioActualizado = convertirUsuario(respuesta).copy(avatar = usuario.avatar)
                _usuarioActual.value = usuarioActualizado
                alCompletar(null)
            } catch (error: ApiRequestException) {
                alCompletar(error.message)
            } catch (_: java.io.IOException) {
                alCompletar("No se pudo actualizar el perfil. Verifica tu conexión.")
            }
        }
    }
}