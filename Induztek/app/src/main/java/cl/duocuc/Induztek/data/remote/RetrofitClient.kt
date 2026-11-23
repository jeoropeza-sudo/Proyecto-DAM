package cl.duocuc.Induztek.data.remote

import android.content.Context
import cl.duocuc.Induztek.BuildConfig
import cl.duocuc.Induztek.data.SesionPreferencias
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

data class MicroserviciosApi(
    val auth: AuthApi,
    val usuarios: UsuariosApi,
    val empresas: EmpresasApi,
    val equipos: EquiposApi,
    val protocolos: ProtocolosApi,
    val instrumentos: InstrumentosApi,
    val avatar: AvatarApi
)

interface AvatarApi {
    @Multipart
    @POST("api/usuarios/{id}/avatar")
    suspend fun subir(
        @Path("id") id: Long,
        @Part archivo: MultipartBody.Part
    ): Response<UsuarioResponse>

    @GET("api/usuarios/{id}/avatar")
    suspend fun obtener(@Path("id") id: Long): Response<ResponseBody>
}

object RetrofitClient {
    @Volatile
    private var servicios: MicroserviciosApi? = null

    @Synchronized
    fun crear(context: Context): MicroserviciosApi {
        servicios?.let { return it }

        val preferencias = SesionPreferencias(context.applicationContext)
        val interceptor = Interceptor { chain ->
            val solicitudOriginal = chain.request()
            val token = preferencias.leerToken()
            val solicitud = if (token.isNullOrBlank()) {
                solicitudOriginal
            } else {
                solicitudOriginal.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            }
            chain.proceed(solicitud)
        }
        val cliente = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()

        return MicroserviciosApi(
            auth = retrofit.create(AuthApi::class.java),
            usuarios = retrofit.create(UsuariosApi::class.java),
            empresas = retrofit.create(EmpresasApi::class.java),
            equipos = retrofit.create(EquiposApi::class.java),
            protocolos = retrofit.create(ProtocolosApi::class.java),
            instrumentos = retrofit.create(InstrumentosApi::class.java),
            avatar = retrofit.create(AvatarApi::class.java)
        ).also { servicios = it }
    }
}
