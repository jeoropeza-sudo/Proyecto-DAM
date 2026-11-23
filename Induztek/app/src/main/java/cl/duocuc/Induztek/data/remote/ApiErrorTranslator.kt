package cl.duocuc.Induztek.data.remote

import com.google.gson.Gson
import com.google.gson.JsonParseException
import retrofit2.Response

object ApiErrorTranslator {
    private val gson = Gson()

    fun <T> mensaje(response: Response<T>): String {
        val body = response.errorBody()
        val contenido = if (body == null) "" else body.string()
        return mensaje(response.code(), contenido)
    }

    fun mensaje(code: Int, contenido: String): String {
        if (contenido.isNotBlank()) {
            try {
                val error = gson.fromJson(contenido, ErrorResponse::class.java)
                val mensajeServidor = error.message ?: error.detail ?: error.error
                if (!mensajeServidor.isNullOrBlank()) {
                    return mensajeServidor
                }
            } catch (_: JsonParseException) {
                if (contenido.length < 300 && !contenido.trimStart().startsWith("<")) {
                    return contenido
                }
            }
        }
        return when (code) {
            400 -> "Revisa los datos ingresados e inténtalo nuevamente."
            401 -> "Tu sesión no es válida o tus credenciales son incorrectas. Inicia sesión nuevamente."
            403 -> "No tienes permiso para realizar esta acción."
            404 -> "No encontramos el registro solicitado."
            409 -> "El registro cambió en el servidor. Actualiza los datos antes de volver a guardar."
            422 -> "El servidor no pudo validar la información enviada."
            in 500..599 -> "El servicio no está disponible en este momento. Inténtalo más tarde."
            else -> "No se pudo completar la solicitud. Código HTTP $code."
        }
    }
}
