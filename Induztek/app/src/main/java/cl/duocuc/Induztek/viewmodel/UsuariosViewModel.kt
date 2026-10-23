package cl.duocuc.Induztek.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import cl.duocuc.Induztek.data.ConvertidorImagen
import cl.duocuc.Induztek.data.InduztekDbHelper
import cl.duocuc.Induztek.data.SesionPreferencias
import cl.duocuc.Induztek.model.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UsuariosViewModel(application: Application) : AndroidViewModel(application) {

    private val db = InduztekDbHelper(application)
    private val sesionPreferencias = SesionPreferencias(application)

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
        val usuario = db.autenticarUsuario(correo, clave)
        if (usuario != null) {
            _usuarioActual.value = usuario
            sesionPreferencias.guardar(usuario.idUsuario)
            _errorAuth.value = null
        } else {
            _errorAuth.value = "Credenciales incorrectas. Verifique correo y contraseña."
        }
        return usuario
    }

    fun cerrarSesion() {
        _usuarioActual.value = null
        sesionPreferencias.borrar()
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
        val id = db.registrarNuevoUsuario(
            rut = rut,
            nombre = nombre,
            correo = correo,
            telefono = tel,
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

    fun obtenerPreguntaSeguridad(correo: String): String? = db.obtenerPreguntaSeguridad(correo)

    fun recuperarClaveConRespuesta(correo: String, respuesta: String, nuevaClave: String): Boolean {
        return db.recuperarClaveConRespuesta(correo, respuesta, nuevaClave)
    }

    override fun onCleared() {
        db.close()
        super.onCleared()
    }
}