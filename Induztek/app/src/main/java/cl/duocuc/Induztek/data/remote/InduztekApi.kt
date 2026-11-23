package cl.duocuc.Induztek.data.remote

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface InduztekApi {
    @POST("api/auth/login")
    suspend fun iniciarSesion(@Body solicitud: LoginRequest): Response<LoginResponse>

    @POST("api/auth/olvide")
    suspend fun solicitarCodigo(@Body solicitud: SolicitudCorreoRecuperacion): Response<Unit>

    @POST("api/auth/restablecer")
    suspend fun restablecerClave(@Body solicitud: SolicitudRestablecimiento): Response<Unit>

    @GET("api/usuarios/{id}")
    suspend fun usuario(@Path("id") id: Long): Response<UsuarioResponse>

    @GET("api/roles")
    suspend fun roles(): Response<List<RolResponse>>

    @POST("api/usuarios/alta")
    suspend fun crearUsuario(@Body solicitud: JsonObject): Response<JsonObject>

    @GET("api/empresas")
    suspend fun empresas(): Response<List<JsonObject>>

    @GET("api/empresas/{id}")
    suspend fun empresa(@Path("id") id: Long): Response<JsonObject>

    @POST("api/empresas")
    suspend fun crearEmpresa(@Body solicitud: JsonObject): Response<JsonObject>

    @PUT("api/empresas/{id}")
    suspend fun actualizarEmpresa(
        @Path("id") id: Long,
        @Body solicitud: JsonObject
    ): Response<JsonObject>

    @GET("api/instalaciones")
    suspend fun instalaciones(): Response<List<JsonObject>>

    @GET("api/instalaciones/empresa/{id}")
    suspend fun instalacionesEmpresa(@Path("id") id: Long): Response<List<JsonObject>>

    @GET("api/instalaciones/cliente/{id}")
    suspend fun instalacionesCliente(@Path("id") id: Long): Response<List<JsonObject>>

    @GET("api/instalaciones/tecnico/{id}")
    suspend fun instalacionesTecnico(@Path("id") id: Long): Response<List<JsonObject>>

    @POST("api/instalaciones")
    suspend fun crearInstalacion(@Body solicitud: JsonObject): Response<JsonObject>

    @PUT("api/instalaciones/{id}")
    suspend fun actualizarInstalacion(
        @Path("id") id: Long,
        @Body solicitud: JsonObject
    ): Response<JsonObject>

    @DELETE("api/instalaciones/{id}")
    suspend fun desactivarInstalacion(@Path("id") id: Long): Response<Unit>

    @GET("api/equipos")
    suspend fun equipos(): Response<List<JsonObject>>

    @GET("api/equipos/cliente/{id}")
    suspend fun equiposCliente(@Path("id") id: Long): Response<List<JsonObject>>

    @GET("api/equipos/instalacion/{id}")
    suspend fun equiposInstalacion(@Path("id") id: Long): Response<List<JsonObject>>

    @GET("api/equipos/tecnico/{id}")
    suspend fun equiposTecnico(@Path("id") id: Long): Response<List<JsonObject>>

    @POST("api/equipos")
    suspend fun crearEquipo(@Body solicitud: JsonObject): Response<JsonElement>

    @PUT("api/equipos/{id}")
    suspend fun actualizarEquipo(
        @Path("id") id: Long,
        @Body solicitud: JsonObject
    ): Response<JsonObject>

    @DELETE("api/equipos/{id}")
    suspend fun eliminarEquipo(@Path("id") id: Long): Response<Unit>

    @GET("api/asignaciones/tecnico/{id}/ids")
    suspend fun idsInstalacionesAsignadas(@Path("id") id: Long): Response<List<Long>>

    @GET("api/asignaciones/tecnico/{id}")
    suspend fun asignacionesTecnico(@Path("id") id: Long): Response<List<JsonObject>>

    @PUT("api/asignaciones/tecnico/{id}")
    suspend fun asignarInstalaciones(
        @Path("id") id: Long,
        @Body solicitud: SolicitudAsignacion
    ): Response<List<JsonObject>>

    @DELETE("api/asignaciones/tecnico/{idTecnico}/instalacion/{idInstalacion}")
    suspend fun retirarAsignacion(
        @Path("idTecnico") idTecnico: Long,
        @Path("idInstalacion") idInstalacion: Long
    ): Response<Unit>

    @GET("api/visitas")
    suspend fun visitas(
        @Query("instalacion") instalacion: Long? = null,
        @Query("tecnico") tecnico: Long? = null,
        @Query("estado") estado: String? = null
    ): Response<List<JsonObject>>

    @GET("api/visitas/{id}")
    suspend fun visita(@Path("id") id: Long): Response<JsonObject>

    @POST("api/visitas")
    suspend fun crearVisita(@Body solicitud: JsonObject): Response<JsonObject>

    @PUT("api/visitas/{id}")
    suspend fun actualizarVisita(
        @Path("id") id: Long,
        @Body solicitud: JsonObject
    ): Response<JsonObject>

    @POST("api/visitas/{id}/equipos")
    suspend fun agregarEquipoVisita(
        @Path("id") id: Long,
        @Body solicitud: JsonObject
    ): Response<JsonObject>

    @PATCH("api/visitas/{id}/cerrar")
    suspend fun cerrarVisitaRemota(@Path("id") id: Long): Response<JsonObject>

    @GET("api/visitas/{id}/equipos")
    suspend fun equiposVisita(@Path("id") id: Long): Response<List<JsonObject>>

    @GET("api/protocolos")
    suspend fun protocolos(): Response<List<JsonObject>>

    @GET("api/protocolos/equipo/{id}")
    suspend fun protocolosEquipo(@Path("id") id: Long): Response<List<JsonObject>>

    @GET("api/protocolos/cliente/{id}")
    suspend fun protocolosCliente(@Path("id") id: Long): Response<List<JsonObject>>

    @GET("api/protocolos/visita/{id}")
    suspend fun protocolosVisita(@Path("id") id: Long): Response<List<JsonObject>>

    @GET("api/protocolos/{id}")
    suspend fun protocolo(@Path("id") id: Long): Response<JsonObject>

    @GET("api/protocolos/{id}/mediciones")
    suspend fun mediciones(@Path("id") id: Long): Response<List<JsonObject>>

    @POST("api/protocolos/con-mediciones")
    suspend fun crearProtocolo(@Body solicitud: ProtocoloPendienteRequest): Response<JsonObject>

    @PATCH("api/protocolos/{id}/formalizar")
    suspend fun formalizarProtocolo(@Path("id") id: Long): Response<JsonObject>

    @GET("api/instrumentos")
    suspend fun instrumentos(
        @Query("tipoPrueba") tipoPrueba: String? = null,
        @Query("activo") activo: Boolean? = null
    ): Response<List<JsonObject>>

    @POST("api/instrumentos")
    suspend fun crearInstrumento(@Body solicitud: JsonObject): Response<JsonObject>

    @PUT("api/instrumentos/{id}")
    suspend fun actualizarInstrumento(
        @Path("id") id: Long,
        @Body solicitud: JsonObject
    ): Response<JsonObject>

    @PATCH("api/instrumentos/{id}/estado")
    suspend fun cambiarEstadoInstrumento(
        @Path("id") id: Long,
        @Query("activo") activo: Boolean
    ): Response<JsonObject>

    @POST("api/instrumentos/{id}/calibraciones")
    suspend fun registrarCalibracion(
        @Path("id") id: Long,
        @Body solicitud: JsonObject
    ): Response<JsonObject>

    @GET("api/instrumentos/{id}/calibraciones")
    suspend fun calibraciones(@Path("id") id: Long): Response<List<JsonObject>>
}
