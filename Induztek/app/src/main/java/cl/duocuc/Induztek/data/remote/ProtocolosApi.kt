package cl.duocuc.Induztek.data.remote

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.Path

interface ProtocolosApi {
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

    @PATCH("api/protocolos/{id}/estado")
    suspend fun cambiarEstado(
        @Path("id") id: Long,
        @Body solicitud: SolicitudEstadoProtocolo
    ): Response<JsonObject>

}
