package cl.duocuc.Induztek.data.remote

import com.google.gson.Gson
import com.google.gson.JsonParseException
import java.io.IOException
import retrofit2.Response

class ApiRequestException(
    val statusCode: Int,
    override val message: String
) : IOException(message)

private val apiErrorGson = Gson()

fun <T> Response<T>.exigirCuerpo(): T {
    exigirExito()
    return body()
        ?: throw ApiRequestException(code(), "El servidor devolvió una respuesta vacía.")
}

fun Response<*>.exigirExito() {
    if (isSuccessful) return

    val contenido = errorBody()?.string().orEmpty()
    val mensaje = if (contenido.isBlank()) {
        "No se pudo completar la solicitud. Código HTTP ${code()}."
    } else {
        try {
            val error = apiErrorGson.fromJson(contenido, ErrorResponse::class.java)
            error.message ?: error.detail ?: error.error ?: contenido
        } catch (_: JsonParseException) {
            contenido.takeIf { it.length < 300 && !it.trimStart().startsWith("<") }
                ?: "No se pudo completar la solicitud. Código HTTP ${code()}."
        }
    }
    throw ApiRequestException(code(), mensaje)
}
