package cl.duocuc.Induztek.data.remote

import android.content.Context
import cl.duocuc.Induztek.BuildConfig
import cl.duocuc.Induztek.data.SesionPreferencias
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private var api: InduztekApi? = null

    @Synchronized
    fun crear(context: Context): InduztekApi {
        val existente = api
        if (existente != null) {
            return existente
        }

        val sesionPreferencias = SesionPreferencias(context.applicationContext)
        val interceptor = Interceptor { chain ->
            val solicitudOriginal = chain.request()
            val token = sesionPreferencias.leerToken()
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
        val servicio = retrofit.create(InduztekApi::class.java)
        api = servicio
        return servicio
    }
}
