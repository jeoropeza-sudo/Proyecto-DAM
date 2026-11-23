package cl.duocuc.Induztek.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/auth/login")
    suspend fun iniciarSesion(@Body solicitud: LoginRequest): Response<LoginResponse>

    @POST("api/auth/olvide")
    suspend fun solicitarCodigo(@Body solicitud: SolicitudCorreoRecuperacion): Response<Unit>

    @POST("api/auth/restablecer")
    suspend fun restablecerClave(@Body solicitud: SolicitudRestablecimiento): Response<Unit>

    @POST("api/auth/configurar")
    suspend fun cambiarContrasena(@Body solicitud: SolicitudCambioContrasena): Response<LoginResponse>
}
