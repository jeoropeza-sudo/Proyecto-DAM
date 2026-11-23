package cl.duocuc.Induztek.data.repository

import android.content.Context
import cl.duocuc.Induztek.data.remote.LoginRequest
import cl.duocuc.Induztek.data.remote.LoginResponse
import cl.duocuc.Induztek.data.remote.MicroserviciosApi
import cl.duocuc.Induztek.data.remote.RetrofitClient
import cl.duocuc.Induztek.data.remote.SolicitudActualizacionPerfil
import cl.duocuc.Induztek.data.remote.SolicitudCambioContrasena
import cl.duocuc.Induztek.data.remote.SolicitudCorreoRecuperacion
import cl.duocuc.Induztek.data.remote.SolicitudRestablecimiento
import cl.duocuc.Induztek.data.remote.UsuarioResponse
import cl.duocuc.Induztek.data.remote.exigirCuerpo
import cl.duocuc.Induztek.data.remote.exigirExito
import com.google.gson.JsonObject

class UsuarioRepository(context: Context) {
    private val apis: MicroserviciosApi = RetrofitClient.crear(context.applicationContext)

    suspend fun iniciarSesion(correo: String, contrasena: String): LoginResponse =
        apis.auth.iniciarSesion(LoginRequest(correo.trim(), contrasena)).exigirCuerpo()

    suspend fun usuario(id: Long): UsuarioResponse =
        apis.usuarios.usuario(id).exigirCuerpo()

    suspend fun actualizarPerfil(id: Long, nombre: String, apellido: String, telefono: String): UsuarioResponse =
        apis.usuarios.actualizarPerfil(
            id,
            SolicitudActualizacionPerfil(nombre, apellido, telefono)
        ).exigirCuerpo()

    suspend fun cambiarContrasena(
        correo: String,
        actual: String,
        nueva: String
    ): LoginResponse =
        apis.auth.cambiarContrasena(
            SolicitudCambioContrasena(correo.trim(), actual, nueva)
        ).exigirCuerpo()

    suspend fun solicitarCodigo(correo: String) {
        apis.auth.solicitarCodigo(SolicitudCorreoRecuperacion(correo.trim())).exigirExito()
    }

    suspend fun restablecerClave(correo: String, codigo: String, clave: String) {
        apis.auth.restablecerClave(
            SolicitudRestablecimiento(correo.trim(), codigo.trim(), clave)
        ).exigirExito()
    }

    suspend fun empresas(): List<JsonObject> =
        apis.empresas.empresas().exigirCuerpo()

    suspend fun crearEmpresa(solicitud: JsonObject): JsonObject =
        apis.empresas.crearEmpresa(solicitud).exigirCuerpo()

    suspend fun crearUsuario(solicitud: JsonObject): JsonObject =
        apis.usuarios.crearUsuario(solicitud).exigirCuerpo()

    suspend fun crearInstalacion(solicitud: JsonObject) {
        apis.equipos.crearInstalacion(solicitud).exigirExito()
    }
}
