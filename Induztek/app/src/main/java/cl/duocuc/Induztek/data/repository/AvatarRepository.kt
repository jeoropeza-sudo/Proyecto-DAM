package cl.duocuc.Induztek.data.repository

import android.content.Context
import cl.duocuc.Induztek.data.remote.ApiRequestException
import cl.duocuc.Induztek.data.remote.RetrofitClient
import cl.duocuc.Induztek.data.remote.exigirCuerpo
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType

class AvatarRepository(context: Context) {
    private val api = RetrofitClient.crear(context.applicationContext).avatar

    suspend fun obtener(idUsuario: Long): ByteArray? {
        return try {
            api.obtener(idUsuario).exigirCuerpo().bytes()
        } catch (error: ApiRequestException) {
            if (error.statusCode == 404) null else throw error
        }
    }

    suspend fun subir(idUsuario: Long, imagen: ByteArray): ByteArray {
        val archivo = MultipartBody.Part.createFormData(
            "archivo",
            "avatar.jpg",
            imagen.toRequestBody("image/jpeg".toMediaType())
        )
        api.subir(idUsuario, archivo).exigirCuerpo()
        return api.obtener(idUsuario).exigirCuerpo().bytes()
    }
}
