package cl.duocuc.Induztek.data.remote

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface UsuariosApi {
    @GET("api/usuarios")
    suspend fun usuarios(@Query("buscar") buscar: String? = null): Response<List<UsuarioResponse>>

    @GET("api/usuarios/{id}")
    suspend fun usuario(@Path("id") id: Long): Response<UsuarioResponse>

    @POST("api/usuarios/alta")
    suspend fun crearUsuario(@Body solicitud: JsonObject): Response<JsonObject>

    @PATCH("api/usuarios/{id}/perfil")
    suspend fun actualizarPerfil(
        @Path("id") id: Long,
        @Body solicitud: SolicitudActualizacionPerfil
    ): Response<UsuarioResponse>
}
