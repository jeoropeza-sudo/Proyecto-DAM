package cl.duocuc.Induztek.data.remote

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface InstrumentosApi {
    @GET("api/instrumentos")
    suspend fun instrumentos(
        @Query("tipoPrueba") tipoPrueba: String? = null,
        @Query("activo") activo: Boolean? = null
    ): Response<List<JsonObject>>

    @POST("api/instrumentos")
    suspend fun crearInstrumento(@Body solicitud: JsonObject): Response<JsonObject>

    @PATCH("api/instrumentos/{id}/estado")
    suspend fun cambiarEstadoInstrumento(
        @Path("id") id: Long,
        @Query("activo") activo: Boolean
    ): Response<JsonObject>

    @GET("api/instrumentos/{id}/calibraciones")
    suspend fun calibraciones(@Path("id") id: Long): Response<List<JsonObject>>

    @POST("api/instrumentos/{id}/calibraciones")
    suspend fun registrarCalibracion(
        @Path("id") id: Long,
        @Body solicitud: JsonObject
    ): Response<JsonObject>
}
