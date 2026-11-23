package cl.duocuc.Induztek.data.remote

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface EmpresasApi {
    @GET("api/empresas")
    suspend fun empresas(): Response<List<JsonObject>>

    @POST("api/empresas")
    suspend fun crearEmpresa(@Body solicitud: JsonObject): Response<JsonObject>
}
