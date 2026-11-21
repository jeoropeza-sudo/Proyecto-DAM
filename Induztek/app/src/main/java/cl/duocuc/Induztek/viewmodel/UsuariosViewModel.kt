package cl.duocuc.Induztek.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import cl.duocuc.Induztek.data.ConvertidorImagen
import cl.duocuc.Induztek.data.InduztekDbHelper
import cl.duocuc.Induztek.data.SesionPreferencias
import cl.duocuc.Induztek.model.CrearCuentaResultado
import cl.duocuc.Induztek.model.Rol
import cl.duocuc.Induztek.model.Usuario
import cl.duocuc.Induztek.model.ValidarContrasena
import cl.duocuc.Induztek.model.ValidarTelefonoChile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.SecureRandom

class UsuariosViewModel(application: Application) : AndroidViewModel(application) {

    private val db = InduztekDbHelper(application)
    private val sesionPreferencias = SesionPreferencias(application)
    private val secureRandom = SecureRandom()

    private val _usuarioActual = MutableStateFlow<Usuario?>(null)
    val usuarioActual: StateFlow<Usuario?> = _usuarioActual.asStateFlow()

    private val _errorAuth = MutableStateFlow<String?>(null)
    val errorAuth: StateFlow<String?> = _errorAuth.asStateFlow()

    init {
        val idUsuario = sesionPreferencias.leer()
        if (idUsuario > 0) {
            val usuario = db.buscarPorIdUsuario(idUsuario)
            if (usuario?.activo == true) {
                _usuarioActual.value = usuario
            } else {
                sesionPreferencias.borrar()
            }
        }
    }

    fun iniciarSesion(correo: String, clave: String): Usuario? {
        if (correo.isBlank()) {
            _errorAuth.value = "Ingresa el correo electrónico de tu cuenta."
            return null
        }
        if (clave.isBlank()) {
            _errorAuth.value = "Ingresa la contraseña de tu cuenta."
            return null
        }

        val usuario = db.autenticarUsuario(correo, clave)
        if (usuario != null) {
            _usuarioActual.value = usuario
            sesionPreferencias.guardar(usuario.idUsuario)
            _errorAuth.value = null
        } else {
            _errorAuth.value = "El correo o la contraseña no coinciden. Verifica ambos datos e inténtalo nuevamente."
        }
        return usuario
    }

    fun cerrarSesion() {
        _usuarioActual.value = null
        sesionPreferencias.borrar()
        _errorAuth.value = null
    }

    fun limpiarErrorAuth() {
        _errorAuth.value = null
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
        return db.crearCuentaAdministrativa(
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
        ).let { resultado ->
            if (resultado.exitosa &&
                !ValidarContrasena.esSegura(resultado.contrasenaTemporal.orEmpty())
            ) {
                error("La contraseña temporal generada no cumple la política de seguridad.")
            }
            resultado
        }
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
        db.close()
        super.onCleared()
    }
}