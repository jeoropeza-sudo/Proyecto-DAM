package cl.duocuc.Induztek.data.remote

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.PUT
import retrofit2.http.Query

interface EquiposApi {
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

    @GET("api/equipos/cliente/{id}")
    suspend fun equiposCliente(@Path("id") id: Long): Response<List<JsonObject>>

    @GET("api/equipos/instalacion/{id}")
    suspend fun equiposInstalacion(@Path("id") id: Long): Response<List<JsonObject>>

    @GET("api/equipos/tecnico/{id}")
    suspend fun equiposTecnico(@Path("id") id: Long): Response<List<JsonObject>>

    @POST("api/equipos")
    suspend fun crearEquipo(@Body solicitud: JsonObject): Response<JsonElement>

    @GET("api/asignaciones/tecnico/{id}")
    suspend fun asignacionesTecnico(@Path("id") id: Long): Response<List<JsonObject>>

    @GET("api/asignaciones/tecnico/{id}/ids")
    suspend fun idsAsignacionesTecnico(@Path("id") id: Long): Response<List<Long>>

    @PUT("api/asignaciones/tecnico/{id}")
    suspend fun asignarInstalaciones(
        @Path("id") id: Long,
        @Body solicitud: SolicitudAsignacion
    ): Response<List<JsonObject>>

    @GET("api/visitas")
    suspend fun visitas(
        @Query("instalacion") instalacion: Long? = null,
        @Query("tecnico") tecnico: Long? = null,
        @Query("estado") estado: String? = null
    ): Response<List<JsonObject>>

    @POST("api/visitas")
    suspend fun crearVisita(@Body solicitud: JsonObject): Response<JsonObject>

    @POST("api/visitas/{id}/equipos")
    suspend fun agregarEquipoVisita(
        @Path("id") id: Long,
        @Body solicitud: JsonObject
    ): Response<JsonObject>

    @PATCH("api/visitas/{id}/cerrar")
    suspend fun cerrarVisitaRemota(@Path("id") id: Long): Response<JsonObject>

}
